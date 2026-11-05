package com.sproutmemo.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

/**
 * 媒体附件区域带光晕效果的功能按钮组件，支持按下动画及禁用态样式。
 */
@Composable
fun GlassMediaButton(
    icon: String,
    text: String,
    glowColor: Color,
    textColor: Color,
    secondaryColor: Color,
    isLight: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current
    val resolvedTextColor = if (isLight && !appColors.isDark && textColor == Color.White) {
        val r = (glowColor.red * 0.55f).coerceIn(0f, 1f)
        val g = (glowColor.green * 0.55f).coerceIn(0f, 1f)
        val b = (glowColor.blue * 0.55f).coerceIn(0f, 1f)
        Color(r, g, b, 1f)
    } else {
        textColor
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isLight) {
                    if (enabled) glowColor.copy(alpha = 0.22f) else glowColor.copy(alpha = 0.08f)
                } else {
                    if (enabled) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.02f)
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (enabled) {
                        listOf(glowColor.copy(alpha = 0.8f), glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0.6f))
                    } else {
                        listOf(Color.Gray.copy(alpha = 0.35f), Color.Gray.copy(alpha = 0.12f))
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                onClick()
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (enabled) resolvedTextColor else secondaryColor.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
