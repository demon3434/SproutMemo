package com.sproutmemo.android.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.ApiClient
import com.sproutmemo.android.data.remote.model.NoteDto
import com.sproutmemo.android.data.remote.model.ReviewSessionDto
import com.sproutmemo.android.data.remote.model.UserDto
import com.sproutmemo.android.data.remote.model.SubjectDto
import com.sproutmemo.android.ui.components.StarRatingStickerBar
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.components.gridPaper
import com.sproutmemo.android.ui.theme.LocalAppColors


@Composable
internal fun SessionDetailPanel(
    session: ReviewSessionDto,
    onNavigateToNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(session.created_at) {
        if (!session.created_at.isNullOrBlank()) {
            session.created_at.replace("T", " ").substringBefore(".")
        } else {
            "未知时间"
        }
    }

    val strategyName = when (session.strategy) {
        "RANDOM" -> "听天由命"
        "EBBINGHAUS" -> "艾宾浩斯"
        "CHALLENGE" -> "自我挑战"
        "PERSISTENT_FAILURE" -> "屡败屡战"
        else -> session.strategy
    }

    val statusColor = when (session.status) {
        "COMPLETED" -> Color(0xFF10B981)
        "IN_PROGRESS" -> Color(0xFF3B82F6)
        "ABANDONED" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }

    val statusName = when (session.status) {
        "COMPLETED" -> "已完成"
        "IN_PROGRESS" -> "进行中"
        "ABANDONED" -> "已放弃"
        else -> session.status
    }

    val displayName = remember(session.semester_name, session.subject_name) {
        buildString {
            append(session.semester_name ?: "未知学期")
            if (!session.subject_name.isNullOrBlank()) {
                append(" · ")
                append(session.subject_name)
            }
        }
    }

    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize()) {
        StationeryCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StationeryIcon(name = "calendar", size = 13.dp, tint = colors.textSecondary)
                        Text(
                            text = "$formattedDate  ·  策略: $strategyName",
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    Text(
                        text = "完成进度: ${session.completed_count} / ${session.target_count}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "题目详情列表:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val notesList = session.notes.orEmpty()
        if (notesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("无错题快照数据", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notesList.size) { idx ->
                    val note = notesList[idx]
                    QuestionListItem(note = note, index = idx + 1, onClick = { onNavigateToNote(note.id) })
                }
            }
        }
    }
}

@Composable
internal fun QuestionListItem(
    note: NoteDto,
    index: Int,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    StationeryCard(
        cornerRadius = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.width(24.dp)
            )
            Text(
                text = "${note.subject_name ?: ""} · ${note.source_paper_name ?: note.question_source_name ?: "错题"}",
                fontSize = 13.sp,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            val masteryText = when (note.mastery_level) {
                "CLUELESS" -> "完全不会"
                "PARTIAL" -> "一知半解"
                else -> "熟练掌握"
            }
            val masteryColor = when (note.mastery_level) {
                "CLUELESS" -> colors.neonRed
                "PARTIAL" -> colors.neonAmber
                else -> colors.neonGreen
            }
            Text(
                text = masteryText,
                fontSize = 13.5.sp,
                color = masteryColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            StarRatingStickerBar(
                stars = note.difficulty_stars.toInt(),
                onStarsChange = null,
                starSize = 14.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "预览",
                    fontSize = 13.5.sp,
                    color = colors.neonBlue,
                    fontWeight = FontWeight.Bold
                )
                StationeryIcon(name = "chevron-right", size = 13.dp, tint = colors.neonBlue)
            }
        }
    }
}

