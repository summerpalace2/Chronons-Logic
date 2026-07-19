package com.chronotask.components.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 原始颜色常量（调色板层）
 *
 * 本文件定义 chronotask 全部原始 RGB 常量，按语义分组：
 * - 品牌色（Forest Silence 森林静谧）
 * - 亮色模式背景/表面/文字
 * - 暗色模式背景/表面/文字
 * - 功能色（成功/警告/错误）
 * - 任务/标签/导航等专用色
 *
 * ## 二维色板结构
 * 每个语义名都提供亮/暗两份，便于 [com.chronotask.components.ui.theme.AppColors]
 * 在构造 [com.chronotask.components.ui.theme.AppLightColors] / [com.chronotask.components.ui.theme.AppDarkColors]
 * 时直接引用。
 *
 * ## 与 Themes.kt 的关系（重要）
 * [com.chronotask.components.ui.theme.Themes.kt] 中 `forestSilenceLight/Dark`
 * 等 Material [androidx.compose.material3.ColorScheme] 同样包含背景、表面、主色等字段，
 * 其颜色值与本文件中的常量相同或相近（例如 0xFF2D4F4F 等）。
 *
 * 这并非错误，而是有意为之的两层分离：
 * - 本文件（Color.kt）：调色板层，一种「颜色原子名」集合，用于 [AppColors] 语义层
 * - Themes.kt：Material 3 ColorScheme 适配层，直接把颜色注入 Material 组件
 *
 * [AppColors] 的存在理由是 Material ColorScheme 没有
 * `taskRunningBg`、`tagWorkBg`、`navSelected` 等领域语义名，
 * 这些颜色与具体业务含义绑定，需要单独这一层语义映射。两层各司其职，互不替代。
 *
 * 若需同步修改某颜色的 RGB 值，请在本文件与 [com.chronotask.components.ui.theme.Themes.kt] 中
 * 对应位置同时修改，以保持亮/暗色表现一致。
 */

// ============================================================
// 品牌色 - Forest Silence (森林静谧)
// ============================================================

/** 主品牌色，用于选中态、强调按钮等 */
val BrandPrimary = Color(0xFF2D4F4F)

/** 品牌浅色变体，用于浅底背景上的次级强调 */
val BrandLight = Color(0xFF3D6363)

/** 品牌深色变体，用于深底背景上的强调 */
val BrandDark = Color(0xFF1A3333)

/** 品牌柔色，用于文字、图标等低强度品牌色 */
val BrandMuted = Color(0xFFA2D1D1)

// ============================================================
// 亮色模式
// ============================================================

/** 亮色模式页面背景 */
val LightBackground = Color(0xFFF5F7F6)

/** 亮色模式组件表面 */
val LightSurface = Color(0xFFF5F7F6)

/** 亮色模式卡片背景 */
val LightCard = Color(0xFFFFFFFF)

/** 亮色模式分割线 */
val LightDivider = Color(0xFFC0C8C6)

/** 亮色模式主文字（标题/正文） */
val LightTextPrimary = Color(0xFF1A1C1C)

/** 亮色模式次级文字（副标题） */
val LightTextSecondary = Color(0xFF444848)

/** 亮色模式三级文字（占位符/禁用） */
val LightTextTertiary = Color(0xFF747878)

// ============================================================
// 暗色模式 - 森林静谧
// ============================================================

/** 暗色模式页面背景 */
val DarkBackground = Color(0xFF0B1515)

/** 暗色模式组件表面 */
val DarkSurface = Color(0xFF0B1515)

/** 暗色模式卡片背景 */
val DarkCard = Color(0xFF131D1D)

/** 暗色模式分割线 */
val DarkDivider = Color(0xFF516262)

/** 暗色模式主文字（标题/正文） */
val DarkTextPrimary = Color(0xFFE0E3E3)

/** 暗色模式次级文字（副标题） */
val DarkTextSecondary = Color(0xFFBFC8C8)

/** 暗色模式三级文字（占位符/禁用） */
val DarkTextTertiary = Color(0xFF8B9292)

// ============================================================
// 功能色
// ============================================================

/** 主强调色 */
val AccentTeal = Color(0xFFA2D1D1)

/** 次强调色（珊瑚红） */
val AccentCoral = Color(0xFFFFB4AB)

/** 错误/危险 */
val ErrorRed = Color(0xFFFFB4AB)

/** 警告/注意 */
val WarningOrange = Color(0xFFFFB4AB)

/** 成功/完成 */
val SuccessGreen = Color(0xFFA2D1D1)

// ============================================================
// 任务状态色
// ============================================================

/** 任务进行中背景（亮色） */
val TaskRunningBg = Color(0xFFEEF4F3)

/** 任务进行中背景（暗色） */
val TaskRunningBgDark = Color(0xFF172121)

/** 已完成任务背景（亮色） */
val TaskCompletedBg = Color(0xFFE8EDEC)

/** 已完成任务背景（暗色） */
val TaskCompletedBgDark = Color(0xFF0F1818)

/** 超出目标警告色 */
val TaskOverTarget = Color(0xFFFFB4AB)

// ============================================================
// 标签色
// ============================================================

/** 工作标签背景 */
val TagWorkBg = Color(0xFFD5E8E7)

/** 工作标签文字 */
val TagWorkText = Color(0xFF1A3D3C)

/** 学习标签背景 */
val TagStudyBg = Color(0xFFD0E6D8)

/** 学习标签文字 */
val TagStudyText = Color(0xFF1A3D2E)

/** 会议标签背景 */
val TagMeetingBg = Color(0xFFE3E0DA)

/** 会议标签文字 */
val TagMeetingText = Color(0xFF3D3525)

// ============================================================
// 导航栏
// ============================================================

/** 导航栏背景（亮色） */
val NavBgLight = Color(0xFFFFFFFF)

/** 导航栏背景（暗色） */
val NavBgDark = Color(0xFF0B1515)

/** 导航选中项（亮色） */
val NavSelected = Color(0xFF2D4F4F)

/** 导航选中项（暗色） */
val NavSelectedDark = Color(0xFFA2D1D1)

/** 导航未选中项（亮色） */
val NavUnselectedLight = Color(0xFF747878)

/** 导航未选中项（暗色） */
val NavUnselectedDark = Color(0xFF516262)

// ============================================================
// 表面色层级
// ============================================================

/** 最低层级表面（亮色） */
val SurfaceContainerLowest = Color(0xFFFFFFFF)

/** 低层级表面（亮色） */
val SurfaceContainerLow = Color(0xFFF0F3F2)

/** 标准层级表面（亮色） */
val SurfaceContainer = Color(0xFFEAEDEC)

/** 高层级表面（亮色） */
val SurfaceContainerHigh = Color(0xFFE4E8E6)

/** 最高层级表面（亮色） */
val SurfaceContainerHighest = Color(0xFFDFE2E1)

/** 最低层级表面（暗色） */
val SurfaceContainerLowestDark = Color(0xFF0B1515)

/** 低层级表面（暗色） */
val SurfaceContainerLowDark = Color(0xFF131D1D)

/** 标准层级表面（暗色） */
val SurfaceContainerDark = Color(0xFF172121)

/** 高层级表面（暗色） */
val SurfaceContainerHighDark = Color(0xFF222C2C)

/** 最高层级表面（暗色） */
val SurfaceContainerHighestDark = Color(0xFF2C3737)