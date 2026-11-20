package com.sproutmemo.android.ui.components

import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * Fullscreen zoomable & rotatable image preview overlay for Create / Edit Note mode.
 * Features pinch-to-zoom, pan, and two semi-transparent floating rotation icon buttons
 * positioned on the left and right edges of the image layer.
 */
@Composable
fun ImageEditDialog(
    imageUri: Uri,
    initialRotation: Float = 0f,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var rotationDegrees by remember(imageUri) {
        mutableFloatStateOf(((initialRotation % 360f) + 360f) % 360f)
    }
    var zoomScale by remember(imageUri) { mutableFloatStateOf(1f) }
    var panOffset by remember(imageUri) { mutableStateOf(Offset.Zero) }

    val colors = com.sproutmemo.android.ui.theme.LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.darkBg.copy(alpha = 0.96f))
        ) {
            // Zoomable & Pannable Image Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageUri) {
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
                    .pointerInput(imageUri) {
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
                    model = imageUri,
                    contentDescription = "预览与旋转图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 48.dp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                            rotationZ = rotationDegrees
                        }
                )
            }

            // Floating Stationery Left Rotate Button (Left Side of Image Layer)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(colors.cardBg.copy(alpha = 0.92f), CircleShape)
                    .border(1.5.dp, colors.edge, CircleShape)
                    .clickable {
                        val updated = ((rotationDegrees - 90f) % 360f + 360f) % 360f
                        rotationDegrees = updated
                        onConfirm(updated)
                    },
                contentAlignment = Alignment.Center
            ) {
                PhotoFrameRotateIcon(
                    size = 24.dp,
                    tint = colors.textPrimary,
                    clockwise = false
                )
            }

            // Floating Stationery Right Rotate Button (Right Side of Image Layer)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(colors.cardBg.copy(alpha = 0.92f), CircleShape)
                    .border(1.5.dp, colors.edge, CircleShape)
                    .clickable {
                        val updated = (rotationDegrees + 90f) % 360f
                        rotationDegrees = updated
                        onConfirm(updated)
                    },
                contentAlignment = Alignment.Center
            ) {
                PhotoFrameRotateIcon(
                    size = 24.dp,
                    tint = colors.textPrimary,
                    clockwise = true
                )
            }

            // Top-Right Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(42.dp)
                    .background(colors.cardBg.copy(alpha = 0.92f), CircleShape)
                    .border(1.5.dp, colors.edge, CircleShape)
            ) {
                StationeryIcon(
                    name = "close",
                    size = 18.dp,
                    tint = colors.textPrimary
                )
            }

            // Bottom Floating Zoom Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .background(colors.cardBg.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                    .border(1.5.dp, colors.edge, RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.5f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    StationeryIcon(name = "minus", size = 16.dp, tint = colors.textPrimary)
                }
                Text(
                    text = "${(zoomScale * 100).roundToInt()}%",
                    color = colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(5.0f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    StationeryIcon(name = "plus", size = 16.dp, tint = colors.textPrimary)
                }
                TextButton(
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
                ) {
                    Text("重置", color = colors.neonBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
