package com.sproutmemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.StatsOverviewDto

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewPresetsStatsCard(
    stats: StatsOverviewDto?,
    modifier: Modifier = Modifier
) {
    if (stats == null) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 复习库概览统计",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F46E5)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 第一排指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricBox(
                    label = "今日已复习",
                    value = "${stats.today_completed_notes} 题",
                    colorTheme = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    label = "复习库错题",
                    value = "${stats.review_pool_notes} / ${stats.total_notes}",
                    colorTheme = Color(0xFF4F46E5),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // 第二排掌握度
            Text(
                text = "🎯 错题掌握度分布",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MasteryProgressIndicator(
                    label = "未掌握",
                    count = stats.mastery_clueless,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                MasteryProgressIndicator(
                    label = "略微掌握",
                    count = stats.mastery_partial,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MasteryProgressIndicator(
                    label = "熟练掌握",
                    count = stats.mastery_mastered,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            // 学科分布
            if (stats.subject_distribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📚 各学科错题分布",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stats.subject_distribution.forEach { dist ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${dist.subject_name}: ${dist.count} 题",
                                fontSize = 11.sp,
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    label: String,
    value: String,
    colorTheme: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorTheme)
        }
    }
}

@Composable
private fun MasteryProgressIndicator(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${count} 题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
