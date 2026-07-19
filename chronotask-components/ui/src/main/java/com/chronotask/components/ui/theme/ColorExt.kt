package com.chronotask.components.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * 根据 [LocalAppDark] 自动选择亮/暗颜色
 *
 * 用法：
 * ```kotlin
 * Text(color = LightTextPrimary.dark(DarkTextPrimary))
 * Box(modifier = Modifier.background(LightBackground.dark(DarkBackground)))
 * ```
 *
 * 扩展函数族为 Color 类型提供「亮色默认值，暗色覆盖值」的便捷切换，
 * 避免在业务层散落大量 `if (isDark) x else y` 判断。
 *
 * 重载一：[Color] 参数类型，适用于直接传颜色常量的场景
 * 重载二：[Int] 参数类型，适用于传十六进制字面量的场景
 */

/**
 * [Color] 参数版本的暗色切换扩展
 *
 * @param darkColor 暗色模式下使用的 [Color]；亮色模式下返回 this
 * @return 若当前为暗色模式返回 [darkColor]，否则返回 this
 */
@Composable
@ReadOnlyComposable
fun Color.dark(darkColor: Color): Color {
    return if (!LocalAppDark.current) this else darkColor
}

/**
 * [Int] 参数版本的暗色切换扩展
 *
 * @param darkColor 暗色模式下使用的 ARGB 整数值；亮色模式下返回 this
 * @return 若当前为暗色模式由 [darkColor] 构造 [Color] 并返回，否则返回 this
 */
@Composable
@ReadOnlyComposable
fun Color.dark(darkColor: Int): Color {
    return if (!LocalAppDark.current) this else Color(darkColor)
}