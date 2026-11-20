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
internal fun TabletSessionCard(
    session: ReviewSessionDto,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val formattedDate = remember(session.created_at) {
        if (!session.created_at.isNullOrBlank()) {
            session.created_at.replace("T", " ").substringBefore(".")
        } else {
            "未知时间"
        }
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
    StationeryCard(
        containerColor = if (isSelected) lerp(colors.cardBg, colors.neonBlue, 0.12f) else colors.cardBg,
        borderColor = if (isSelected) colors.neonBlue else colors.rule,
        edgeColor = if (isSelected) colors.neonBlue else colors.edge,
        cornerRadius = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Text(
                    text = "${session.completed_count} / ${session.target_count}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.status == "COMPLETED") colors.neonGreen else colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StationeryIcon(name = "calendar", size = 12.dp, tint = colors.textSecondary)
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
internal fun PhoneSessionCard(
    session: ReviewSessionDto,
    apiService: com.sproutmemo.android.data.remote.ApiService,
    onNavigateToNote: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var detailedSession by remember { mutableStateOf<ReviewSessionDto?>(null) }
    var loadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded && detailedSession == null) {
            loadingDetails = true
            try {
                val res = apiService.getReviewSessionDetails(session.id)
                if (res.isSuccessful && res.body()?.code == 200) {
                    detailedSession = res.body()?.data?.copy(
                        semester_name = session.semester_name,
                        subject_name = session.subject_name
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingDetails = false
            }
        }
    }

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
            append(" (共 ${session.target_count} 题)")
        }
    }

    val colors = LocalAppColors.current
    StationeryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        cornerRadius = 14.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StationeryIcon(name = "calendar", size = 12.dp, tint = colors.textSecondary)
                        Text(
                            text = "$formattedDate · 策略: $strategyName",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Text(
                    text = "${session.completed_count} / ${session.target_count}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (session.status == "COMPLETED") colors.neonGreen else colors.textSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "错题详情列表:",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (loadingDetails) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        val notes = detailedSession?.notes.orEmpty()
                        if (notes.isEmpty()) {
                            Text(
                                text = "无错题快照数据",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            notes.forEachIndexed { idx, note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToNote(note.id) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        text = "${note.subject_name ?: ""} · ${note.source_paper_name ?: note.question_source_name ?: "错题"}",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StarRatingStickerBar(
                                        stars = note.difficulty_stars.toInt(),
                                        onStarsChange = null,
                                        starSize = 13.dp
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
                                        fontSize = 13.sp,
                                        color = masteryColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    StationeryIcon(name = "chevron-right", size = 13.dp, tint = colors.neonBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

