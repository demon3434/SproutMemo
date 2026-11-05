package com.sproutmemo.android.media

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MP4 FastStart 重排工具
 *
 * Android 的 MediaMuxer 默认将 moov atom（索引/元数据块）写在文件末尾。
 * 这导致流式播放时播放器必须先向服务器请求文件末尾的 moov，再回到头部请求音视频数据，
 * 在公网/弱网环境下会造成 10~20 秒的起播延迟。
 *
 * 本工具参考 FFmpeg -movflags +faststart 的行为：
 * 将 moov atom 从文件末尾物理移动到 mdat 之前（文件开头附近），
 * 实现流式播放秒开。
 *
 * 使用场景：视频压缩（MediaMuxer 输出）或直接复制的 MP4 文件，在上传前调用。
 */
object Mp4FastStart {
    private const val TAG = "Mp4FastStart"

    // 已知的顶层 MP4 容器 box 类型（必须在扫描时允许遍历）
    private val KNOWN_BOX_TYPES = setOf(
        "ftyp", "free", "skip", "wide", "mdat", "moov", "pdin", "bloc", "meco"
    )

    /**
     * 对输入文件执行 faststart 重排，将 moov 移到 ftyp/mdat 之前写入输出文件。
     *
     * @param inputFile  MediaMuxer 输出的原始 MP4 文件（moov 在尾部）
     * @param outputFile faststart 重排后的目标文件
     * @return true = 成功；false = 解析失败或 moov 已在头部（无需处理，调用方可直接用 inputFile）
     */
    fun apply(inputFile: File, outputFile: File): Boolean {
        try {
            val boxes = parseTopLevelBoxes(inputFile) ?: return false

            val moovBox = boxes.find { it.type == "moov" } ?: run {
                Log.w(TAG, "No moov atom found in ${inputFile.name}")
                return false
            }

            val nonMoovBoxes = boxes.filter { it.type != "moov" }

            // 如果 moov 已经在所有 mdat 之前，则无需重排
            val firstMdatOffset = nonMoovBoxes.find { it.type == "mdat" }?.offset ?: Long.MAX_VALUE
            if (moovBox.offset < firstMdatOffset) {
                Log.i(TAG, "moov is already before mdat, no reordering needed")
                return false
            }

            // 计算 moov 移到头部后，mdat 等 box 的偏移量将增加的量
            // （moov 移到文件头后，其他所有 box 整体向后偏移 moovSize）
            val moovSize = moovBox.size
            val moovData = readBoxData(inputFile, moovBox)

            // 更新 moov 内部所有 stco/co64 偏移（chunk offset 需要加上 moovSize 字节）
            val updatedMoovData = updateChunkOffsets(moovData, moovSize)

            // 按新顺序写入：ftyp（如果有）→ moov（更新后）→ 其余 box（按原顺序）
            FileOutputStream(outputFile).use { out ->
                // 优先写 ftyp
                val ftypBox = nonMoovBoxes.find { it.type == "ftyp" }
                if (ftypBox != null) {
                    copyBoxToStream(inputFile, ftypBox, out)
                }

                // 写更新后的 moov
                out.write(updatedMoovData)

                // 写其余 box（ftyp 已写过，跳过）
                for (box in nonMoovBoxes) {
                    if (box.type == "ftyp") continue
                    copyBoxToStream(inputFile, box, out)
                }
            }

            Log.i(TAG, "FastStart applied: moov moved to front. ${inputFile.length() / 1024}KB -> ${outputFile.length() / 1024}KB")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "FastStart failed: ${e.message}", e)
            return false
        }
    }

    // ─── 内部数据结构 ─────────────────────────────────────────────────────────

    private data class BoxInfo(val type: String, val offset: Long, val size: Long)

    // ─── 解析顶层 Box 列表 ────────────────────────────────────────────────────

    private fun parseTopLevelBoxes(file: File): List<BoxInfo>? {
        val boxes = mutableListOf<BoxInfo>()
        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileSize = raf.length()
                var pos = 0L
                while (pos < fileSize) {
                    if (fileSize - pos < 8) break

                    raf.seek(pos)
                    val headerBuf = ByteArray(8)
                    raf.readFully(headerBuf)
                    val bb = ByteBuffer.wrap(headerBuf).order(ByteOrder.BIG_ENDIAN)

                    val size32 = bb.int.toLong() and 0xFFFFFFFFL
                    val typeBytes = ByteArray(4)
                    bb.get(typeBytes)
                    val type = String(typeBytes, Charsets.ISO_8859_1)

                    val boxSize: Long = when {
                        size32 == 1L -> { // 扩展 64-bit 大小
                            val extBuf = ByteArray(8)
                            raf.readFully(extBuf)
                            ByteBuffer.wrap(extBuf).order(ByteOrder.BIG_ENDIAN).long
                        }
                        size32 == 0L -> fileSize - pos // 到文件结尾
                        else -> size32
                    }

                    if (boxSize < 8 || pos + boxSize > fileSize + 1) {
                        Log.w(TAG, "Suspicious box at $pos: type=$type size=$boxSize fileSize=$fileSize")
                        break
                    }

                    boxes.add(BoxInfo(type, pos, boxSize))
                    pos += boxSize
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to parse boxes: ${e.message}", e)
            return null
        }
        return boxes
    }

    // ─── 读取 box 完整数据 ────────────────────────────────────────────────────

    private fun readBoxData(file: File, box: BoxInfo): ByteArray {
        val data = ByteArray(box.size.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(box.offset)
            raf.readFully(data)
        }
        return data
    }

    // ─── 将 box 数据从原文件追加到输出流 ────────────────────────────────────────

    private fun copyBoxToStream(inputFile: File, box: BoxInfo, out: FileOutputStream) {
        FileInputStream(inputFile).use { fis ->
            fis.skip(box.offset)
            val buf = ByteArray(64 * 1024)
            var remaining = box.size
            while (remaining > 0) {
                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                val read = fis.read(buf, 0, toRead)
                if (read < 0) break
                out.write(buf, 0, read)
                remaining -= read
            }
        }
    }

    // ─── 更新 moov 内部的 stco/co64 偏移 ────────────────────────────────────────
    // moov 整体前移后，所有 chunk offset 需要加上 moovSize 字节
    // （因为 mdat 等数据块相对文件头的位置增加了 moovSize）

    private fun updateChunkOffsets(moovData: ByteArray, delta: Long): ByteArray {
        val buf = moovData.copyOf()
        var i = 0
        while (i <= buf.size - 8) {
            val size = readUInt32BE(buf, i)
            if (size < 8 || i + size > buf.size) { i++; continue }

            val type = String(buf, i + 4, 4, Charsets.ISO_8859_1)
            when (type) {
                "stco" -> {
                    // stco: 4B size + 4B type + 4B version/flags + 4B entry count + N×4B offsets
                    if (i + 16 <= buf.size) {
                        val count = readUInt32BE(buf, i + 12)
                        for (k in 0 until count.toInt()) {
                            val off = i + 16 + k * 4
                            if (off + 4 <= buf.size) {
                                val old = readUInt32BE(buf, off)
                                writeUInt32BE(buf, off, old + delta)
                            }
                        }
                    }
                    i += size.toInt()
                    continue
                }
                "co64" -> {
                    // co64: 4B size + 4B type + 4B version/flags + 4B entry count + N×8B offsets
                    if (i + 16 <= buf.size) {
                        val count = readUInt32BE(buf, i + 12)
                        for (k in 0 until count.toInt()) {
                            val off = i + 16 + k * 8
                            if (off + 8 <= buf.size) {
                                val old = readUInt64BE(buf, off)
                                writeUInt64BE(buf, off, old + delta)
                            }
                        }
                    }
                    i += size.toInt()
                    continue
                }
                "moov", "trak", "mdia", "minf", "stbl", "udta", "edts", "dinf" -> {
                    // 容器 box，递归进入（跳过 8 字节头部）
                    i += 8
                    continue
                }
                else -> {
                    i += size.toInt()
                    continue
                }
            }
        }
        return buf
    }

    // ─── 字节序工具函数 ───────────────────────────────────────────────────────

    private fun readUInt32BE(buf: ByteArray, offset: Int): Long {
        return ((buf[offset].toLong() and 0xFF) shl 24) or
                ((buf[offset + 1].toLong() and 0xFF) shl 16) or
                ((buf[offset + 2].toLong() and 0xFF) shl 8) or
                (buf[offset + 3].toLong() and 0xFF)
    }

    private fun writeUInt32BE(buf: ByteArray, offset: Int, value: Long) {
        buf[offset] = ((value shr 24) and 0xFF).toByte()
        buf[offset + 1] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 3] = (value and 0xFF).toByte()
    }

    private fun readUInt64BE(buf: ByteArray, offset: Int): Long {
        var result = 0L
        for (k in 0 until 8) result = (result shl 8) or (buf[offset + k].toLong() and 0xFF)
        return result
    }

    private fun writeUInt64BE(buf: ByteArray, offset: Int, value: Long) {
        for (k in 0 until 8) buf[offset + k] = ((value shr (56 - k * 8)) and 0xFF).toByte()
    }
}
