package com.sproutmemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.RecentStatRowDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun RecentStatsTable(
    recentStats: List<RecentStatRowDto>,
    modifier: Modifier = Modifier
) {
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
                    text = "近期复习统计",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (recentStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无近期复习统计数据", color = colors.textSecondary, fontSize = 13.sp)
                }
            } else {
                // Horizontal scrolling table to fit all columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .background(colors.paper2, shape = RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            TableHeaderCell("时间范围", 90.dp, colors.textSecondary)
                            TableHeaderCell("复习/完成", 80.dp, colors.textSecondary)
                            TableHeaderCell("完成率", 65.dp, colors.textSecondary)
                            TableHeaderCell("刷题数量", 70.dp, colors.textSecondary)
                            TableHeaderCell("做对数量", 70.dp, colors.textSecondary)
                            TableHeaderCell("正确率", 65.dp, colors.textSecondary)
                        }

                        // Table Body Rows
                        recentStats.forEachIndexed { _, row ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                TableBodyCell(row.range, 90.dp, isBold = true, textColor = colors.textPrimary)
                                TableBodyCell("${row.review_count} / ${row.completed_count}", 80.dp, textColor = colors.textPrimary)
                                TableBodyCell(row.session_ratio, 65.dp, textColor = colors.textPrimary)
                                TableBodyCell("${row.total_questions}", 70.dp, textColor = colors.textPrimary)
                                TableBodyCell("${row.correct_questions}", 70.dp, textColor = colors.textPrimary)
                                TableBodyCell(row.correct_ratio, 65.dp, textColor = colors.neonGreen)
                            }
                            HorizontalDivider(color = colors.rule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    textColor: Color
) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableBodyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isBold: Boolean = false,
    textColor: Color
) {
    Text(
        text = text,
        fontSize = 13.5.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
        color = textColor,
        modifier = Modifier.width(width)
    )
}
