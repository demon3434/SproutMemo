package com.sproutmemo.android.ui.components

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors
import java.io.File

@Composable
internal fun MediaTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = LocalAppColors.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = colors.textPrimary,
            fontSize = 13.sp
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .border(1.dp, colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            color = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

internal fun getUriSize(context: Context, item: LocalMediaItem): Long {
    val cUri = item.compressedUri
    if (cUri != null && cUri.scheme == "file") {
        return try {
            val path = cUri.path
            if (path != null) File(path).length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
    if (item.size > 0L) return item.size
    val uri = item.uri
    if (uri.scheme == "file") {
        return try {
            val path = uri.path
            if (path != null) File(path).length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
    var size = 0L
    try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.SIZE)
                size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return size
}

internal fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 KB"
    if (sizeBytes < 1024) return "${sizeBytes} B"
    val kb = sizeBytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

