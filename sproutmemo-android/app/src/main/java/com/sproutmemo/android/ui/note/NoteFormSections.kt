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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.components.MasteryStampRow
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.StationeryPrimaryButton
import com.sproutmemo.android.ui.theme.LocalAppColors
import com.sproutmemo.android.ui.theme.SproutTextScale

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

    StationeryCard(
        borderColor = if (isConfigured) colors.neonBlue.copy(alpha = 0.4f) else colors.neonRed.copy(alpha = 0.4f),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StationeryIcon(name = "book", size = 18.dp, tint = colors.neonBlue)
                    Text(
                        text = "关联习题来源",
                        fontSize = SproutTextScale.CardTitle,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (!isConfigured) {
                    Text("未选择学习阶段、学期或学科 (点击配置)", fontSize = SproutTextScale.Label, color = colors.neonRed)
                } else {
                    Text(
                        text = "${currentStage?.name ?: "无"} / ${currentSem?.name ?: "无"} / ${currentSub?.subject?.subject_name ?: "无"}",
                        fontSize = SproutTextScale.Label,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentUnit = units.find { it.id == selectedUnitCode }?.name ?: "未知单元"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        StationeryIcon(
                            name = if (sourceType == "BOOK") "book" else "file",
                            size = 14.dp,
                            tint = colors.textSecondary
                        )
                        if (sourceType == "BOOK") {
                            val currentBook = books.find { it.id == selectedQuestionSourceId }?.name ?: "未关联书本"
                            Text(
                                text = "习题册: $currentBook (单元: $currentUnit, 页码: ${pageNumber.ifBlank { "未填" }})",
                                fontSize = SproutTextScale.Caption,
                                color = colors.textSecondary
                            )
                        } else {
                            val categoryName = paperCategories.find { it.code == paperCategory }?.name ?: "其它"
                            Text(
                                text = "试卷: ${sourcePaperName.ifBlank { "自定义试卷" }} (分类: $categoryName, 单元: $currentUnit)",
                                fontSize = SproutTextScale.Caption,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StationeryIcon(name = "settings", size = 16.dp, tint = colors.neonBlue)
                Text("配置", fontSize = SproutTextScale.Button, color = colors.neonBlue, fontWeight = FontWeight.Bold)
            }
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

    StationeryCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("难度星级", fontSize = SproutTextScale.Label, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            StarRatingStickerBar(
                stars = difficultyStars,
                onStarsChange = { viewModel.difficultyStars.value = it },
                starSize = 28.dp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("掌握程度评估", fontSize = SproutTextScale.Label, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            MasteryStampRow(
                selectedLevel = masteryLevel,
                onSelect = { viewModel.masteryLevel.value = it }
            )

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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.edge)
                ) {
                    Text("取消", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (submitState is CreateNoteState.Submitting) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .weight(0.6f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.neonBlue)
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    StationeryPrimaryButton(
                        text = submitButtonText,
                        onClick = {
                            if (onSubmitClick != null) {
                                onSubmitClick()
                            } else {
                                viewModel.submitNote()
                            }
                        },
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

