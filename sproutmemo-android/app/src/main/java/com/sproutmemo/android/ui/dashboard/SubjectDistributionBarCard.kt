package com.sproutmemo.android.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.data.remote.model.StatsOverviewDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun SubjectDistributionBarCard(stats: StatsOverviewDto?) {
    val colors = LocalAppColors.current
    val items = stats?.subject_distribution ?: emptyList()
    val maxCount = items.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

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
                text = "📚 习题学科分布",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (items.isEmpty()) {
                Text(
                    text = "暂无各学科分布数据",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.subject_name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(60.dp)
                            )

                            val fillFraction = (item.count.toFloat() / maxCount).coerceIn(0.05f, 1.0f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(colors.textSecondary.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fillFraction)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(colors.neonBlue)
                                )
                            }

                            Text(
                                text = "${item.count} 题",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.width(44.dp).padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
