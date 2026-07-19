
package com.chronotask.components.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.chronotask.components.common.appDataStore


/**
 * Theme.kt
 *
 * ChronoTask 主题 Composable 入口模块。
 *
 * 本文件提供 [ChronoTaskTheme] 根 Composable，负责：
 * 1. 读取系统深色模式状态与 DataStore 中保存的字体索引，通过 [buildTypography] 生成动态字体排版。
 * 2. 使用 [androidx.compose.runtime.CompositionLocalProvider] 向子树注入
 *    [LocalAppDark]、[LocalAppColors]、[LocalChronoThemeIndex] 三个自定义 CompositionLocal
 *    （定义于 AppColor.kt / AppDark.kt / ColorExt.kt / ThemeState.kt）。
 * 3. 同步设置状态栏颜色与亮色外观以匹配当前主题背景。
 *
 * 主题持久化由 ThemeState 模块的 `selectTheme()` 完成，本文件仅做读取与渲染。
 */

/**
 * ChronoTask 主题根入口 Composable。
 *
 * 根据系统深色模式设置与 DataStore 中持久化的字体/主题索引，动态构建配色方案与排版，
 * 并通过 CompositionLocal 将主题状态透传到整棵子树。同时自动同步状态栏颜色与外观。
 *
 * @param darkTheme 是否启用深色模式，默认由 [isSystemInDarkTheme] 决定（跟随系统）。
 * @param content 子 Composable 内容，将在 [MaterialTheme] 与自定义 CompositionLocal 的包裹下渲染。
 */
@Composable
fun ChronoTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeState = rememberChronoThemeState()
    val themeIndex by themeState
    val theme = ChronoTheme.entries.getOrElse(themeIndex) { ChronoTheme.default }
    val colorScheme = if (darkTheme) theme.darkScheme else theme.lightScheme

    // 动态字体 + 字号缩放
    val fontIndex by appDataStore.fontIndex.collectAsState(initial = 2)
    val appFont = AppFont.entries.getOrElse(fontIndex) { AppFont.default }
    val fontSize by appDataStore.fontSize.collectAsState(initial = 16)
    val typography = buildTypography(appFont, fontSize.toFloat())

    CompositionLocalProvider(
        LocalAppDark provides darkTheme,
        LocalAppColors provides if (darkTheme) AppDarkColors else AppLightColors,
        LocalChronoThemeIndex provides themeState,
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}