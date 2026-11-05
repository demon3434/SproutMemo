package com.sproutmemo.android.media

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class MediaCompressor(private val context: Context) {
    private val TAG = "MediaCompressor"

    fun compressImage(inputUri: Uri, rotationDegrees: Float = 0f): File? {
        return FileUtils.compressImage(context, inputUri, rotationDegrees)
    }

    fun compressVideo(inputUri: Uri, resolution: String = "720p"): File? {
        val cacheDir = context.cacheDir
        val muxerOutputFile = File(cacheDir, "VID_muxer_${System.currentTimeMillis()}.mp4")
        val finalOutputFile = File(cacheDir, "VID_${System.currentTimeMillis()}.mp4")

        if (resolution == "original") {
            try {
                context.contentResolver.openInputStream(inputUri)?.use { input ->
                    FileOutputStream(muxerOutputFile).use { output -> input.copyTo(output) }
                }
                return applyFastStartOrFallback(muxerOutputFile, finalOutputFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy video", e)
                return null
            }
        }

        val targetHeight = when (resolution) {
            "360p" -> 360
            "480p" -> 480
            "720p" -> 720
            "1080p" -> 1080
            else -> 720
        }
        val targetBitrate = when (resolution) {
            "360p" -> 250 * 1024
            "480p" -> 400 * 1024
            "720p" -> 900 * 1024
            "1080p" -> 1500 * 1024
            else -> 900 * 1024
        }
        val iFrameInterval = 8

        val tempVideoFile = File(cacheDir, "temp_VID_${System.currentTimeMillis()}.mp4")

        val transcodeSuccess =
            doTranscodeVideo(inputUri, tempVideoFile, targetHeight, targetBitrate, iFrameInterval, preferHevc = true)
            || run {
                tempVideoFile.delete()
                doTranscodeVideo(inputUri, tempVideoFile, targetHeight, targetBitrate, iFrameInterval, preferHevc = false)
            }

        if (transcodeSuccess) {
            val mergeSuccess = mergeAudioAndVideo(tempVideoFile, inputUri, muxerOutputFile)
            tempVideoFile.delete()
            if (mergeSuccess) {
                return applyFastStartOrFallback(muxerOutputFile, finalOutputFile)
            }
        }

        // Fallback: 直接复制原始视频，仍尝试 faststart 重排
        try {
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                FileOutputStream(muxerOutputFile).use { output -> input.copyTo(output) }
            }
            return applyFastStartOrFallback(muxerOutputFile, finalOutputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback copy failed", e)
        }
        return null
    }

    /**
     * 尝试将 moov atom 移到文件头部（faststart），若失败则直接使用 muxerOutput。
     * 无论成功与否，都会清理临时的 muxerOutputFile。
     */
    private fun applyFastStartOrFallback(muxerOutputFile: File, finalOutputFile: File): File {
        return try {
            val success = Mp4FastStart.apply(muxerOutputFile, finalOutputFile)
            if (success && finalOutputFile.exists() && finalOutputFile.length() > 0) {
                muxerOutputFile.delete()
                Log.i(TAG, "FastStart applied. Final: ${finalOutputFile.length() / 1024} KB")
                finalOutputFile
            } else {
                // faststart 无需处理（moov 已在头部）或失败，直接用 muxer 输出
                finalOutputFile.delete()
                muxerOutputFile.renameTo(finalOutputFile)
                Log.i(TAG, "FastStart skipped/failed, using muxer output directly")
                finalOutputFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "FastStart error: ${e.message}", e)
            finalOutputFile.delete()
            muxerOutputFile.renameTo(finalOutputFile)
            finalOutputFile
        }
    }

    private fun isHevcEncoderAvailable(): Boolean {
        return try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val testFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1280, 720).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 900 * 1024)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 8)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            list.findEncoderForFormat(testFormat) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun doTranscodeVideo(
        inputUri: Uri,
        outputFile: File,
        targetHeight: Int,
        targetBitrate: Int,
        iFrameInterval: Int,
        preferHevc: Boolean = true
    ): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false
        var afd: AssetFileDescriptor? = null

        try {
            extractor = MediaExtractor()
            afd = context.contentResolver.openAssetFileDescriptor(inputUri, "r") ?: return false
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

            var videoTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    break
                }
            }
            if (videoTrackIndex == -1) return false
            extractor.selectTrack(videoTrackIndex)
            val srcFormat = extractor.getTrackFormat(videoTrackIndex)

            val srcWidth = try { srcFormat.getInteger(MediaFormat.KEY_WIDTH) } catch (e: Exception) { 0 }
            val srcHeight = try { srcFormat.getInteger(MediaFormat.KEY_HEIGHT) } catch (e: Exception) { 0 }
            if (srcWidth <= 0 || srcHeight <= 0) return false

            val useHevc = preferHevc && isHevcEncoderAvailable()
            val encoderMime = if (useHevc) MediaFormat.MIMETYPE_VIDEO_HEVC else MediaFormat.MIMETYPE_VIDEO_AVC

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val encoderFormat = MediaFormat.createVideoFormat(encoderMime, srcWidth, srcHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            }

            encoder = MediaCodec.createEncoderByType(encoderMime)
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            val srcMime = srcFormat.getString(MediaFormat.KEY_MIME) ?: return false
            decoder = MediaCodec.createDecoderByType(srcMime)
            decoder.configure(srcFormat, inputSurface, null, 0)
            decoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            var isExtractorEOS = false
            var isDecoderEOS = false
            var isEncoderEOS = false
            var videoTrackMuxerIndex = -1

            while (!isEncoderEOS) {
                if (!isExtractorEOS) {
                    val inIndex = decoder.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isExtractorEOS = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!isDecoderEOS) {
                    val outIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                    if (outIndex >= 0) {
                        val hasData = bufferInfo.size > 0
                        decoder.releaseOutputBuffer(outIndex, hasData)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            encoder.signalEndOfInputStream()
                            isDecoderEOS = true
                        }
                    }
                }

                val encIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (encIndex >= 0) {
                    val encodedData = encoder.getOutputBuffer(encIndex)!!
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        encoder.releaseOutputBuffer(encIndex, false)
                        continue
                    }
                    if (bufferInfo.size > 0 && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackMuxerIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEncoderEOS = true
                    }
                } else if (encIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    videoTrackMuxerIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
            }

            if (muxerStarted) {
                muxer.stop()
                muxerStarted = false
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Transcode error: ${e.message}", e)
            return false
        } finally {
            try { afd?.close() } catch (ignored: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (ignored: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (ignored: Exception) {}
            try { inputSurface?.release() } catch (ignored: Exception) {}
            if (muxerStarted) { try { muxer?.stop() } catch (ignored: Exception) {} }
            try { muxer?.release() } catch (ignored: Exception) {}
            try { extractor?.release() } catch (ignored: Exception) {}
        }
    }

    private fun mergeAudioAndVideo(tempVideoFile: File, originalVideoUri: Uri, finalOutputFile: File): Boolean {
        var extractorVideo: MediaExtractor? = null
        var extractorAudio: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var afdAudio: AssetFileDescriptor? = null

        try {
            extractorVideo = MediaExtractor().apply { setDataSource(tempVideoFile.absolutePath) }
            extractorAudio = MediaExtractor()
            afdAudio = context.contentResolver.openAssetFileDescriptor(originalVideoUri, "r") ?: return false
            extractorAudio.setDataSource(afdAudio.fileDescriptor, afdAudio.startOffset, afdAudio.length)

            var srcVideoTrack = -1
            for (i in 0 until extractorVideo.trackCount) {
                if (extractorVideo.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    srcVideoTrack = i
                    break
                }
            }
            var srcAudioTrack = -1
            for (i in 0 until extractorAudio.trackCount) {
                if (extractorAudio.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    srcAudioTrack = i
                    break
                }
            }
            if (srcVideoTrack == -1) return false

            muxer = MediaMuxer(finalOutputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            extractorVideo.selectTrack(srcVideoTrack)
            val dstVideoTrack = muxer.addTrack(extractorVideo.getTrackFormat(srcVideoTrack))

            var dstAudioTrack = -1
            if (srcAudioTrack != -1) {
                extractorAudio.selectTrack(srcAudioTrack)
                dstAudioTrack = muxer.addTrack(extractorAudio.getTrackFormat(srcAudioTrack))
            }

            muxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractorVideo.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.set(0, sampleSize, extractorVideo.sampleTime, extractorVideo.sampleFlags)
                muxer.writeSampleData(dstVideoTrack, buffer, bufferInfo)
                extractorVideo.advance()
            }

            if (srcAudioTrack != -1) {
                while (true) {
                    val sampleSize = extractorAudio.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.set(0, sampleSize, extractorAudio.sampleTime, extractorAudio.sampleFlags)
                    muxer.writeSampleData(dstAudioTrack, buffer, bufferInfo)
                    extractorAudio.advance()
                }
            }

            muxer.stop()
            muxerStarted = false
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Merge error: ${e.message}", e)
            return false
        } finally {
            try { afdAudio?.close() } catch (ignored: Exception) {}
            if (muxerStarted) { try { muxer?.stop() } catch (ignored: Exception) {} }
            try { muxer?.release() } catch (ignored: Exception) {}
            try { extractorVideo?.release() } catch (ignored: Exception) {}
            try { extractorAudio?.release() } catch (ignored: Exception) {}
        }
    }
}
