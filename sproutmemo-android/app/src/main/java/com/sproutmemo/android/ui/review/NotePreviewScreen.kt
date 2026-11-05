package com.sproutmemo.android.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.NoteImageLightboxDialog
import com.sproutmemo.android.ui.note.AudioPlayerWidget
import com.sproutmemo.android.ui.note.VideoPlayerWidget
import com.sproutmemo.android.ui.note.ImageAttachmentsSection
import com.sproutmemo.android.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePreviewScreen(
    noteId: String,
    userRole: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    var note by remember { mutableStateOf<NoteDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(noteId) {
        loading = true
        try {
            val res = ApiClient.getService(context).getNoteDetail(noteId)
            if (res.isSuccessful && res.body()?.code == 200) {
                note = res.body()?.data
            } else {
                errorMsg = res.errorBody()?.string() ?: res.body()?.msg ?: "加载失败"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMsg = e.localizedMessage ?: "加载异常"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("题目预览", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.cardBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.darkBg)
                .padding(innerPadding)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.neonBlue
                )
            } else if (errorMsg != null) {
                Text(
                    text = errorMsg ?: "",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else note?.let { currentNote ->
                val imageAttachments = remember(currentNote.id, currentNote.attachments) {
                    currentNote.attachments.filter { it.file_type?.equals("IMAGE", ignoreCase = true) == true }
                }
                val audioAttachments = remember(currentNote.id, currentNote.attachments) {
                    currentNote.attachments.filter { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
                }
                val videoAttachments = remember(currentNote.id, currentNote.attachments) {
                    currentNote.attachments.filter { it.file_type?.equals("VIDEO", ignoreCase = true) == true }
                }
                val imageUrls = remember(currentNote.id, currentNote.attachments) {
                    imageAttachments.map { ApiClient.getFullAttachmentUrl(context, it.storage_path) }
                }

                var showLightbox by remember { mutableStateOf(false) }
                var lightboxUrls by remember { mutableStateOf<List<String>>(emptyList()) }
                var lightboxIndex by remember { mutableStateOf(0) }

                val onOpenLightbox = { urls: List<String>, index: Int ->
                    lightboxUrls = urls
                    lightboxIndex = index
                    showLightbox = true
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${currentNote.subject_name ?: "未知学科"} · ${currentNote.semester_name ?: ""}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val sourceText = if (currentNote.source_type == "BOOK") {
                                "习题册: 《${currentNote.question_source_name ?: "—"}》 -> ${currentNote.unit_name ?: "未设置单元"} -> P.${currentNote.page_number ?: "—"}"
                            } else {
                                val category = currentNote.paper_category_name?.takeIf { it.isNotBlank() } ?: "日常作业/练习"
                                "试卷/其它: $category -> 《${currentNote.source_paper_name ?: currentNote.question_source_name ?: "—"}》 -> ${currentNote.unit_name ?: "未设置单元"}"
                            }
                            Text(
                                text = sourceText,
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    // Attributes Card (Difficulty, Mastery, Review Pool status)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Difficulty
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "难度星级",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.neonBlue,
                                    modifier = Modifier.width(80.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { star ->
                                        Text(
                                            text = if (star <= currentNote.difficulty_stars) "★" else "☆",
                                            fontSize = 20.sp,
                                            color = if (star <= currentNote.difficulty_stars) Color(0xFFF59E0B) else colors.textSecondary.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }

                            // Mastery Level
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "掌握程度",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.neonBlue,
                                    modifier = Modifier.width(80.dp)
                                )
                                val masteryLabel = when (currentNote.mastery_level) {
                                    "CLUELESS" -> "🔴 完全不会"
                                    "PARTIAL" -> "🟡 一知半解"
                                    else -> "🟢 熟练掌握"
                                }
                                Box(
                                    modifier = Modifier
                                        .background(colors.darkBg, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = masteryLabel,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            // Review Pool Switch (Read-only representation)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "纳入复习题库",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.neonBlue
                                )
                                Switch(
                                    checked = currentNote.in_review_pool,
                                    onCheckedChange = null,
                                    enabled = false,
                                    colors = SwitchDefaults.colors(
                                        disabledCheckedThumbColor = colors.neonBlue,
                                        disabledCheckedTrackColor = colors.neonBlue.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }

                    // Auxiliary Text Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "题目与解析",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.neonBlue
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentNote.auxiliary_text ?: "(暂无文字描述)",
                                fontSize = 14.sp,
                                color = if (currentNote.auxiliary_text.isNullOrBlank()) colors.textSecondary else colors.textPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // Attachments Section
                    if (imageAttachments.isNotEmpty() || audioAttachments.isNotEmpty() || videoAttachments.isNotEmpty()) {
                        Text(
                            text = "附件列表",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // Images
                        if (imageAttachments.isNotEmpty()) {
                            DetailSection("图片附件 (${imageAttachments.size})", colors.neonBlue) {
                                ImageAttachmentsSection(imageAttachments, imageUrls, onOpenLightbox)
                            }
                        }

                        // Audios
                        if (audioAttachments.isNotEmpty()) {
                            DetailSection("音频附件 (${audioAttachments.size})", colors.neonBlue) {
                                audioAttachments.forEach { attach ->
                                    AudioPlayerWidget(
                                        url = ApiClient.getFullAttachmentUrl(context, attach.storage_path),
                                        title = attach.title ?: attach.original_name,
                                        fileSize = attach.file_size
                                    )
                                }
                            }
                        }

                        // Videos
                        if (videoAttachments.isNotEmpty()) {
                            DetailSection("视频附件 (${videoAttachments.size})", colors.neonBlue) {
                                videoAttachments.forEach { attach ->
                                    VideoPlayerWidget(
                                        url = ApiClient.getFullAttachmentUrl(context, attach.storage_path),
                                        title = attach.title ?: attach.original_name,
                                        fileSize = attach.file_size
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLightbox && lightboxUrls.isNotEmpty()) {
                    NoteImageLightboxDialog(
                        imageUrls = lightboxUrls,
                        initialIndex = lightboxIndex,
                        noteId = currentNote.id,
                        attachmentIds = imageAttachments.map { it.id },
                        onNoteUpdated = { updated -> note = updated },
                        onDismiss = { showLightbox = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    titleColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = titleColor)
        content()
    }
}
