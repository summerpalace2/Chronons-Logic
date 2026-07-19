package com.chronotask.components.ui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 暗色模式状态 CompositionLocal
 *
 * 在整个主题系统中提供「当前是否为暗色模式」的布尔信号，
 * 由 [com.chronotask.components.ui.theme.ChronoTaskTheme] 在 Composition 树的根节点注入。
 *
 * ## 架构角色
 * 本模块属于 chronotask-components 主题层，与 [LocalAppColors] 并列，
 * 一个携带「值（语义色）」，一个携带「状态（亮/暗）」。
 *
 * ## 用法
 * ```kotlin
 * val isDark = LocalAppDark.current
 * // 通常配合 Color.dark() 扩展使用，无需直接读取此值
 * ```
 *
 * 若在 [com.chronotask.components.ui.theme.ChronoTaskTheme] 之外访问，
 * 将抛出 [IllegalStateException]，提示需包裹主题。
 */
val LocalAppDark: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf {
    error("未配置 ChronoTaskTheme，请在 setContent 中包裹 ChronoTaskTheme { }")
}