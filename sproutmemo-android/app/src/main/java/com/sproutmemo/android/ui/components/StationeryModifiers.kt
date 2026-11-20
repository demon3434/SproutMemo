package com.sproutmemo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.LocalAppColors

fun Modifier.gridPaper(
    paperColor: Color,
    gridColor: Color,
    stepDp: Dp = 24.dp
): Modifier = this
    .background(paperColor)
    .drawBehind {
        val stepPx = stepDp.toPx()
        var x = stepPx
        while (x < size.width) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += stepPx
        }
        var y = stepPx
        while (y < size.height) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += stepPx
        }
    }

/**
 * 横线信纸底纹（与文字 lineHeight 对齐，确保横线始终垫在文字下方不压字）
 */
fun Modifier.linedPaper(
    paperColor: Color,
    lineColor: Color,
    stepDp: Dp = 32.dp,
    topPaddingDp: Dp = 12.dp
): Modifier = this
    .background(paperColor)
    .drawBehind {
        val stepPx = stepDp.toPx()
        var y = topPaddingDp.toPx() + stepPx
        while (y <= size.height - 4.dp.toPx()) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
            y += stepPx
        }
    }

/**
 * 信纸横线正文框（仅用于题目正文/解析，避免整张卡片铺横线穿过控件）
 */
@Composable
fun StationeryLinedNoteBox(
    text: String?,
    emptyPlaceholder: String = "(暂无文字描述)",
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val isEmpty = text.isNullOrBlank()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, colors.rule, RoundedCornerShape(14.dp))
            .linedPaper(
                paperColor = colors.darkBg,
                lineColor = colors.rule,
                stepDp = 32.dp,
                topPaddingDp = 12.dp
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (isEmpty) emptyPlaceholder else text!!,
            fontSize = 15.5.sp,
            lineHeight = 32.sp,
            color = if (isEmpty) colors.textSecondary else colors.textPrimary
        )
    }
}

/**
 * 半透明手账和纸胶带装饰
 */
@Composable
fun StationeryWashiTape(
    modifier: Modifier = Modifier,
    color: Color = Color(0x88F6C945),
    width: Dp = 64.dp,
    height: Dp = 15.dp,
    rotation: Float = -2f
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .rotate(rotation)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

