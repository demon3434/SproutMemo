package com.sproutmemo.android.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.roundToInt

object FileUtils {
    private const val TAG = "FileUtils"

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun openUriStream(context: Context, uri: Uri): InputStream? {
        val scheme = uri.scheme?.lowercase()
        return when (scheme) {
            "http", "https" -> {
                val conn = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 15_000
                    doInput = true
                }
                conn.inputStream
            }
            "file" -> {
                val path = uri.path ?: return null
                FileInputStream(File(path))
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }

    private fun readExifTransformMatrix(context: Context, uri: Uri): Matrix {
        val matrix = Matrix()
        var stream: InputStream? = null
        try {
            stream = openUriStream(context, uri) ?: return matrix
            val exif = ExifInterface(stream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1f, 1f)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read EXIF orientation from $uri: ${e.message}")
        } finally {
            try { stream?.close() } catch (ignored: Exception) {}
        }
        return matrix
    }

    fun compressImage(context: Context, inputUri: Uri, rotationDegrees: Float = 0f): File? {
        var inputStream: InputStream? = null
        try {
            val isRemote = inputUri.scheme == "http" || inputUri.scheme == "https"
            val workingUri: Uri
            var tempDownloadedFile: File? = null

            if (isRemote) {
                tempDownloadedFile = File(context.cacheDir, "DL_${System.nanoTime()}.tmp")
                openUriStream(context, inputUri)?.use { input ->
                    FileOutputStream(tempDownloadedFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (!tempDownloadedFile.exists() || tempDownloadedFile.length() == 0L) {
                    tempDownloadedFile.delete()
                    return null
                }
                workingUri = Uri.fromFile(tempDownloadedFile)
            } else {
                workingUri = inputUri
            }

            try {
                // 1. Read EXIF orientation transform matrix
                val transformMatrix = readExifTransformMatrix(context, workingUri)
                if (rotationDegrees % 360f != 0f) {
                    transformMatrix.postRotate(rotationDegrees)
                }

                // 2. Decode image bounds
                inputStream = openUriStream(context, workingUri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()

                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                var inSampleSize = 1
                val maxEdge = 1920f

                if (srcWidth > maxEdge || srcHeight > maxEdge) {
                    val ratio = if (srcWidth > srcHeight) srcWidth / maxEdge else srcHeight / maxEdge
                    inSampleSize = ratio.toInt().coerceAtLeast(1)
                }

                // 3. Decode sampled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                inputStream = openUriStream(context, workingUri)
                var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()

                if (bitmap == null) return null

                // 4. Resize precisely to max edge of 1920
                val currentW = bitmap.width
                val currentH = bitmap.height
                if (currentW > maxEdge || currentH > maxEdge) {
                    val scale = if (currentW > currentH) maxEdge / currentW else maxEdge / currentH
                    val targetW = (currentW * scale).roundToInt().coerceAtLeast(1)
                    val targetH = (currentH * scale).roundToInt().coerceAtLeast(1)
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                    if (scaledBitmap != bitmap) {
                        bitmap.recycle()
                        bitmap = scaledBitmap
                    }
                }

                // 5. Apply combined EXIF + user rotation transform matrix
                if (!transformMatrix.isIdentity) {
                    val transformedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, transformMatrix, true
                    )
                    if (transformedBitmap != bitmap) {
                        bitmap.recycle()
                        bitmap = transformedBitmap
                    }
                }

                // 6. Compress into JPEG file <= 1MB
                val cacheDir = context.cacheDir
                val compressedFile = File(cacheDir, "IMG_${System.nanoTime()}.jpg")

                var quality = 90
                var fileLength: Long
                do {
                    FileOutputStream(compressedFile).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                        outputStream.flush()
                    }
                    fileLength = compressedFile.length()
                    if (fileLength <= 1024 * 1024) {
                        break
                    }
                    quality -= 5
                } while (quality >= 60)

                bitmap.recycle()
                return compressedFile
            } finally {
                tempDownloadedFile?.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image", e)
            return null
        } finally {
            try { inputStream?.close() } catch (ignored: Exception) {}
        }
    }

    fun calculateMd5(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val md5Bytes = digest.digest()
            md5Bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate MD5 of file", e)
            null
        }
    }

    fun calculateMd5(context: Context, uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val md5Bytes = digest.digest()
            md5Bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate MD5 of Uri", e)
            null
        }
    }
}
