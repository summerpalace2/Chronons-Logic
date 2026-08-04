package com.chronotask.components.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chronotask.components.ui.R

/**
 * 应用内可选字体及其 Material 3 排版构建工具。
 *
 * 字体资源使用 Android 可下载字体 XML，由 Compose 的 [Font] 转换为 [FontFamily]。
 * 当设备无法从字体提供方下载字体时，Android 会按字体族回退机制选择可用字体。
 */
enum class AppFont(
    val displayNameResId: Int,
    val descriptionResId: Int,
    val family: FontFamily
) {
    INTER(
        R.string.font_name_inter,
        R.string.font_desc_inter,
        FontFamily(Font(R.font.inter))
    ),
    ROBOTO(
        R.string.font_name_roboto,
        R.string.font_desc_roboto,
        FontFamily(Font(R.font.roboto))
    ),
    NOTO_SANS_SC(
        R.string.font_name_noto_sans_sc,
        R.string.font_desc_noto_sans_sc,
        FontFamily(Font(R.font.noto_sans_sc))
    );

    companion object {
        /** 默认字体，优先使用中文字符集覆盖更完整的 Noto Sans SC。 */
        val default: AppFont = NOTO_SANS_SC
    }
}

/**
 * 根据选中的字体和字号构建完整的 Material 3 [Typography]。
 *
 * 字号和行高使用同一个缩放因子，避免用户放大字号后文本行高仍保持原值，
 * 从而降低中文文本拥挤和裁切的风险。
 *
 * @param font 选中的字体。
 * @param fontScale 用户设置的基础字号，单位为 sp，默认值为 16sp。
 */
fun buildTypography(font: AppFont, fontScale: Float = 16f): Typography {
    val factor = fontScale / 16f
    fun scale(value: Float): Float = value * factor

    return Typography(
        displayLarge = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Bold,
            fontSize = scale(48f).sp,
            lineHeight = scale(52.8f).sp,
            letterSpacing = (-0.02f).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(32f).sp,
            lineHeight = scale(40f).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(24f).sp,
            lineHeight = scale(32f).sp
        ),
        titleLarge = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(20f).sp,
            lineHeight = scale(28f).sp
        ),
        titleMedium = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Medium,
            fontSize = scale(16f).sp,
            lineHeight = scale(24f).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Normal,
            fontSize = scale(16f).sp,
            lineHeight = scale(24f).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Normal,
            fontSize = scale(14f).sp,
            lineHeight = scale(20f).sp
        ),
        bodySmall = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Normal,
            fontSize = scale(12f).sp,
            lineHeight = scale(16f).sp
        ),
        labelMedium = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scale(12f).sp,
            lineHeight = scale(16f).sp,
            letterSpacing = 0.5.sp
        ),
        labelLarge = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Medium,
            fontSize = scale(18f).sp,
            lineHeight = scale(24f).sp
        ),
        labelSmall = TextStyle(
            fontFamily = font.family,
            fontWeight = FontWeight.Medium,
            fontSize = scale(11f).sp,
            lineHeight = scale(16f).sp,
            letterSpacing = 0.5.sp
        )
    )
}
