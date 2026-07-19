/**
 * Type.kt
 *
 * ⚠️ 遗留 / 回退排版定义（LEGACY / FALLBACK）。
 *
 * 本文件定义的 [ChronoTaskTypography] 使用 [FontFamily.Default] 作为所有文本样式的字体，
 * 是一个静态、不依赖外部状态的兜底 [Typography] 实例。
 *
 * 实际运行中，活跃的排版由 [buildTypography] 根据用户字体偏好动态构建
 * （参见 [AppFont] 与 Theme.kt 中的 [ChronoTaskTheme]）。
 * 此文件保留作为默认回退，不建议在新代码中直接引用。
 */
package com.chronotask.components.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ChronoTask 默认排版（遗留 / 回退）。
 *
 * 所有文本样式统一使用 [FontFamily.Default]，不携带任何自定义字体。
 *
 * 注意：当前主题实际使用的是 [buildTypography] 动态生成的排版，本实例仅作兼容保留。
 */
val ChronoTaskTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.8.sp,
        letterSpacing = (-0.02).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
)