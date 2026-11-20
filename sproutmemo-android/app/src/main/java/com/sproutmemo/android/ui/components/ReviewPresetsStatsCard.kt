package com.sproutmemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sproutmemo.android.ui.theme.LocalAppColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewPresetsStatsCard(
    stats: StatsOverviewDto?,
    modifier: Modifier = Modifier
) {
    if (stats == null) return
    val colors = LocalAppColors.current

    StationeryCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StationeryIcon(name = "chart", size = 18.dp, tint = colors.neonBlue)
                Text(
                    text = "复习库概览统计",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 第一排指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricBox(
                    label = "今日已复习",
                    value = "${stats.today_completed_notes} 题",
                    colorTheme = colors.neonGreen,
                    bgColor = colors.paper2,
                    borderColor = colors.rule,
                    labelColor = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    label = "复习库错题",
                    value = "${stats.review_pool_notes} / ${stats.total_notes}",
                    colorTheme = colors.neonBlue,
                    bgColor = colors.paper2,
                    borderColor = colors.rule,
                    labelColor = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.rule)
            Spacer(modifier = Modifier.height(12.dp))

            // 第二排掌握度
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StationeryIcon(name = "target", size = 14.dp, tint = colors.textSecondary)
                Text(
                    text = "错题掌握度分布",
                    fontSize = 13.5.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MasteryProgressIndicator(
                    label = "未掌握",
                    count = stats.mastery_clueless,
                    color = colors.neonRed,
                    bgColor = colors.paper2,
                    borderColor = colors.rule,
                    labelColor = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                MasteryProgressIndicator(
                    label = "略微掌握",
                    count = stats.mastery_partial,
                    color = colors.neonAmber,
                    bgColor = colors.paper2,
                    borderColor = colors.rule,
                    labelColor = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                MasteryProgressIndicator(
                    label = "熟练掌握",
                    count = stats.mastery_mastered,
                    color = colors.neonGreen,
                    bgColor = colors.paper2,
                    borderColor = colors.rule,
                    labelColor = colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 学科分布
            if (stats.subject_distribution.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StationeryIcon(name = "book", size = 14.dp, tint = colors.textSecondary)
                    Text(
                        text = "各学科错题分布",
                        fontSize = 13.5.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    stats.subject_distribution.forEach { dist ->
                        Box(
                            modifier = Modifier
                                .background(colors.neonBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, colors.neonBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${dist.subject_name}: ${dist.count} 题",
                                fontSize = 13.sp,
                                color = colors.neonBlue,
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
    bgColor: Color,
    borderColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(label, fontSize = 13.sp, color = labelColor, fontWeight = FontWeight.Bold)
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
    bgColor: Color,
    borderColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 13.5.sp, color = labelColor, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${count} 题", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
