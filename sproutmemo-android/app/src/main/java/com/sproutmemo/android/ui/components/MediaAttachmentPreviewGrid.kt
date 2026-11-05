package com.sproutmemo.android.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sproutmemo.android.ui.theme.LocalAppColors
import java.io.File

data class LocalMediaItem(
    val uri: Uri,
    val type: String, // "image", "audio", "video"
    val rotation: Float = 0f,
    val name: String = "",
    val compressedUri: Uri? = null,
    val id: String? = null,
    val size: Long = 0L
)

@Composable
fun MediaAttachmentPreviewGrid(
    items: List<LocalMediaItem>,
    onRemoveItem: (Int) -> Unit,
    onEditImage: (Int) -> Unit,
    onRotateImage: (Int) -> Unit = {},
    onUpdateMediaName: (Int, String) -> Unit,
    onMoveItem: (type: String, groupIndex: Int, direction: Int) -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val indexedItems = remember(items) { items.mapIndexed { index, item -> index to item } }
    val images = remember(indexedItems) { indexedItems.filter { it.second.type == "image" } }
    val videos = remember(indexedItems) { indexedItems.filter { it.second.type == "video" } }
    val audios = remember(indexedItems) { indexedItems.filter { it.second.type == "audio" } }

    var playingAudioUri by remember { mutableStateOf<Uri?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var showVideoDialog by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Image Attachments Section
        if (images.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📸 图片附件 (${images.size}/9) · 点击图片可放大预览并旋转",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val imageChunks = images.chunked(3)
                imageChunks.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { (absIndex, media) ->
                            val groupIndex = images.indexOfFirst { it.first == absIndex }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.darkBg)
                                        .border(1.dp, colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .clickable { onEditImage(absIndex) }
                                ) {
                                    AsyncImage(
                                        model = media.uri,
                                        contentDescription = "Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = media.rotation
                                            }
                                    )

                                    // Delete top-right overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                            .clickable { onRemoveItem(absIndex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Size tag bottom-right
                                    val sizeBytes = remember(media) { getUriSize(context, media) }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(formatSize(sizeBytes), color = Color.White, fontSize = 9.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onMoveItem("image", groupIndex, -1) },
                                        enabled = groupIndex > 0,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("←", fontSize = 14.sp, color = if (groupIndex > 0) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                    }
                                    IconButton(
                                        onClick = { onMoveItem("image", groupIndex, 1) },
                                        enabled = groupIndex < images.size - 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("→", fontSize = 14.sp, color = if (groupIndex < images.size - 1) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                MediaTitleField(
                                    value = media.name,
                                    onValueChange = { onUpdateMediaName(absIndex, it) },
                                    placeholder = "名称/备注"
                                )
                            }
                        }

                        val remaining = 3 - chunk.size
                        repeat(remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2. Video Attachments Section
        if (videos.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎥 视频附件 (${videos.size}/3)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val videoChunks = videos.chunked(3)
                videoChunks.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        chunk.forEach { (absIndex, media) ->
                            val groupIndex = videos.indexOfFirst { it.first == absIndex }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .border(1.dp, colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                ) {
                                    val thumbnail = rememberVideoThumbnail(media.uri, context)
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail,
                                            contentDescription = "Video Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(36.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .clickable {
                                                videoUri = media.uri
                                                showVideoDialog = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("▶", color = colors.neonBlue, fontSize = 16.sp)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .clickable { onRemoveItem(absIndex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("×", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    val sizeBytes = remember(media) { getUriSize(context, media) }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(formatSize(sizeBytes), color = Color.White, fontSize = 9.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onMoveItem("video", groupIndex, -1) },
                                        enabled = groupIndex > 0,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("←", fontSize = 14.sp, color = if (groupIndex > 0) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                    }
                                    IconButton(
                                        onClick = { onMoveItem("video", groupIndex, 1) },
                                        enabled = groupIndex < videos.size - 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("→", fontSize = 14.sp, color = if (groupIndex < videos.size - 1) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                MediaTitleField(
                                    value = media.name,
                                    onValueChange = { onUpdateMediaName(absIndex, it) },
                                    placeholder = "名称/备注"
                                )
                            }
                        }

                        val remaining = 3 - chunk.size
                        repeat(remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 3. Audio Attachments Section
        if (audios.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎵 音频附件 (${audios.size}/6)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                audios.forEachIndexed { groupIndex, (absIndex, media) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                        border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isPlaying = playingAudioUri == media.uri
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.darkBg)
                                    .clickable {
                                        if (isPlaying) {
                                            mediaPlayer?.stop()
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            playingAudioUri = null
                                        } else {
                                            try {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = MediaPlayer().apply {
                                                    if (media.uri.scheme == "file") {
                                                        setDataSource(media.uri.path ?: "")
                                                    } else {
                                                        setDataSource(context, media.uri)
                                                    }
                                                    prepare()
                                                    start()
                                                    setOnCompletionListener {
                                                        playingAudioUri = null
                                                    }
                                                }
                                                playingAudioUri = media.uri
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "音频播放失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isPlaying) "⏹" else "▶",
                                    color = colors.neonBlue,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val sizeBytes = remember(media) { getUriSize(context, media) }
                                    Text(
                                        text = "音频录音 (${formatSize(sizeBytes)})",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onMoveItem("audio", groupIndex, -1) },
                                            enabled = groupIndex > 0,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("↑", fontSize = 14.sp, color = if (groupIndex > 0) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                        }
                                        IconButton(
                                            onClick = { onMoveItem("audio", groupIndex, 1) },
                                            enabled = groupIndex < audios.size - 1,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("↓", fontSize = 14.sp, color = if (groupIndex < audios.size - 1) colors.neonBlue else colors.textSecondary.copy(alpha = 0.3f))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onRemoveItem(absIndex) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("🗑️", fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                MediaTitleField(
                                    value = media.name,
                                    onValueChange = { onUpdateMediaName(absIndex, it) },
                                    placeholder = "输入备注/名称..."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVideoDialog && videoUri != null) {
        CachedVideoPlayerDialog(
            videoUri = videoUri!!,
            context = context,
            onDismiss = { showVideoDialog = false }
        )
    }
}

@Composable
private fun MediaTitleField(
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
            fontSize = 11.sp
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
                            fontSize = 11.sp,
                            color = colors.textSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

private fun getUriSize(context: Context, item: LocalMediaItem): Long {
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

private fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 KB"
    if (sizeBytes < 1024) return "${sizeBytes} B"
    val kb = sizeBytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
