package com.sproutmemo.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 方案 A：手机相机/相册风格的「照片方框 + 90° 旋转弧线箭头」矢量图标
 * @param clockwise true 为顺时针旋转 90°（右转），false 为逆时针旋转 90°（左转）
 */
@Composable
fun PhotoFrameRotateIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = Color.White,
    clockwise: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = (w * 0.095f).coerceAtLeast(1.8f)
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        withTransform({
            if (!clockwise) {
                scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
            }
        }) {
            // 1. 左下角圆角矩形照片框: x=3..15, y=8..21 (基于 24x24 坐标系)
            val rectLeft = w * (3f / 24f)
            val rectTop = h * (8f / 24f)
            val rectW = w * (12f / 24f)
            val rectH = h * (13f / 24f)
            val cornerR = w * (2.2f / 24f)

            drawRoundRect(
                color = tint,
                topLeft = Offset(rectLeft, rectTop),
                size = Size(rectW, rectH),
                cornerRadius = CornerRadius(cornerR, cornerR),
                style = stroke
            )

            // 2. 右上角 90° 旋转弧线: M11 4 h4 a5 5 0 0 1 5 5 v4
            val arcPath = Path().apply {
                moveTo(w * (11f / 24f), h * (4f / 24f))
                lineTo(w * (15f / 24f), h * (4f / 24f))
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = w * (10f / 24f),
                        top = h * (4f / 24f),
                        right = w * (20f / 24f),
                        bottom = h * (14f / 24f)
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(w * (20f / 24f), h * (13f / 24f))
            }
            drawPath(path = arcPath, color = tint, style = stroke)

            // 3. 箭头尖端: M17 10.5 l3 3 l3 -3
            val arrowPath = Path().apply {
                moveTo(w * (17.2f / 24f), h * (10.5f / 24f))
                lineTo(w * (20f / 24f), h * (13.5f / 24f))
                lineTo(w * (22.8f / 24f), h * (10.5f / 24f))
            }
            drawPath(path = arrowPath, color = tint, style = stroke)
        }
    }
}

