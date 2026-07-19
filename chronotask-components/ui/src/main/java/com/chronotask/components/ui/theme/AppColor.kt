package com.chronotask.components.ui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 应用级语义颜色 CompositionLocal
 *
 * 声明式地提供「当前 AppColors 实例」，由 [ChronoTaskTheme] 根据亮/暗模式
 * 注入 [AppLightColors] 或 [AppDarkColors]。
 *
 * ## 三层色板架构
 * ```
 * Color.kt（原始调色板）  →  AppColor.kt（语义映射）  →  Themes.kt（Material ColorScheme）
 *    原始 RGB 常量             领域语义名                     Material 组件适配
 * ```
 * - **[Color.kt]** — 原始颜色常量，无任何语义，仅作「原子色板」命名
 * - **本文件（AppColor.kt）** — 把原子色组合成名领域语义色集合 [AppColors]，
 *   例如 `taskRunningBg`、`tagWorkBg`、`navSelected`
 * - **[Themes.kt]** — 将颜色直接注入 Material 3 [androidx.compose.material3.ColorScheme]，
 *   使系统组件跟随主题
 *
 * ## 用法
 * ```kotlin
 * val colors = LocalAppColors.current
 * Box(modifier = Modifier.background(colors.background))
 * Text(color = colors.textPrimary)
 * ```
 */
val LocalAppColors: ProvidableCompositionLocal<AppColors> = staticCompositionLocalOf {
    error("未配置 ChronoTaskTheme，请在 setContent 中包裹 ChronoTaskTheme { }")
}

/**
 * 应用级语义颜色集合
 *
 * 按**业务用途**分类，而非按颜色名：
 * - background/surface/card — 层级背景
 * - textPrimary/textSecondary/textTertiary — 文字层级
 * - brandPrimary/brandLight/brandDark — 品牌色
 * - taskRunning/taskCompleted/taskOverTarget — 任务状态
 * - tagWork/tagStudy/tagMeeting — 标签分类
 * - navBackground/navSelected/navUnselected — 导航栏
 *
 * 此类的实例由 [AppLightColors] / [AppDarkColors] 提供，
 * 通过 [LocalAppColors] 在 Composition 树中访问。
 */
open class AppColors(
    // ─── 层级背景 ───
    /** 最底层页面背景 */
    val background: Color,
    /** 组件表面（与 background 同色） */
    val surface: Color,
    /** 卡片容器背景 */
    val card: Color,
    /** 分割线颜色 */
    val divider: Color,

    // ─── 文字层级 ───
    /** 主文字色（标题、正文） */
    val textPrimary: Color,
    /** 次级文字色（副标题、说明） */
    val textSecondary: Color,
    /** 三级文字色（占位符、禁用态） */
    val textTertiary: Color,

    // ─── 品牌色 ───
    /** 主品牌色 */
    val brandPrimary: Color,
    /** 品牌浅色变体 */
    val brandLight: Color,
    /** 品牌深色变体 */
    val brandDark: Color,
    /** 品牌柔色（低强度品牌色） */
    val brandMuted: Color,

    // ─── 功能色 ───
    /** 错误/危险色 */
    val error: Color,
    /** 警告/注意色 */
    val warning: Color,
    /** 成功/完成色 */
    val success: Color,

    // ─── 任务状态 ───
    /** 进行中任务背景 */
    val taskRunningBg: Color,
    /** 已完成任务背景 */
    val taskCompletedBg: Color,
    /** 超出目标警告色 */
    val taskOverTarget: Color,
    /** 进行中任务指示器色 */
    val taskRunningIndicator: Color,

    // ─── 标签色 ───
    /** 工作标签背景 */
    val tagWorkBg: Color,
    /** 工作标签文字 */
    val tagWorkText: Color,
    /** 学习标签背景 */
    val tagStudyBg: Color,
    /** 学习标签文字 */
    val tagStudyText: Color,
    /** 会议标签背景 */
    val tagMeetingBg: Color,
    /** 会议标签文字 */
    val tagMeetingText: Color,

    // ─── 导航栏 ───
    /** 导航栏背景 */
    val navBackground: Color,
    /** 导航选中项 */
    val navSelected: Color,
    /** 导航未选中项 */
    val navUnselected: Color,

    // ─── 表面色层级（Material Container 系列） ───
    /** 最低层级表面 */
    val surfaceContainerLowest: Color,
    /** 低层级表面 */
    val surfaceContainerLow: Color,
    /** 标准层级表面 */
    val surfaceContainer: Color,
    /** 高层级表面 */
    val surfaceContainerHigh: Color,
    /** 最高层级表面 */
    val surfaceContainerHighest: Color,
)

/**
 * 亮色模式下的 [AppColors] 实例，所有颜色取自 [Color.kt] 中亮色常量
 */
object AppLightColors : AppColors(
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    divider = LightDivider,

    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,

    brandPrimary = BrandPrimary,
    brandLight = BrandLight,
    brandDark = BrandDark,
    brandMuted = BrandMuted,

    error = ErrorRed,
    warning = WarningOrange,
    success = SuccessGreen,

    taskRunningBg = TaskRunningBg,
    taskCompletedBg = TaskCompletedBg,
    taskOverTarget = TaskOverTarget,
    taskRunningIndicator = AccentTeal,

    tagWorkBg = TagWorkBg,
    tagWorkText = TagWorkText,
    tagStudyBg = TagStudyBg,
    tagStudyText = TagStudyText,
    tagMeetingBg = TagMeetingBg,
    tagMeetingText = TagMeetingText,

    navBackground = NavBgLight,
    navSelected = NavSelected,
    navUnselected = NavUnselectedLight,

    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
)

/**
 * 暗色模式下的 [AppColors] 实例
 *
 * 部分颜色直接复用亮色常量（如品牌色），更多针对暗色下对比度重做处理
 * （标签背景改用更低 alpha，部分文字改用暗色专属色相）。
 */
object AppDarkColors : AppColors(
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    divider = DarkDivider,

    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,

    brandPrimary = BrandMuted,
    brandLight = BrandLight,
    brandDark = BrandDark,
    brandMuted = BrandMuted,

    error = ErrorRed,
    warning = WarningOrange,
    success = SuccessGreen,

    taskRunningBg = TaskRunningBgDark,
    taskCompletedBg = TaskCompletedBgDark,
    taskOverTarget = TaskOverTarget,
    taskRunningIndicator = BrandMuted,

    tagWorkBg = TagWorkBg.copy(alpha = 0.15f),
    tagWorkText = BrandMuted,
    tagStudyBg = TagStudyBg.copy(alpha = 0.15f),
    tagStudyText = Color(0xFFACCEBE),
    tagMeetingBg = TagMeetingBg.copy(alpha = 0.15f),
    tagMeetingText = DarkTextSecondary,

    navBackground = NavBgDark,
    navSelected = NavSelectedDark,
    navUnselected = NavUnselectedDark,

    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)