package com.sproutmemo.android.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.ui.components.MasteryStampRow
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryLinedNoteBox
import com.sproutmemo.android.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun NoteDetailPane(
    note: NoteDto?,
    userRole: String,
    viewModel: NoteListViewModel,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenLightbox: (List<String>, Int) -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    if (note == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StationeryIcon(name = "chevron-left", size = 16.dp, tint = colors.textSecondary)
                Text("请在左侧选择错题卡片以预览详情", color = colors.textSecondary)
            }
        }
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Pending edit - only committed on confirm
    var pendingDifficulty by remember { mutableStateOf<Float?>(null) }
    var pendingMastery by remember { mutableStateOf<String?>(null) }
    var pendingReviewPool by remember { mutableStateOf<Boolean?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    val imageAttachments = remember(note.id, note.attachments) {
        note.attachments.filter { it.file_type?.equals("IMAGE", ignoreCase = true) == true }
    }
    val audioAttachments = remember(note.id, note.attachments) {
        note.attachments.filter { it.file_type?.equals("AUDIO", ignoreCase = true) == true }
    }
    val videoAttachments = remember(note.id, note.attachments) {
        note.attachments.filter { it.file_type?.equals("VIDEO", ignoreCase = true) == true }
    }
    val imageUrls = remember(note.id, note.attachments) {
        imageAttachments.map { ApiClient.getFullAttachmentUrl(context, it.storage_path) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete(note.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("确认删除", color = Color.White) }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }) { Text("取消") } },
            title = { Text("确认删除？") },
            text = { Text("删除后不可恢复，是否确认？") },
            containerColor = colors.cardBg
        )
    }

    if (showConfirmDialog) {
        val descParts = buildList {
            pendingDifficulty?.let {
                val s = it.toInt()
                add("难度 → ${"★".repeat(s)}${"☆".repeat(5 - s)} ($s 星)")
            }
            pendingMastery?.let { m ->
                val label = when (m) {
                    "CLUELESS" -> "完全不会"
                    "PARTIAL" -> "一知半解"
                    else -> "熟练掌握"
                }
                add("掌握程度 → $label")
            }
            pendingReviewPool?.let { add("复习题库 → ${if (it) "纳入" else "排除"}") }
        }
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    pendingDifficulty = null; pendingMastery = null; pendingReviewPool = null
                    showConfirmDialog = false
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.updateNoteFields(
                            noteId = note.id,
                            difficultyStars = pendingDifficulty ?: note.difficulty_stars.toFloat(),
                            masteryLevel = pendingMastery ?: note.mastery_level,
                            excludeFromReview = pendingReviewPool?.let { !it } ?: !note.in_review_pool,
                            onSuccess = {
                                isSaving = false; showConfirmDialog = false
                                pendingDifficulty = null; pendingMastery = null; pendingReviewPool = null
                                snackMsg = "已保存 ✓"
                            },
                            onError = { err -> isSaving = false; snackMsg = "保存失败：$err" }
                        )
                    },
                    enabled = !isSaving
                ) { Text(if (isSaving) "保存中…" else "确认保存") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    if (!isSaving) {
                        pendingDifficulty = null; pendingMastery = null; pendingReviewPool = null
                        showConfirmDialog = false
                    }
                }) { Text("取消") }
            },
            title = { Text("确认修改？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    descParts.forEach { Text(it, fontSize = 13.sp, color = colors.textPrimary) }
                }
            },
            containerColor = colors.cardBg
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StationeryCard(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "${note.subject_name ?: "未知学科"} · ${note.semester_name ?: ""}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (userRole != "PARENT") {
                            IconButton(onClick = { onEdit(note.id) }, Modifier.size(36.dp)) {
                                StationeryIcon(name = "pen", size = 18.dp, tint = colors.neonBlue)
                            }
                        }
                        if (userRole != "PARENT") {
                            IconButton(onClick = { showDeleteDialog = true }, Modifier.size(36.dp)) {
                                StationeryIcon(name = "trash", size = 18.dp, tint = colors.neonRed)
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.rule)

            // Source Info — flow layout
            DetailSection("习题来源") {
                val flowText = if (note.source_type == "BOOK") {
                    "习题册 -> 《${note.question_source_name ?: "—"}》 -> ${note.unit_name ?: "未设置单元"} -> P.${note.page_number ?: "—"}"
                } else {
                    val category = note.paper_category_name?.takeIf { it.isNotBlank() } ?: "日常作业/练习"
                    "试卷/其它 -> $category -> 《${note.source_paper_name ?: note.question_source_name ?: "—"}》 -> ${note.unit_name ?: "未设置单元"}"
                }
                Text(
                    text = flowText,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "写于：${note.created_at?.substringBefore('T') ?: "—"}",
                        fontSize = 13.sp,
                        color = colors.textSecondary.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "改于：${note.updated_at?.substringBefore('T') ?: "—"}",
                        fontSize = 13.sp,
                        color = colors.textSecondary.copy(alpha = 0.85f)
                    )
                }
            }

            // Difficulty & Status — inline interactive controls
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val currentStars = note.difficulty_stars.toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "难度星级",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue,
                        modifier = Modifier.width(90.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StarRatingStickerBar(
                            stars = currentStars,
                            onStarsChange = if (userRole != "PARENT") {
                                { star ->
                                    pendingDifficulty = star.toFloat()
                                    showConfirmDialog = true
                                }
                            } else null,
                            starSize = 24.dp
                        )
                        Text(
                            "($currentStars 星)",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Mastery Level (Solid Card-Stock Stamps)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "掌握程度",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue
                    )
                    MasteryStampRow(
                        selectedLevel = note.mastery_level,
                        enabled = userRole != "PARENT",
                        onSelect = { code ->
                            if (userRole != "PARENT" && note.mastery_level != code) {
                                pendingMastery = code
                                showConfirmDialog = true
                            }
                        }
                    )
                }

                // Review Pool Toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "纳入复习题库",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue
                    )
                    if (userRole != "PARENT") {
                        Switch(
                            checked = note.in_review_pool,
                            onCheckedChange = { newVal ->
                                pendingReviewPool = newVal
                                showConfirmDialog = true
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.neonBlue,
                                checkedTrackColor = colors.neonBlue.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            // Auxiliary text
            DetailSection("题目与解析") {
                StationeryLinedNoteBox(text = note.auxiliary_text)
            }

            // Images
            if (imageAttachments.isNotEmpty()) {
                DetailSection("图片附件 (${imageAttachments.size})") {
                    ImageAttachmentsSection(imageAttachments, imageUrls, onOpenLightbox)
                }
            }

            // Audios
            if (audioAttachments.isNotEmpty()) {
                DetailSection("音频附件 (${audioAttachments.size})") {
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
                DetailSection("视频附件 (${videoAttachments.size})") {
                    videoAttachments.forEach { attach ->
                        VideoPlayerWidget(
                            url = ApiClient.getFullAttachmentUrl(context, attach.storage_path),
                            title = attach.title ?: attach.original_name,
                            fileSize = attach.file_size
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        }

        snackMsg?.let { msg ->
            LaunchedEffect(msg) { delay(2500); snackMsg = null }
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = { snackMsg = null }) { Text("×") } }
            ) { Text(msg) }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.neonBlue)
        content()
    }
}
