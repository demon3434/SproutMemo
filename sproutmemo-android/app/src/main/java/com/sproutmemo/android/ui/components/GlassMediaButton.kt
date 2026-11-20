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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.AppIconsMap
import com.sproutmemo.android.ui.theme.LocalAppColors
import com.sproutmemo.android.ui.theme.SproutTextScale

/**
 * 媒体附件区域手账风格功能按钮组件，支持按下动画及禁用态样式。
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
        appColors.accentDeep
    } else {
        textColor
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    val contentTint = if (enabled) resolvedTextColor else secondaryColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isLight) {
                    if (enabled) appColors.neonBlue.copy(alpha = 0.14f) else appColors.paper2.copy(alpha = 0.5f)
                } else {
                    if (enabled) appColors.paper2 else appColors.paper2.copy(alpha = 0.45f)
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) {
                    if (isLight) appColors.neonBlue.copy(alpha = 0.45f) else appColors.edge
                } else {
                    appColors.rule
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) {
                onClick()
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon in AppIconsMap) {
                StationeryIcon(name = icon, size = 18.dp, tint = contentTint)
            } else {
                Text(text = icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentTint,
                fontSize = SproutTextScale.Button,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
