package com.sproutmemo.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sproutmemo.android.R

val SproutFontFamily = FontFamily(
    Font(R.font.lxgw_wenkai_lite_regular, FontWeight.Normal),
    Font(R.font.lxgw_wenkai_lite_regular, FontWeight.Medium),
    Font(R.font.lxgw_wenkai_lite_regular, FontWeight.SemiBold),
    Font(R.font.lxgw_wenkai_lite_regular, FontWeight.Bold)
)

val SproutTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SproutFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun SproutMemoTheme(
    themeId: Int = 18,
    content: @Composable () -> Unit,
) {
    // 仅保留亮色手账主题；若旧配置为暗色 ID，安全回退至默认麦芽纸白 (18)
    val appColors = ThemeList.find { it.id == themeId } ?: DefaultThemeColors

    val colorScheme = lightColorScheme(
        primary = appColors.neonBlue,
        onPrimary = appColors.cardBg,
        background = appColors.darkBg,
        surface = appColors.cardBg,
        surfaceVariant = appColors.paper2,
        outline = appColors.rule,
        onBackground = appColors.textPrimary,
        onSurface = appColors.textPrimary,
        onSurfaceVariant = appColors.textSecondary,
        secondary = appColors.neonGreen,
        error = appColors.neonRed
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SproutTypography,
            content = content
        )
    }
}
