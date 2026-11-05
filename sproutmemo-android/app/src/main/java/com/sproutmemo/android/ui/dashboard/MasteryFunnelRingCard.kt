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
import com.sproutmemo.android.data.remote.model.StatsOverviewDto
import com.sproutmemo.android.ui.theme.LocalAppColors

@Composable
fun MasteryFunnelRingCard(stats: StatsOverviewDto?) {
    val colors = LocalAppColors.current
    val clueless = stats?.mastery_clueless ?: 0
    val partial = stats?.mastery_partial ?: 0
    val mastered = stats?.mastery_mastered ?: 0
    val total = (clueless + partial + mastered).coerceAtLeast(1)

    val cluelessPct = Math.round((clueless.toFloat() / total) * 100)
    val partialPct = Math.round((partial.toFloat() / total) * 100)
    val masteredPct = Math.round((mastered.toFloat() / total) * 100)

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
                text = "🎯 掌握程度占比",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MasteryRingItem(
                    label = "完全不会",
                    count = clueless,
                    percentage = "$cluelessPct%",
                    color = Color(0xFFEF4444),
                    progress = clueless.toFloat() / total
                )

                MasteryRingItem(
                    label = "一知半解",
                    count = partial,
                    percentage = "$partialPct%",
                    color = Color(0xFFF59E0B),
                    progress = partial.toFloat() / total
                )

                MasteryRingItem(
                    label = "熟练掌握",
                    count = mastered,
                    percentage = "$masteredPct%",
                    color = Color(0xFF10B981),
                    progress = mastered.toFloat() / total
                )
            }
        }
    }
}

@Composable
private fun MasteryRingItem(
    label: String,
    count: Int,
    percentage: String,
    color: Color,
    progress: Float
) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(64.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp,
                trackColor = colors.textSecondary.copy(alpha = 0.15f)
            )
            Text(
                text = percentage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = "$count 题", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(text = label, fontSize = 11.sp, color = colors.textSecondary)
    }
}
