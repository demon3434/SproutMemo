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
            Text("👈 请在左侧选择错题卡片以预览详情", color = colors.textSecondary)
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
                    "CLUELESS" -> "🔴 完全不会"
                    "PARTIAL" -> "🟡 一知半解"
                    else -> "🟢 熟练掌握"
                }
                add("掌握程度 → $label")
            }
            pendingReviewPool?.let { add("复习题库 → ${if (it) "✅ 纳入" else "⬜ 排除"}") }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.cardBg, shape = RoundedCornerShape(16.dp))
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
                            Text(
                                text = "✏️",
                                fontSize = 16.sp,
                                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                            )
                        }
                    }
                    if (userRole != "PARENT") {
                        IconButton(onClick = { showDeleteDialog = true }, Modifier.size(36.dp)) {
                            Text("🗑️", fontSize = 16.sp)
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))

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
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "写于：${note.created_at?.substringBefore('T') ?: "—"}",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "改于：${note.updated_at?.substringBefore('T') ?: "—"}",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            // Difficulty & Status — inline interactive controls
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val currentStars = note.difficulty_stars.toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "难度星级",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue,
                        modifier = Modifier.width(80.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..5).forEach { star ->
                            val filled = star <= currentStars
                            Text(
                                text = if (filled) "★" else "☆",
                                fontSize = 24.sp,
                                color = if (filled) Color(0xFFF59E0B) else colors.textSecondary.copy(alpha = 0.4f),
                                modifier = if (userRole != "PARENT") Modifier.clickable {
                                    pendingDifficulty = star.toFloat()
                                    showConfirmDialog = true
                                } else Modifier
                            )
                        }
                        Text(
                            "($currentStars 星)",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Mastery Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "掌握程度",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.neonBlue,
                        modifier = Modifier.width(80.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        listOf(
                            Triple("CLUELESS", "完全不会", Color(0xFFEF4444)),
                            Triple("PARTIAL", "一知半解", Color(0xFFF59E0B)),
                            Triple("MASTERED", "熟练掌握", Color(0xFF10B981))
                        ).forEach { (code, label, chipColor) ->
                            val selected = note.mastery_level == code
                            val emoji = when (code) {
                                "CLUELESS" -> "🔴"
                                "PARTIAL" -> "🟡"
                                else -> "🟢"
                            }
                            OutlinedButton(
                                onClick = {
                                    if (userRole != "PARENT" && !selected) {
                                        pendingMastery = code
                                        showConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) chipColor.copy(alpha = 0.15f) else Color.Transparent,
                                    contentColor = if (selected) chipColor else colors.textSecondary
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) chipColor else colors.textSecondary.copy(alpha = 0.3f)
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                enabled = userRole != "PARENT" || selected
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(emoji, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Review Pool Toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "纳入复习题库",
                        fontSize = 12.sp,
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
                Text(
                    text = note.auxiliary_text ?: "(暂无文字描述)",
                    fontSize = 14.sp,
                    color = if (note.auxiliary_text.isNullOrBlank()) colors.textSecondary else colors.textPrimary,
                    lineHeight = 22.sp
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.neonBlue)
        content()
    }
}
