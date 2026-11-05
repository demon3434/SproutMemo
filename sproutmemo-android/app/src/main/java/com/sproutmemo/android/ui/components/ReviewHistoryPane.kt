package com.sproutmemo.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.ReviewSessionDto

@Composable
fun ReviewHistoryPane(
    sessions: List<ReviewSessionDto>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("📋 暂无复习打卡历史记录", color = Color(0xFF94A3B8), fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sessions) { session ->
            ReviewHistoryItem(session = session)
        }
    }
}

@Composable
private fun ReviewHistoryItem(
    session: ReviewSessionDto
) {
    var expanded by remember { mutableStateOf(false) }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "复习计划 (共 ${session.target_count} 题)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📅 $formattedDate · 策略: $strategyName",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = "${session.completed_count} / ${session.target_count}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (session.status == "COMPLETED") Color(0xFF10B981) else Color(0xFF475569)
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (session.notes.orEmpty().isEmpty()) {
                        Text(
                            text = "无错题快照数据",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        session.notes.orEmpty().forEachIndexed { idx, note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = "${note.subject_name ?: ""} · ${note.source_paper_name ?: note.question_source_name ?: "错题"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "⭐ ${note.difficulty_stars}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
