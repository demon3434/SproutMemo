package com.sproutmemo.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.AppThemeColors

/**
 * 具有强对比度与醒目状态指示的选项卡片 (用于二选一/多选一的互斥类型切换)
 */
@Composable
fun SelectableOptionCard(
    label: String,
    iconText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: AppThemeColors,
    modifier: Modifier = Modifier
) {
    // 亮色高亮主题（例如柠檬黄）防白字不可见：计算 neonBlue 的明度
    val isLightAccent = (colors.neonBlue.red * 0.299f + colors.neonBlue.green * 0.587f + colors.neonBlue.blue * 0.114f) > 0.68f
    val selectedContentColor = if (isLightAccent) Color(0xFF1E293B) else Color.White

    val targetBgColor = if (isSelected) colors.neonBlue else colors.cardBg
    val targetBorderColor = if (isSelected) colors.neonBlue else colors.textSecondary.copy(alpha = 0.25f)
    val targetContentColor = if (isSelected) selectedContentColor else colors.textSecondary

    val animatedBg by animateColorAsState(targetBgColor, animationSpec = tween(200), label = "cardBg")
    val animatedBorder by animateColorAsState(targetBorderColor, animationSpec = tween(200), label = "cardBorder")

    Surface(
        modifier = modifier
            .height(46.dp)
            .shadow(
                elevation = if (isSelected) 3.dp else 0.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = animatedBg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, animatedBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconText,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = targetContentColor
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选",
                    tint = selectedContentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
