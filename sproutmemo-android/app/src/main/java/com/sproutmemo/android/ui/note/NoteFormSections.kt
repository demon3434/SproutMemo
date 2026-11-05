package com.sproutmemo.android.ui.note

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun NoteSourceSummaryCard(
    viewModel: NoteFormViewModel,
    onOpenSettings: () -> Unit
) {
    val colors = LocalAppColors.current
    val stages by viewModel.stages.collectAsState()
    val semesters by viewModel.semesters.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val books by viewModel.books.collectAsState()
    val paperCategories by viewModel.paperCategories.collectAsState()
    val units by viewModel.units.collectAsState()

    val selectedStageId by viewModel.selectedStageId.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSubjectId by viewModel.selectedSubjectId.collectAsState()
    val sourceType by viewModel.sourceType.collectAsState()
    val selectedQuestionSourceId by viewModel.selectedQuestionSourceId.collectAsState()
    val paperCategory by viewModel.paperCategory.collectAsState()
    val sourcePaperName by viewModel.sourcePaperName.collectAsState()
    val selectedUnitCode by viewModel.selectedUnitCode.collectAsState()
    val pageNumber by viewModel.pageNumber.collectAsState()

    val currentStage = stages.find { it.id == selectedStageId }
    val currentSem = semesters.find { it.id == selectedSemesterId }
    val currentSub = subjects.find { it.subject_id == selectedSubjectId }

    val isConfigured = selectedStageId.isNotBlank() && selectedSemesterId.isNotBlank() && selectedSubjectId.isNotBlank()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        border = BorderStroke(1.dp, if (isConfigured) colors.neonBlue.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSettings() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📚 关联习题来源",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (!isConfigured) {
                    Text("未选择学习阶段、学期或学科 (点击配置)", fontSize = 12.sp, color = Color.Red)
                } else {
                    Text(
                        text = "${currentStage?.name ?: "无"} / ${currentSem?.name ?: "无"} / ${currentSub?.subject?.subject_name ?: "无"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val currentUnit = units.find { it.id == selectedUnitCode }?.name ?: "未知单元"
                    if (sourceType == "BOOK") {
                        val currentBook = books.find { it.id == selectedQuestionSourceId }?.name ?: "未关联书本"
                        Text(
                            text = "📖 习题册: $currentBook (单元: $currentUnit, 页码: ${pageNumber.ifBlank { "未填" }})",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    } else {
                        val categoryName = paperCategories.find { it.code == paperCategory }?.name ?: "其它"
                        Text(
                            text = "📄 试卷: ${sourcePaperName.ifBlank { "自定义试卷" }} (分类: $categoryName, 单元: $currentUnit)",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Text("⚙️ 配置", fontSize = 12.sp, color = colors.neonBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NoteAttributesAndSubmitSection(
    viewModel: NoteFormViewModel,
    submitButtonText: String,
    onCancel: () -> Unit,
    onSubmitClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val submitState by viewModel.submitState.collectAsState()
    val difficultyStars by viewModel.difficultyStars.collectAsState()
    val masteryLevel by viewModel.masteryLevel.collectAsState()
    val inReviewPool by viewModel.inReviewPool.collectAsState()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("难度星级", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { star ->
                    Text(
                        text = "★",
                        fontSize = 32.sp,
                        color = if (star <= difficultyStars) Color(0xFFF59E0B) else colors.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier
                            .clickable { viewModel.difficultyStars.value = star }
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("掌握程度评估", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("CLUELESS", "完全不会", Color(0xFFEF4444)),
                    Triple("PARTIAL", "一知半解", Color(0xFFF59E0B)),
                    Triple("MASTERED", "熟练掌握", Color(0xFF10B981))
                ).forEach { (level, label, color) ->
                    val isSelected = masteryLevel == level
                    val emoji = when (level) {
                        "CLUELESS" -> "🔴"
                        "PARTIAL" -> "🟡"
                        else -> "🟢"
                    }
                    OutlinedButton(
                        onClick = { viewModel.masteryLevel.value = level },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (isSelected) color else colors.textSecondary
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) color else colors.textSecondary.copy(alpha = 0.3f)
                        ),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(emoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("纳入复习题库", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Switch(
                    checked = inReviewPool,
                    onCheckedChange = { viewModel.inReviewPool.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.neonBlue,
                        checkedTrackColor = colors.neonBlue.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(0.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.textSecondary.copy(alpha = 0.3f))
                ) {
                    Text("取消", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        if (onSubmitClick != null) {
                            onSubmitClick()
                        } else {
                            viewModel.submitNote()
                        }
                    },
                    enabled = submitState !is CreateNoteState.Submitting,
                    modifier = Modifier
                        .weight(0.6f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                ) {
                    if (submitState is CreateNoteState.Submitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(submitButtonText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

