package com.sproutmemo.android.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.sproutmemo.android.data.remote.MediaCacheManager
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun rememberVideoThumbnail(uri: Uri, context: Context): ImageBitmap? {
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                if (uri.scheme == "http" || uri.scheme == "https") {
                    retriever.setDataSource(uri.toString(), HashMap<String, String>())
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                    }
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
                Log.e("VideoThumbnail", "Failed to extract thumbnail for uri: $uri", e)
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
        }
    }
    return bitmap
}

@Composable
fun CachedVideoPlayerDialog(
    videoUri: Uri,
    context: Context,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1_500,
                6_000,
                300,
                500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    DisposableEffect(videoUri) {
        val cacheFactory = MediaCacheManager.createCacheDataSourceFactory(context)
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
        val mediaSource = ProgressiveMediaSource.Factory(cacheFactory)
            .createMediaSource(mediaItem)

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().also { p ->
                p.setSeekParameters(SeekParameters.CLOSEST_SYNC)
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
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            exoPlayer?.release()
            exoPlayer = null
            onDismiss()
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
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("×", color = Color.White, fontSize = 28.sp)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    fun formatTime(ms: Long): String {
                        val totalSec = ms / 1000
                        val min = totalSec / 60
                        val sec = totalSec % 60
                        return "%02d:%02d".format(min, sec)
                    }
                    Text(
                        text = "${formatTime(currentMs)} / ${formatTime(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    IconButton(
                        onClick = {
                            exoPlayer?.let { p ->
                                if (p.isPlaying) p.pause() else p.play()
                                isPlaying = !isPlaying
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f,
                    onValueChange = { v ->
                        exoPlayer?.seekTo((v * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = colors.neonBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        thumbColor = colors.neonBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
