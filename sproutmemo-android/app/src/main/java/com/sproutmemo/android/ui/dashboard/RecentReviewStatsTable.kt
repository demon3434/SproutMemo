package com.sproutmemo.android.ui.dashboard

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
import com.sproutmemo.android.data.remote.model.RecentStatRowDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun RecentReviewStatsTable(rows: List<RecentStatRowDto>) {
    val colors = LocalAppColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "📈 近期复习统计",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.darkBg, shape = RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("时间范围", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("复习/完成", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("完成率", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("刷题/做对", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("正确率", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            }

            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.15f), thickness = 0.5.dp)

            if (rows.isEmpty()) {
                Text(
                    text = "暂无统计数据",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally)
                )
            } else {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(row.range, modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("${row.review_count}/${row.completed_count}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = colors.textSecondary)
                        Text(row.session_ratio, modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.neonBlue)
                        Text("${row.total_questions}/${row.correct_questions}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = colors.textSecondary)
                        Text(row.correct_ratio, modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                }
            }
        }
    }
}
