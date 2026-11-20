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
import com.sproutmemo.android.ui.components.StationeryCard
import com.sproutmemo.android.ui.components.StationeryIcon
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun RecentReviewStatsTable(rows: List<RecentStatRowDto>) {
    val colors = LocalAppColors.current

    StationeryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                StationeryIcon(name = "chart", size = 18.dp, tint = colors.neonBlue)
                Text(
                    text = "近期复习统计",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.paper2, shape = RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("时间范围", modifier = Modifier.weight(1.2f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("复习/完成", modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("完成率", modifier = Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("刷题/做对", modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                Text("正确率", modifier = Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            }

            HorizontalDivider(color = colors.rule, thickness = 0.8.dp)

            if (rows.isEmpty()) {
                Text(
                    text = "暂无统计数据",
                    fontSize = 13.5.sp,
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
                        Text(row.range, modifier = Modifier.weight(1.2f), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("${row.review_count}/${row.completed_count}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.textSecondary)
                        Text(row.session_ratio, modifier = Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.neonBlue)
                        Text("${row.total_questions}/${row.correct_questions}", modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.textSecondary)
                        Text(row.correct_ratio, modifier = Modifier.weight(0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f), thickness = 0.5.dp)
                }
            }
        }
    }
}
