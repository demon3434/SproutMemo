package com.sproutmemo.android.ui.note

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.model.AttachmentDto
import com.sproutmemo.android.ui.components.NoteAttachmentRotator
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))}MB"
}

internal fun formatSeconds(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun AudioPlayerWidget(url: String, title: String?, fileSize: Long) {
    val colors = LocalAppColors.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableStateOf(0) }
    var durationMs by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    DisposableEffect(url) {
        onDispose { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null }
    }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(300)
            mediaPlayer?.let { if (it.isPlaying) currentMs = it.currentPosition }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.darkBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎵", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title?.takeIf { it.isNotBlank() } ?: "音频附件",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(formatFileSize(fileSize), fontSize = 11.sp, color = colors.textSecondary)
                }
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            scope.launch {
                                val mp = MediaPlayer()
                                mp.setDataSource(url)
                                mp.setOnPreparedListener { durationMs = mp.duration; mp.start(); isPlaying = true }
                                mp.setOnCompletionListener { isPlaying = false; currentMs = 0; mp.seekTo(0) }
                                mp.prepareAsync()
                                mediaPlayer = mp
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                }) { Text(if (isPlaying) "⏸️" else "▶️", fontSize = 22.sp) }
            }
            if (durationMs > 0) {
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = currentMs.toFloat(),
                    onValueChange = { v -> currentMs = v.toInt(); mediaPlayer?.seekTo(v.toInt()) },
                    valueRange = 0f..durationMs.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = colors.neonBlue, activeTrackColor = colors.neonBlue),
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatSeconds(currentMs / 1000), fontSize = 10.sp, color = colors.textSecondary)
                    Text(formatSeconds(durationMs / 1000), fontSize = 10.sp, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun rememberVideoThumbnail(url: String, context: android.content.Context): ImageBitmap? {
    val uri = remember(url) { Uri.parse(url) }
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                if (uri.scheme == "http" || uri.scheme == "https") {
                    retriever.setDataSource(uri.toString(), HashMap<String, String>())
                } else {
                    retriever.setDataSource(context, uri)
                }
                val raw = retriever.getFrameAtTime(-1)
                if (raw != null) {
                    val maxW = 300
                    val scale = raw.width.toFloat() / maxW
                    val scaled = if (scale > 1f) {
                        Bitmap.createScaledBitmap(raw, maxW, (raw.height / scale).toInt(), true)
                    } else raw
                    bitmap = scaled.asImageBitmap()
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoThumbnail", "Failed to extract thumbnail for uri: $uri", e)
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
        }
    }
    return bitmap
}

@Composable
fun VideoPlayerWidget(url: String, title: String?, fileSize: Long) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var showVideoDialog by remember { mutableStateOf(false) }
    val thumbnail = rememberVideoThumbnail(url, context)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showVideoDialog = true },
        colors = CardDefaults.cardColors(containerColor = colors.darkBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(1.dp, colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumbnail,
                        contentDescription = "Video Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("🎬", fontSize = 28.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title?.takeIf { it.isNotBlank() } ?: "视频附件",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(formatFileSize(fileSize), fontSize = 11.sp, color = colors.textSecondary)
            }
            Text("▶ 播放", fontSize = 12.sp, color = colors.neonBlue)
        }
    }

    if (showVideoDialog) {
        var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
        var isPlaying by remember { mutableStateOf(true) }
        var currentMs by remember { mutableStateOf(0L) }
        var durationMs by remember { mutableStateOf(0L) }

        val loadControl = remember {
            androidx.media3.exoplayer.DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_500, 6_000, 300, 500)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        DisposableEffect(url) {
            val cacheFactory = com.sproutmemo.android.data.remote.MediaCacheManager.createCacheDataSourceFactory(context)
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(url))
                .setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                .build()
            val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(cacheFactory)
                .createMediaSource(mediaItem)

            val player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build().also { p ->
                    p.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
                    p.setMediaSource(mediaSource)
                    p.prepare()
                    p.playWhenReady = true
                }
            exoPlayer = player
            onDispose {
                player.release()
                exoPlayer = null
            }
        }

        LaunchedEffect(isPlaying) {
            while (true) {
                delay(500)
                exoPlayer?.let { p ->
                    currentMs = p.currentPosition
                    durationMs = p.duration.coerceAtLeast(0L)
                    isPlaying = p.isPlaying
                }
            }
        }

        androidx.activity.compose.BackHandler {
            exoPlayer?.release()
            exoPlayer = null
            showVideoDialog = false
        }

        Dialog(
            onDismissRequest = {
                exoPlayer?.release()
                exoPlayer = null
                showVideoDialog = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                exoPlayer?.let { player ->
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = player
                                useController = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .aspectRatio(16f / 9f)
                    )
                }

                IconButton(
                    onClick = {
                        exoPlayer?.release()
                        exoPlayer = null
                        showVideoDialog = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text("×", color = Color.White, fontSize = 32.sp)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (durationMs > 0L) {
                        Slider(
                            value = currentMs.toFloat(),
                            onValueChange = { v ->
                                currentMs = v.toLong()
                                exoPlayer?.seekTo(v.toLong())
                            },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = colors.neonBlue,
                                activeTrackColor = colors.neonBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatSeconds((currentMs / 1000).toInt()), fontSize = 10.sp, color = Color.White)
                            Text(formatSeconds((durationMs / 1000).toInt()), fontSize = 10.sp, color = Color.White)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            exoPlayer?.let { p ->
                                if (p.isPlaying) {
                                    p.pause()
                                    isPlaying = false
                                } else {
                                    p.play()
                                    isPlaying = true
                                }
                            }
                        }) {
                            Text(if (isPlaying) "⏸️" else "▶️", fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageAttachmentsSection(
    attachments: List<AttachmentDto>,
    imageUrls: List<String>,
    onOpenLightbox: (List<String>, Int) -> Unit
) {
    val colors = LocalAppColors.current
    attachments.forEachIndexed { idx, attach ->
        val fullUrl = imageUrls.getOrElse(idx) { "" }
        val optimisticDeg = NoteAttachmentRotator.optimisticRotations[attach.id] ?: 0f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.darkBg)
                .clickable { onOpenLightbox(imageUrls, idx) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = fullUrl,
                    contentDescription = "缩略图",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = optimisticDeg
                        }
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    attach.title?.takeIf { it.isNotBlank() } ?: attach.original_name ?: "图片附件",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(formatFileSize(attach.file_size), fontSize = 11.sp, color = colors.textSecondary)
            }
            Text("🔍", fontSize = 18.sp)
        }
    }
}

