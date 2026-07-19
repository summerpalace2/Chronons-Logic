/**

 * FontManager.kt

 *

 * 字体枚举与排版构建模块。

 *

 * 本文件定义了 [AppFont] 字体枚举（包含系统字体家族映射），以及 [buildTypography] 函数——

 * 根据选中的字体枚举为 Material3 全量文本样式生成对应的 [Typography]。

 *

 * 注意：当前 [AppFont.family] 字段使用的是系统内置 [FontFamily]（Default / Serif / Monospace），

 * 并未加载真正的自定义字体文件（如 .ttf / .otf）。若后续需要引入自定义字体包，

 * 应使用 `Font(R.font.xxx)` 构建 [FontFamily] 并替换现有字段值。

 */

package com.chronotask.components.ui.theme



import androidx.compose.material3.Typography

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontFamily

import androidx.compose.ui.text.font.FontWeight

import com.chronotask.components.ui.R

import androidx.compose.ui.unit.sp



/**

 * 应用内字体枚举。

 *

 * 定义了 ChronoTask 可供切换的系统字体选项。每个枚举值携带用于 UI 展示的名称/描述资源 ID，

 * 以及对应的系统 [FontFamily]。

 *

 * @property displayNameResId 字体在 UI 中显示的名称字符串资源 ID（如 R.string.font_name_inter）。

 * @property descriptionResId 字体描述信息字符串资源 ID（如 R.string.font_desc_inter）。

 * @property family 该字体对应的系统 [FontFamily]；当前仅使用系统家族，未使用自定义字体文件。

 */

enum class AppFont(val displayNameResId: Int, val descriptionResId: Int, val family: FontFamily) {

    INTER(R.string.font_name_inter, R.string.font_desc_inter, FontFamily.Default),

    ROBOTO(R.string.font_name_roboto, R.string.font_desc_roboto, FontFamily.Serif),

    PINGFANG(R.string.font_name_pingfang, R.string.font_desc_pingfang, FontFamily.Default),

    SOURCE_HAN(R.string.font_name_sourcehan, R.string.font_desc_sourcehan, FontFamily.Monospace);



    companion object {

        /** 默认字体（中文场景偏好 PingFang）。 */

        val default = PINGFANG

    }

}



/**

 * 根据选中的字体 + 字号缩放构建 Material3 [Typography]。

 *

 * 将 [font] 的 [AppFont.family] 应用到全量文本样式，并按 [fontScale]（sp）调整基础字号。

 * 所有字号均基于原始 sp 值乘以缩放因子 [fontScale] / 16.0。

 *

 * @param font 选中的 [AppFont] 枚举值，决定排版使用的字体家族。

 * @param fontScale 用户自定义的基础字号（sp），默认 16。

 * @return 已应用目标字体家族 + 字号缩放的完整 [Typography]。

 */



/**

 * 根据选中的字体 + 字号缩放构建 Material3 [Typography]。

 *

 * 将所有文本样式的基础字号按 [fontScale]（sp）缩放。

 */

fun buildTypography(font: AppFont, fontScale: Float = 16f): Typography {

    val factor = fontScale / 16f

    fun s(base: Float) = base * factor

    return Typography(

        displayLarge = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Bold, fontSize = s(48f).sp, lineHeight = 52.8f.sp, letterSpacing = (-0.02f).sp),

        headlineLarge = TextStyle(fontFamily = font.family, fontWeight = FontWeight.SemiBold, fontSize = s(32f).sp, lineHeight = 40f.sp),

        headlineMedium = TextStyle(fontFamily = font.family, fontWeight = FontWeight.SemiBold, fontSize = s(24f).sp, lineHeight = 32f.sp),

        titleLarge = TextStyle(fontFamily = font.family, fontWeight = FontWeight.SemiBold, fontSize = s(20f).sp, lineHeight = 28f.sp),

        titleMedium = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Medium, fontSize = s(16f).sp, lineHeight = 24f.sp),

        bodyLarge = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Normal, fontSize = s(16f).sp, lineHeight = 24f.sp),

        bodyMedium = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Normal, fontSize = s(14f).sp, lineHeight = 20f.sp),

        bodySmall = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Normal, fontSize = s(12f).sp, lineHeight = 16f.sp),

        labelMedium = TextStyle(fontFamily = font.family, fontWeight = FontWeight.SemiBold, fontSize = s(12f).sp, lineHeight = 16f.sp, letterSpacing = 0.5f.sp),

        labelLarge = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Medium, fontSize = s(18f).sp, lineHeight = 24f.sp),

        labelSmall = TextStyle(fontFamily = font.family, fontWeight = FontWeight.Medium, fontSize = s(11f).sp, lineHeight = 16f.sp, letterSpacing = 0.5f.sp)
        )}

