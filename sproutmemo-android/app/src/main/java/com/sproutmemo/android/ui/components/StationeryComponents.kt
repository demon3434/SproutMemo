package com.sproutmemo.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.ui.theme.AppIconsMap
import com.sproutmemo.android.ui.theme.IconShape
import com.sproutmemo.android.ui.theme.LocalAppColors

/**
 * 双端共享矢量图标渲染组件（源自 design-system/icons.json -> AppIcons.kt）
 * 若找不到矢量定义则自动使用共享规则里的 fallback Emoji 托底
 */
@Composable
fun StationeryIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color? = null,
    strokeWidth: Float = 2f
) {
    val colors = LocalAppColors.current
    val effectiveTint = tint ?: colors.textPrimary
    val def = AppIconsMap[name]

    if (def == null || def.shapes.isEmpty()) {
        Text(
            text = def?.fallback ?: "•",
            fontSize = (size.value * 0.85f).sp,
            modifier = modifier
        )
        return
    }

    val parsedPaths = remember(name) {
        def.shapes.mapNotNull { shape ->
            if (shape is IconShape.Path) {
                shape.d to PathParser().parsePathString(shape.d).toPath()
            } else null
        }.toMap()
    }

    Canvas(modifier = modifier.size(size)) {
        val scaleX = this.size.width / 24f
        val scaleY = this.size.height / 24f
        withTransform({
            scale(scaleX = scaleX, scaleY = scaleY, pivot = Offset.Zero)
        }) {
            def.shapes.forEach { shape ->
                when (shape) {
                    is IconShape.Path -> {
                        val path = parsedPaths[shape.d] ?: return@forEach
                        val fill = shape.fill
                        if (fill != null && fill != Color.Transparent) {
                            drawPath(path = path, color = fill)
                        }
                        val strokeColor = shape.stroke ?: effectiveTint
                        if (strokeColor != Color.Transparent) {
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style = Stroke(
                                    width = shape.strokeWidth ?: strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                    is IconShape.Circle -> {
                        val fill = shape.fill
                        if (fill != null && fill != Color.Transparent) {
                            drawCircle(
                                color = fill,
                                radius = shape.r,
                                center = Offset(shape.cx, shape.cy)
                            )
                        }
                        val strokeColor = shape.stroke ?: effectiveTint
                        if (strokeColor != Color.Transparent) {
                            drawCircle(
                                color = strokeColor,
                                radius = shape.r,
                                center = Offset(shape.cx, shape.cy),
                                style = Stroke(width = shape.strokeWidth ?: strokeWidth)
                            )
                        }
                    }
                    is IconShape.Rect -> {
                        val fill = shape.fill
                        if (fill != null && fill != Color.Transparent) {
                            drawRoundRect(
                                color = fill,
                                topLeft = Offset(shape.x, shape.y),
                                size = Size(shape.width, shape.height),
                                cornerRadius = CornerRadius(shape.rx, shape.rx)
                            )
                        }
                        val strokeColor = shape.stroke ?: effectiveTint
                        if (strokeColor != Color.Transparent) {
                            drawRoundRect(
                                color = strokeColor,
                                topLeft = Offset(shape.x, shape.y),
                                size = Size(shape.width, shape.height),
                                cornerRadius = CornerRadius(shape.rx, shape.rx),
                                style = Stroke(width = shape.strokeWidth ?: strokeWidth)
                            )
                        }
                    }
                    is IconShape.Ellipse -> {
                        val strokeColor = shape.stroke ?: effectiveTint
                        drawOval(
                            color = strokeColor,
                            topLeft = Offset(shape.cx - shape.rx, shape.cy - shape.ry),
                            size = Size(shape.rx * 2f, shape.ry * 2f),
                            style = Stroke(width = shape.strokeWidth ?: strokeWidth)
                        )
                    }
                    is IconShape.Poly -> {
                        if (shape.points.size >= 2) {
                            val p = Path().apply {
                                moveTo(shape.points.first().x, shape.points.first().y)
                                for (i in 1 until shape.points.size) {
                                    lineTo(shape.points[i].x, shape.points[i].y)
                                }
                                if (shape.closed) close()
                            }
                            val fill = shape.fill
                            if (fill != null && fill != Color.Transparent) {
                                drawPath(path = p, color = fill)
                            }
                            val strokeColor = shape.stroke ?: effectiveTint
                            if (strokeColor != Color.Transparent) {
                                drawPath(
                                    path = p,
                                    color = strokeColor,
                                    style = Stroke(
                                        width = shape.strokeWidth ?: strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 方格手账纸底纹
 */
/**
 * 立体卡纸手账卡片（带底部卡纸立体厚度 edge）
 */
@Composable
fun StationeryCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    borderColor: Color? = null,
    edgeColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val bg = containerColor ?: colors.cardBg
    val border = borderColor ?: colors.rule
    val edge = edgeColor ?: colors.edge

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(edge)
            .padding(bottom = 3.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = bg,
            border = BorderStroke(1.5.dp, border)
        ) {
            Column(content = content)
        }
    }
}

/**
 * 立体卡纸主按钮
 */
@Composable
fun StationeryPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconName: String? = null,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val topOffset = if (isPressed && enabled) 2.dp else 0.dp
    val bottomPad = if (isPressed && enabled) 1.dp else 3.dp

    Box(
        modifier = modifier
            .padding(top = topOffset)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) colors.accentDeep else colors.edge)
            .padding(bottom = bottomPad)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.neonBlue,
                contentColor = Color(0xFFFEFDF9),
                disabledContainerColor = colors.rule,
                disabledContentColor = colors.textSecondary
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (iconName != null) {
                StationeryIcon(name = iconName, size = 18.dp, tint = Color(0xFFFEFDF9))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
        }
    }
}

/**
 * 掌握程度 3 枚手账矢量印章组（完全不会 / 一知半解 / 熟练掌握）
 */
@Composable
fun MasteryStampRow(
    selectedLevel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalAppColors.current
    val items = listOf(
        Triple("CLUELESS", "完全不会", "mastery-clueless"),
        Triple("PARTIAL", "一知半解", "mastery-partial"),
        Triple("MASTERED", "熟练掌握", "mastery-mastered")
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (code, label, icon) ->
            val isSelected = selectedLevel == code
            val activeBg = when (code) {
                "CLUELESS" -> Color(0xFFEF4444)
                "PARTIAL" -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            val activeDeep = when (code) {
                "CLUELESS" -> Color(0xFFB91C1C)
                "PARTIAL" -> Color(0xFFB45309)
                else -> Color(0xFF047857)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) activeDeep else colors.edge)
                    .padding(bottom = 3.dp)
                    .then(if (enabled) Modifier.clickable { onSelect(code) } else Modifier)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) activeBg else colors.cardBg)
                        .border(1.5.dp, if (isSelected) activeDeep else colors.rule, RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationeryIcon(name = icon, size = 18.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFFFEFDF9) else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 糖果渐变进度条
 */
@Composable
fun StationeryCandyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.paper2)
            .border(1.dp, colors.rule, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.neonBlue)
        )
    }
}

/**
 * 复习答题结果三选一立体卡纸按钮组（没做对 / 勉强答对 / 轻松过关）
 */
@Composable
fun ReviewOutcomeRow(
    selectedOutcome: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val outcomes = listOf(
        Triple("WRONG", "没做对", "mastery-clueless"),
        Triple("PARTIALLY_CORRECT", "勉强答对", "mastery-partial"),
        Triple("CORRECT", "轻松过关", "mastery-mastered")
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        outcomes.forEach { (code, label, icon) ->
            val isSelected = selectedOutcome == code
            val activeBg = when (code) {
                "WRONG" -> Color(0xFFEF4444)
                "PARTIALLY_CORRECT" -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
            }
            val activeDeep = when (code) {
                "WRONG" -> Color(0xFFB91C1C)
                "PARTIALLY_CORRECT" -> Color(0xFFB45309)
                else -> Color(0xFF047857)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) activeDeep else colors.edge)
                    .padding(bottom = 3.dp)
                    .clickable { onSelect(code) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) activeBg else colors.cardBg)
                        .border(1.5.dp, if (isSelected) activeDeep else colors.rule, RoundedCornerShape(14.dp))
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StationeryIcon(name = icon, size = 16.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFFFEFDF9) else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 难度星级贴纸栏
 */
@Composable
fun StarRatingStickerBar(
    stars: Int,
    onStarsChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    starSize: Dp = 24.dp,
    showContainer: Boolean = true
) {
    val colors = LocalAppColors.current
    val containerModifier = if (showContainer) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.darkBg)
            .border(1.5.dp, colors.rule, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    } else {
        modifier
    }
    Row(
        modifier = containerModifier,
        horizontalArrangement = Arrangement.spacedBy(if (showContainer) 8.dp else 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val iconModifier = if (onStarsChange != null) {
                Modifier.clickable { onStarsChange(i) }
            } else {
                Modifier
            }
            StationeryIcon(
                name = if (i <= stars) "star-filled" else "star-outline",
                size = starSize,
                modifier = iconModifier
            )
        }
    }
}

