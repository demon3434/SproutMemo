package com.sproutmemo.android.ui.note

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sproutmemo.android.media.AudioRecorder
import com.sproutmemo.android.ui.components.GlassMediaButton
import com.sproutmemo.android.ui.components.ImageEditDialog
import com.sproutmemo.android.ui.components.MediaAttachmentPreviewGrid
import com.sproutmemo.android.ui.theme.LocalAppColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteFormContent(
    viewModel: NoteFormViewModel,
    submitButtonText: String,
    onCancel: () -> Unit,
    onSubmitClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val config = LocalConfiguration.current
    val isTablet = remember(config) { config.screenWidthDp >= 600 }

    val auxiliaryText by viewModel.auxiliaryText.collectAsState()
    val localMediaItems by viewModel.localMediaItems.collectAsState()

    var editingImageIndex by remember { mutableStateOf<Int?>(null) }
    var showSourceSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val fileProviderAuthority = "com.sproutmemo.android.fileprovider"

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    val audioRecorder = remember { AudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addMediaItem(uri, "image") }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addMediaItem(uri, "audio") }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri -> viewModel.addMediaItem(uri, "video") }
    }

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { viewModel.addMediaItem(it, "image") }
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success) {
            tempVideoUri?.let { viewModel.addMediaItem(it, "video") }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempFile = audioRecorder.startRecording()
            isRecording = tempFile != null
        } else {
            Toast.makeText(context, "请授予录音权限以进行实时录制", Toast.LENGTH_SHORT).show()
        }
    }

    var captureType by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted) {
            if (captureType == "photo") {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val photoFile = File(context.cacheDir, "CAP_$timeStamp.jpg")
                try { photoFile.createNewFile() } catch (e: Exception) { e.printStackTrace() }
                val uri = FileProvider.getUriForFile(context, fileProviderAuthority, photoFile)
                tempPhotoUri = uri
                cameraImageLauncher.launch(uri)
            } else if (captureType == "video") {
                if (audioGranted) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val videoFile = File(context.cacheDir, "CAP_$timeStamp.mp4")
                    try { videoFile.createNewFile() } catch (e: Exception) { e.printStackTrace() }
                    val uri = FileProvider.getUriForFile(context, fileProviderAuthority, videoFile)
                    tempVideoUri = uri
                    cameraVideoLauncher.launch(uri)
                } else {
                    Toast.makeText(context, "请授予录音权限以进行实时录像", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "请授予相机权限以进行实时拍摄/录像", Toast.LENGTH_SHORT).show()
        }
    }

    val onCaptureImage = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val photoFile = File(context.cacheDir, "CAP_$timeStamp.jpg")
            try { photoFile.createNewFile() } catch (e: Exception) { e.printStackTrace() }
            val uri = FileProvider.getUriForFile(context, fileProviderAuthority, photoFile)
            tempPhotoUri = uri
            cameraImageLauncher.launch(uri)
        } else {
            captureType = "photo"
            cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    val onCaptureVideo = {
        val cameraCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val audioCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (cameraCheck == PackageManager.PERMISSION_GRANTED && audioCheck == PackageManager.PERMISSION_GRANTED) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val videoFile = File(context.cacheDir, "CAP_$timeStamp.mp4")
            try { videoFile.createNewFile() } catch (e: Exception) { e.printStackTrace() }
            val uri = FileProvider.getUriForFile(context, fileProviderAuthority, videoFile)
            tempVideoUri = uri
            cameraVideoLauncher.launch(uri)
        } else {
            captureType = "video"
            cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    val onToggleRecording = {
        if (isRecording) {
            val voiceFile = audioRecorder.stopRecording()
            isRecording = false
            if (voiceFile != null) {
                viewModel.addMediaItem(Uri.fromFile(voiceFile), "audio")
            }
        } else {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            )
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                val tempFile = audioRecorder.startRecording()
                isRecording = tempFile != null
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @Composable
    fun NoteInputAndMediaSection() {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("题目详情与笔记解析", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = auxiliaryText,
                    onValueChange = { viewModel.auxiliaryText.value = it },
                    placeholder = { Text("在此输入题目正文、考点分析或解题思路...", color = colors.textSecondary.copy(alpha = 0.5f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.neonBlue,
                        unfocusedBorderColor = colors.textSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = colors.neonBlue,
                        unfocusedLabelColor = colors.textSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                MediaAttachmentPreviewGrid(
                    items = localMediaItems,
                    onRemoveItem = { viewModel.removeMediaItem(it) },
                    onEditImage = { editingImageIndex = it },
                    onRotateImage = { viewModel.rotateMediaBy90(it) },
                    onUpdateMediaName = { idx, name -> viewModel.updateMediaName(idx, name) },
                    onMoveItem = { type, groupIndex, direction ->
                        viewModel.moveMediaItemWithinType(type, groupIndex, direction)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val imageCount = remember(localMediaItems) { localMediaItems.count { it.type == "image" } }
                val audioCount = remember(localMediaItems) { localMediaItems.count { it.type == "audio" } }
                val videoCount = remember(localMediaItems) { localMediaItems.count { it.type == "video" } }

                Text("添加多媒体附件", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassMediaButton(
                            icon = "📷",
                            text = "相册图片 ($imageCount/9)",
                            glowColor = Color(0xFF00E5FF),
                            textColor = colors.textPrimary,
                            secondaryColor = colors.textSecondary,
                            isLight = false,
                            enabled = imageCount < 9,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )
                        GlassMediaButton(
                            icon = "📸",
                            text = "实时拍照",
                            glowColor = Color(0xFF00B0FF),
                            textColor = Color.White,
                            secondaryColor = colors.textSecondary,
                            isLight = true,
                            enabled = imageCount < 9,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = onCaptureImage
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassMediaButton(
                            icon = "🎥",
                            text = "文件视频 ($videoCount/3)",
                            glowColor = Color(0xFFD500F9),
                            textColor = colors.textPrimary,
                            secondaryColor = colors.textSecondary,
                            isLight = false,
                            enabled = videoCount < 3,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = { videoPickerLauncher.launch("video/*") }
                        )
                        GlassMediaButton(
                            icon = "📹",
                            text = "实时录像",
                            glowColor = Color(0xFFE040FB),
                            textColor = Color.White,
                            secondaryColor = colors.textSecondary,
                            isLight = true,
                            enabled = videoCount < 3,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = onCaptureVideo
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassMediaButton(
                            icon = "📁",
                            text = "导入音频 ($audioCount/6)",
                            glowColor = Color(0xFF00E676),
                            textColor = colors.textPrimary,
                            secondaryColor = colors.textSecondary,
                            isLight = false,
                            enabled = audioCount < 6,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = { audioPickerLauncher.launch("audio/*") }
                        )
                        val isRecordingText = if (isRecording) "停止录音" else "实时录音"
                        val recGlowColor = if (isRecording) colors.neonRed else Color(0xFF00E676)
                        GlassMediaButton(
                            icon = if (isRecording) "⏹" else "🎤",
                            text = isRecordingText,
                            glowColor = recGlowColor,
                            textColor = if (isRecording) colors.neonRed else Color.White,
                            secondaryColor = colors.textSecondary,
                            isLight = true,
                            enabled = audioCount < 6 || isRecording,
                            modifier = Modifier.weight(1f).height(48.dp),
                            onClick = onToggleRecording
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.darkBg)
            .padding(16.dp)
    ) {
        if (isTablet) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NoteInputAndMediaSection()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NoteSourceSummaryCard(
                            viewModel = viewModel,
                            onOpenSettings = { showSourceSettings = true }
                        )
                        NoteAttributesAndSubmitSection(
                            viewModel = viewModel,
                            submitButtonText = submitButtonText,
                            onCancel = onCancel,
                            onSubmitClick = onSubmitClick
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NoteSourceSummaryCard(
                    viewModel = viewModel,
                    onOpenSettings = { showSourceSettings = true }
                )
                NoteInputAndMediaSection()
                NoteAttributesAndSubmitSection(
                    viewModel = viewModel,
                    submitButtonText = submitButtonText,
                    onCancel = onCancel,
                    onSubmitClick = onSubmitClick
                )
            }
        }
    }

    if (showSourceSettings) {
        SourceSettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showSourceSettings = false },
            onConfirm = { showSourceSettings = false }
        )
    }

    editingImageIndex?.let { index ->
        if (index in localMediaItems.indices) {
            val mediaItem = localMediaItems[index]
            ImageEditDialog(
                imageUri = mediaItem.uri,
                initialRotation = mediaItem.rotation,
                onDismiss = { editingImageIndex = null },
                onConfirm = { degrees ->
                    viewModel.updateMediaRotation(index, degrees)
                }
            )
        }
    }
}
