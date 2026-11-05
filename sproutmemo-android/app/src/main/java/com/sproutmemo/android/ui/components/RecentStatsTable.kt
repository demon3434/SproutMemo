package com.sproutmemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.RecentStatRowDto

@Composable
fun RecentStatsTable(
    recentStats: List<RecentStatRowDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📈 近期复习统计",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (recentStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("暂无近期复习统计数据", color = Color(0xFF94A3B8), fontSize = 13.sp)
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
                                .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            TableHeaderCell("时间范围", 90.dp)
                            TableHeaderCell("复习/完成", 80.dp)
                            TableHeaderCell("完成率", 65.dp)
                            TableHeaderCell("刷题数量", 70.dp)
                            TableHeaderCell("做对数量", 70.dp)
                            TableHeaderCell("正确率", 65.dp)
                        }

                        // Table Body Rows
                        recentStats.forEachIndexed { _, row ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp, horizontal = 12.dp)
                            ) {
                                TableBodyCell(row.range, 90.dp, isBold = true)
                                TableBodyCell("${row.review_count} / ${row.completed_count}", 80.dp)
                                TableBodyCell(row.session_ratio, 65.dp)
                                TableBodyCell("${row.total_questions}", 70.dp)
                                TableBodyCell("${row.correct_questions}", 70.dp)
                                TableBodyCell(row.correct_ratio, 65.dp, textColor = Color(0xFF10B981))
                            }
                            Divider(color = Color(0xFFF1F5F9))
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
    width: androidx.compose.ui.unit.Dp
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B),
        modifier = Modifier.width(width)
    )
}

@Composable
private fun TableBodyCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isBold: Boolean = false,
    textColor: Color = Color(0xFF1E293B)
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
        color = textColor,
        modifier = Modifier.width(width)
    )
}
