package com.sproutmemo.android.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Manages asynchronous persistent image rotation requests so that rotating an image
 * in Note View mode immediately updates the UI and reliably persists to the server
 * even if the user closes the Lightbox immediately after tapping rotate.
 */
object NoteAttachmentRotator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val pendingAngles = ConcurrentHashMap<String, Int>()
    private val debounceJobs = ConcurrentHashMap<String, Job>()
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    val optimisticRotations = mutableStateMapOf<String, Float>()

    fun rotate(
        context: Context,
        noteId: String,
        attachmentId: String,
        deltaDegrees: Int,
        onNoteUpdated: (NoteDto) -> Unit
    ) {
        if (noteId.isBlank() || attachmentId.isBlank()) return
        val appContext = context.applicationContext

        val currentOpt = optimisticRotations[attachmentId] ?: 0f
        optimisticRotations[attachmentId] = ((currentOpt + deltaDegrees) % 360f + 360f) % 360f

        val accumulated = ((pendingAngles[attachmentId] ?: 0) + deltaDegrees) % 360
        pendingAngles[attachmentId] = accumulated

        debounceJobs[attachmentId]?.cancel()
        debounceJobs[attachmentId] = scope.launch {
            delay(280)
            executePendingRotation(appContext, noteId, attachmentId, onNoteUpdated)
        }
    }

    fun flush(
        context: Context,
        noteId: String,
        attachmentId: String,
        onNoteUpdated: (NoteDto) -> Unit
    ) {
        if (noteId.isBlank() || attachmentId.isBlank()) return
        val hasPending = (pendingAngles[attachmentId] ?: 0) % 360 != 0
        if (!hasPending) return
        val appContext = context.applicationContext
        debounceJobs[attachmentId]?.cancel()
        scope.launch {
            executePendingRotation(appContext, noteId, attachmentId, onNoteUpdated)
        }
    }

    private suspend fun executePendingRotation(
        appContext: Context,
        noteId: String,
        attachmentId: String,
        onNoteUpdated: (NoteDto) -> Unit
    ) {
        val mutex = mutexes.getOrPut(attachmentId) { Mutex() }
        mutex.withLock {
            val rawDelta = pendingAngles.remove(attachmentId) ?: 0
            val normAngle = ((rawDelta % 360) + 360) % 360
            if (normAngle == 0) {
                optimisticRotations.remove(attachmentId)
                return
            }
            try {
                val res = withContext(Dispatchers.IO) {
                    ApiClient.getService(appContext).rotateNoteAttachment(
                        id = noteId,
                        attachmentId = attachmentId,
                        body = mapOf("angle" to normAngle)
                    )
                }
                if (res.isSuccessful && res.body()?.code == 200) {
                    val updatedNote = res.body()?.data
                    if (updatedNote != null) {
                        if ((pendingAngles[attachmentId] ?: 0) == 0) {
                            optimisticRotations.remove(attachmentId)
                        }
                        onNoteUpdated(updatedNote)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun NoteImageLightboxDialog(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    noteId: String? = null,
    attachmentIds: List<String> = emptyList(),
    onNoteUpdated: ((NoteDto) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (imageUrls.isEmpty()) return

    val context = LocalContext.current
    val lockedUrls = remember { imageUrls.toList() }
    val lockedAttachmentIds = remember { attachmentIds.toList() }

    var selectedIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, lockedUrls.lastIndex))
    }
    var zoomScale by remember(selectedIndex) { mutableFloatStateOf(1f) }
    var panOffset by remember(selectedIndex) { mutableStateOf(Offset.Zero) }
    val sessionRotations = remember { mutableStateMapOf<Int, Float>() }

    val currentRotation = sessionRotations[selectedIndex] ?: 0f
    val currentUrl = lockedUrls.getOrElse(selectedIndex) { "" }

    val handleDismiss = {
        if (!noteId.isNullOrBlank() && onNoteUpdated != null) {
            lockedAttachmentIds.forEach { attId ->
                NoteAttachmentRotator.flush(context, noteId, attId, onNoteUpdated)
            }
        }
        onDismiss()
    }

    val triggerRotate = { deltaDegrees: Int ->
        val updated = (((sessionRotations[selectedIndex] ?: 0f) + deltaDegrees) % 360f + 360f) % 360f
        sessionRotations[selectedIndex] = updated
        val attId = lockedAttachmentIds.getOrNull(selectedIndex)
        if (!noteId.isNullOrBlank() && !attId.isNullOrBlank() && onNoteUpdated != null) {
            NoteAttachmentRotator.rotate(
                context = context,
                noteId = noteId,
                attachmentId = attId,
                deltaDegrees = deltaDegrees,
                onNoteUpdated = onNoteUpdated
            )
        }
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
        ) {
            // Zoomable & Pannable Image Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (zoomScale > 1.05f) {
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                } else {
                                    zoomScale = 2.4f
                                }
                            }
                        )
                    }
                    .pointerInput(selectedIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (zoomScale * zoom).coerceIn(0.5f, 5.0f)
                            zoomScale = newScale
                            if (newScale > 1.0f) {
                                panOffset += pan
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = currentUrl,
                    contentDescription = "预览大图",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 48.dp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                            rotationZ = currentRotation
                        }
                )
            }

            // Floating Semi-Transparent Left Rotate Button (Left Side)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                    .clickable { triggerRotate(-90) },
                contentAlignment = Alignment.Center
            ) {
                PhotoFrameRotateIcon(
                    size = 24.dp,
                    tint = Color.White,
                    clockwise = false
                )
            }

            // Floating Semi-Transparent Right Rotate Button (Right Side)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                    .clickable { triggerRotate(90) },
                contentAlignment = Alignment.Center
            ) {
                PhotoFrameRotateIcon(
                    size = 24.dp,
                    tint = Color.White,
                    clockwise = true
                )
            }

            // Top-Right Close Button
            IconButton(
                onClick = handleDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭预览",
                    tint = Color.White
                )
            }

            // Bottom Floating Zoom & Page Navigation Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (lockedUrls.size > 1) {
                    TextButton(
                        onClick = { if (selectedIndex > 0) selectedIndex-- },
                        enabled = selectedIndex > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                    ) {
                        Text(
                            "◀",
                            color = if (selectedIndex > 0) Color.White else Color.White.copy(alpha = 0.3f),
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "${selectedIndex + 1}/${lockedUrls.size}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = { if (selectedIndex < lockedUrls.lastIndex) selectedIndex++ },
                        enabled = selectedIndex < lockedUrls.lastIndex,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                    ) {
                        Text(
                            "▶",
                            color = if (selectedIndex < lockedUrls.lastIndex) Color.White else Color.White.copy(alpha = 0.3f),
                            fontSize = 15.sp
                        )
                    }
                    Text("|", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
                }

                TextButton(
                    onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    Text("➖", color = Color.White, fontSize = 14.sp)
                }
                Text(
                    text = "${(zoomScale * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(5.0f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    Text("➕", color = Color.White, fontSize = 14.sp)
                }
                TextButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    Text("重置", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
        }
    }
}

