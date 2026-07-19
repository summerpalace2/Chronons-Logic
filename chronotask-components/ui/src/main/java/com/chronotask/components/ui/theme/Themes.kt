package com.chronotask.components.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.chronotask.components.ui.R
import androidx.compose.ui.graphics.Color

/**
 * ChronoTask 应用主题枚举。
 *
 * 定义了 5 套完整的 Material 3 配色方案，每套均包含：
 * - 亮色方案 [lightScheme]
 * - 暗色方案 [darkScheme]
 * - 预览色板 [previewColors]（供设置页展示色块）
 *
 * 主题通过 [displayNameResId] 引用多语言资源，通过 [descriptionResId] 引用多语言描述文案。
 * 默认主题为 [default]（[FOREST_SILENCE]：「森林静谧」）。
 */
enum class ChronoTheme(
    /** 主题对外展示的英文名称。 */
    val displayNameResId: Int,
    /** 主题描述文案的多语言资源 ID，用于本地化显示。 */
    val descriptionResId: Int,
    /** 亮色模式下的 Material 3 [ColorScheme]。 */
    val lightScheme: ColorScheme,
    /** 暗色模式下的 Material 3 [ColorScheme]。 */
    val darkScheme: ColorScheme,
    /** 用于设置页色块预览的降采样色板，仅包含代表性颜色。 */
    val previewColors: ThemePreviewColors
) {
    /**
     * 「森林静谧」—— 以深绿为主色调、米色为辅的低饱和度自然系配色。
     *
     * 默认主题，强调护眼与沉浸感，适合长时间阅读与任务管理场景。
     */
    FOREST_SILENCE(
        displayNameResId = R.string.theme_name_forest,
        descriptionResId = R.string.theme_desc_forest,
        lightScheme = forestSilenceLight,
        darkScheme = forestSilenceDark,
        previewColors = forestSilencePreview
    ),

    /**
     * 「深宵暮色」—— 以靛蓝为主色调的夜间系配色。
     *
     * 强调冷峻沉稳的视觉氛围，适合夜间专注场景。
     */
    DEEP_MIDNIGHT(
        displayNameResId = R.string.theme_name_midnight,
        descriptionResId = R.string.theme_desc_midnight,
        lightScheme = deepMidnightLight,
        darkScheme = deepMidnightDark,
        previewColors = deepMidnightPreview
    ),

    /**
     * 「北欧冷灰」—— 以中灰为主色调的极简冷淡系配色。
     *
     * 去除多余色彩干扰，聚焦内容本身。
     */
    NORDIC_GREY(
        displayNameResId = R.string.theme_name_nordic,
        descriptionResId = R.string.theme_desc_nordic,
        lightScheme = nordicGreyLight,
        darkScheme = nordicGreyDark,
        previewColors = nordicGreyPreview
    ),

    /**
     * 「暮色晨曦」—— 以玫红为主色调的优雅暖色系配色。
     *
     * 柔和浪漫，适合偏好暖色调的用户。
     */
    SUNSET_ROSE(
        displayNameResId = R.string.theme_name_sunset,
        descriptionResId = R.string.theme_desc_sunset,
        lightScheme = sunsetRoseLight,
        darkScheme = sunsetRoseDark,
        previewColors = sunsetRosePreview
    ),

    /**
     * 「曜石金沙」—— 以墨黑为底、金色点缀的高对比度奢华配色。
     *
     * 纯黑背景搭配暖金主色，强调视觉冲击力与品质感。
     */
    OBSIDIAN_GOLD(
        displayNameResId = R.string.theme_name_obsidian,
        descriptionResId = R.string.theme_desc_obsidian,
        lightScheme = obsidianGoldLight,
        darkScheme = obsidianGoldDark,
        previewColors = obsidianGoldPreview
    );

    companion object {
        /** 应用默认主题（森林静谧）。 */
        val default = FOREST_SILENCE
    }
}

/**
 * 主题预览色板数据类。
 *
 * 为设置页的色块展示提供降采样的颜色列表，每个元素为「标签 → 颜色」的键值对。
 * 仅保留代表性的角色色（主色、背景、容器、文字、次要色、边框等），
 * 完整的色角色仍由 [ColorScheme] 定义。
 *
 * @property lightColors 亮色模式下的预览色板。
 * @property darkColors  暗色模式下的预览色板。
 */
data class ThemePreviewColors(
    val lightColors: List<Pair<String, Color>>,
    val darkColors: List<Pair<String, Color>>
)

// ─── Forest Silence (森林静谧) ───────────────────────────────────────────────
//
//  设计 rationale：
//  · 主色采用深青绿（#2D4F4F），象征森林的沉稳与静谧
//  · 亮色背景采用略带青调的米色（#F5F7F6），减少纯白的刺眼感
//  · 暗色背景采用极低亮度的墨绿黑（#0B1515），保持色彩一致性
//  · secondary / tertiary 分别采用灰青与薄荷绿，提供层次感而不喧宾夺主
//  · error 采用 Material 标准红，保证错误提示的可识别性
//

/**
 * 「森林静谧」亮色方案。
 *
 * 颜色角色说明：
 * - [primary] / [onPrimary]：主色及其上方文字色，用于强调按钮、选中态等核心元素。
 * - [primaryContainer] / [onPrimaryContainer]：主色容器，用于填充式卡片、标签等。
 * - [inversePrimary]：反相主色，用于深色背景上需要突出主色语义的场景。
 * - [secondary] / [onSecondary]：次要色及其上方文字色，用于次级按钮、筛选标签。
 * - [secondaryContainer] / [onSecondaryContainer]：次要容器面。
 * - [tertiary] / [onTertiary]：第三色，用于点缀与辅助信息。
 * - [error] / [onError] / [errorContainer] / [onErrorContainer]：错误态配色。
 * - [background] / [onBackground]：页面底色及其上方文字色。
 * - [surface] / [onSurface]：组件表面色（卡片、对话框等）及其上方文字色。
 * - [onSurfaceVariant]：表面上的次要文字色（说明、副标题）。
 * - [surfaceVariant]：带色调的表面色，用于需要轻微区分的表面。
 * - [outline] / [outlineVariant]：边框色（variant 为半透明版本，用于弱分割线）。
 * - [surfaceContainer*]：Material 3 容器的五级亮度梯度，从最低 [surfaceContainerLowest]
 *   到最高 [surfaceContainerHighest]，用于表达层级关系。
 */
private val forestSilenceLight = lightColorScheme(
    primary = Color(0xFF2D4F4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D6363),
    onPrimaryContainer = Color(0xFFA2D1D1),
    inversePrimary = Color(0xFFA2D1D1),
    secondary = Color(0xFF516262),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8E7),
    onSecondaryContainer = Color(0xFF2D4F4F),
    tertiary = Color(0xFFACCEBE),
    onTertiary = Color.White,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF5F7F6),
    onBackground = Color(0xFF1A1C1C),
    surface = Color(0xFFF5F7F6),
    onSurface = Color(0xFF1A1C1C),
    onSurfaceVariant = Color(0xFF444848),
    surfaceVariant = Color.White,
    outline = Color(0xFFC0C8C6),
    outlineVariant = Color(0xFFC0C8C6).copy(alpha = 0.5f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F3F2),
    surfaceContainer = Color(0xFFEAEDEC),
    surfaceContainerHigh = Color(0xFFE4E8E6),
    surfaceContainerHighest = Color(0xFFDFE2E1),
)

/**
 * 「森林静谧」暗色方案。
 *
 * 暗色模式将主色反相为浅青绿（#A2D1D1），背景压至极低亮度墨绿黑（#0B1515），
 * 所有 onXxx 颜色使用深色以保证在亮色容器上的可读性。
 * 颜色角色含义同 [forestSilenceLight]。
 */
private val forestSilenceDark = darkColorScheme(
    primary = Color(0xFFA2D1D1),
    onPrimary = Color(0xFF123636),
    primaryContainer = Color(0xFF2D4F4F),
    onPrimaryContainer = Color(0xFFA2D1D1),
    inversePrimary = Color(0xFF2D4F4F),
    secondary = Color(0xFFBFC8C8),
    onSecondary = Color(0xFF2C3737),
    secondaryContainer = Color(0xFF414848),
    onSecondaryContainer = Color(0xFFDAE5E4),
    tertiary = Color(0xFFACCEBE),
    onTertiary = Color(0xFF1A3D2E),
    error = Color(0xFFD32F2F),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1515),
    onBackground = Color(0xFFE0E3E3),
    surface = Color(0xFF0B1515),
    onSurface = Color(0xFFE0E3E3),
    onSurfaceVariant = Color(0xFFBFC8C8),
    surfaceVariant = Color(0xFF131D1D),
    outline = Color(0xFF8B9292),
    outlineVariant = Color(0xFF414848),
    surfaceContainerLowest = Color(0xFF0B1515),
    surfaceContainerLow = Color(0xFF131D1D),
    surfaceContainer = Color(0xFF172121),
    surfaceContainerHigh = Color(0xFF222C2C),
    surfaceContainerHighest = Color(0xFF2C3737),
)

/**
 * 「森林静谧」预览色板。
 *
 * 从 [forestSilenceLight] 与 [forestSilenceDark] 中抽取 10 个代表性颜色，
 * 用于设置页的色块预览展示。每个 Pair 的 [Pair.first] 为中文标签，
 * [Pair.second] 为对应的 [Color] 色值。
 */
private val forestSilencePreview = ThemePreviewColors(
    lightColors = listOf(
        "主色" to Color(0xFF2D4F4F),
        "背景" to Color(0xFFF5F7F6),
        "容器" to Color.White,
        "文字" to Color(0xFF1A1C1C),
        "次要" to Color(0xFF516262),
        "边框" to Color(0xFFC0C8C6),
        "主色容器" to Color(0xFFD5E8E7),
        "第三色" to Color(0xFFACCEBE),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFFE4E8E6),
    ),
    darkColors = listOf(
        "主色" to Color(0xFFA2D1D1),
        "背景" to Color(0xFF0B1515),
        "容器" to Color(0xFF131D1D),
        "文字" to Color(0xFFE0E3E3),
        "次要" to Color(0xFFBFC8C8),
        "边框" to Color(0xFF8B9292),
        "主色容器" to Color(0xFF2D4F4F),
        "第三色" to Color(0xFFACCEBE),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFF2C3737),
    )
)

// ─── 深宵暮色 亮色 / 暗色 / 预览 ───

private val deepMidnightLight = lightColorScheme(
    primary = Color(0xFF2E3A8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF000A5E),
    inversePrimary = Color(0xFFB4C5FF),
    secondary = Color(0xFF575F7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE1F9),
    onSecondaryContainer = Color(0xFF171A2C),
    tertiary = Color(0xFF5D5E61),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF1A1C1E),
    onSurfaceVariant = Color(0xFF44464F),
    surfaceVariant = Color.White,
    outline = Color(0xFFC6C5D3),
    outlineVariant = Color(0xFFC6C5D3).copy(alpha = 0.5f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF5),
    surfaceContainerHigh = Color(0xFFE6E6EC),
    surfaceContainerHighest = Color(0xFFE0E0E7),
)

private val deepMidnightDark = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    onPrimary = Color(0xFF001A64),
    primaryContainer = Color(0xFF3D4590),
    onPrimaryContainer = Color(0xFFDFE0FF),
    inversePrimary = Color(0xFF2E3A8C),
    secondary = Color(0xFFC1C6DC),
    onSecondary = Color(0xFF2A3047),
    secondaryContainer = Color(0xFF40475E),
    onSecondaryContainer = Color(0xFFDDE1F9),
    tertiary = Color(0xFFC6C6C9),
    onTertiary = Color(0xFF303033),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1B1F),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1B1F),
    onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC5C5D0),
    surfaceVariant = Color(0xFF23262E),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFF15161A),
    surfaceContainerLow = Color(0xFF1A1B1F),
    surfaceContainer = Color(0xFF1E1F23),
    surfaceContainerHigh = Color(0xFF292A2E),
    surfaceContainerHighest = Color(0xFF343539),
)

private val deepMidnightPreview = ThemePreviewColors(
    lightColors = listOf(
        "主色" to Color(0xFF2E3A8C),
        "背景" to Color(0xFFF8F9FF),
        "容器" to Color.White,
        "文字" to Color(0xFF1A1C1E),
        "次要" to Color(0xFF575F7A),
        "边框" to Color(0xFFC6C5D3),
        "主色容器" to Color(0xFFDFE0FF),
        "第三色" to Color(0xFF5D5E61),
        "错误" to Color(0xFFBA1A1A),
        "表面变体" to Color(0xFFE6E6EC),
    ),
    darkColors = listOf(
        "主色" to Color(0xFFB4C5FF),
        "背景" to Color(0xFF1A1B1F),
        "容器" to Color(0xFF23262E),
        "文字" to Color(0xFFE3E2E6),
        "次要" to Color(0xFFC1C6DC),
        "边框" to Color(0xFF8E9099),
        "主色容器" to Color(0xFF3D4590),
        "第三色" to Color(0xFFC6C6C9),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFF343539),
    )
)

// ─── 北欧冷灰 亮色 / 暗色 / 预览 ───

private val nordicGreyLight = lightColorScheme(
    primary = Color(0xFF5D5E61),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4E6),
    onPrimaryContainer = Color(0xFF1A1B1D),
    inversePrimary = Color(0xFFC6C6C9),
    secondary = Color(0xFF76777A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8EA),
    onSecondaryContainer = Color(0xFF202123),
    tertiary = Color(0xFF5D5E61),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF2F2F2),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF2F2F2),
    onSurface = Color(0xFF1B1B1B),
    onSurfaceVariant = Color(0xFF454547),
    surfaceVariant = Color.White,
    outline = Color(0xFFC4C4C6),
    outlineVariant = Color(0xFFC4C4C6).copy(alpha = 0.5f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEFEFEF),
    surfaceContainer = Color(0xFFE9E9E9),
    surfaceContainerHigh = Color(0xFFE3E3E3),
    surfaceContainerHighest = Color(0xFFDEDEDE),
)

private val nordicGreyDark = darkColorScheme(
    primary = Color(0xFFC6C6C9),
    onPrimary = Color(0xFF2F2F32),
    primaryContainer = Color(0xFF4A4A4D),
    onPrimaryContainer = Color(0xFFE4E4E6),
    inversePrimary = Color(0xFF5D5E61),
    secondary = Color(0xFFB0B0B3),
    onSecondary = Color(0xFF28282B),
    secondaryContainer = Color(0xFF3E3E41),
    onSecondaryContainer = Color(0xFFE8E8EA),
    tertiary = Color(0xFFC6C6C9),
    onTertiary = Color(0xFF2F2F32),
    error = Color(0xFFC62828),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFFC3C3C5),
    surfaceVariant = Color(0xFF1E1E1E),
    outline = Color(0xFF6E6E71),
    outlineVariant = Color(0xFF454547),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF121212),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF212121),
    surfaceContainerHighest = Color(0xFF2C2C2C),
)

private val nordicGreyPreview = ThemePreviewColors(
    lightColors = listOf(
        "主色" to Color(0xFF5D5E61),
        "背景" to Color(0xFFF2F2F2),
        "容器" to Color.White,
        "文字" to Color(0xFF1B1B1B),
        "次要" to Color(0xFF76777A),
        "边框" to Color(0xFFC4C4C6),
        "主色容器" to Color(0xFFE4E4E6),
        "第三色" to Color(0xFF5D5E61),
        "错误" to Color(0xFFBA1A1A),
        "表面变体" to Color(0xFFE3E3E3),
    ),
    darkColors = listOf(
        "主色" to Color(0xFFC6C6C9),
        "背景" to Color(0xFF121212),
        "容器" to Color(0xFF1E1E1E),
        "文字" to Color(0xFFE6E6E6),
        "次要" to Color(0xFFB0B0B3),
        "边框" to Color(0xFF6E6E71),
        "主色容器" to Color(0xFF4A4A4D),
        "第三色" to Color(0xFFC6C6C9),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFF2C2C2C),
    )
)

// ─── 暮色晨曦 亮色 / 暗色 / 预览 ───

private val sunsetRoseLight = lightColorScheme(
    primary = Color(0xFF8C4A60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF3A071D),
    inversePrimary = Color(0xFFFFB1C8),
    secondary = Color(0xFF7D5A65),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF2F1621),
    tertiary = Color(0xFF8C4A60),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF201A1B),
    onSurfaceVariant = Color(0xFF514345),
    surfaceVariant = Color.White,
    outline = Color(0xFFD0BFC2),
    outlineVariant = Color(0xFFD0BFC2).copy(alpha = 0.5f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF0F0),
    surfaceContainer = Color(0xFFF6E9EA),
    surfaceContainerHigh = Color(0xFFF0E4E4),
    surfaceContainerHighest = Color(0xFFEADEE0),
)

private val sunsetRoseDark = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF551D33),
    primaryContainer = Color(0xFF5B3341),
    onPrimaryContainer = Color(0xFFFFD9E4),
    inversePrimary = Color(0xFF8C4A60),
    secondary = Color(0xFFE0BDC5),
    onSecondary = Color(0xFF402831),
    secondaryContainer = Color(0xFF583F48),
    onSecondaryContainer = Color(0xFFFDD8E1),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF551D33),
    error = Color(0xFF9E1B32),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1F1A1B),
    onBackground = Color(0xFFECE0E1),
    surface = Color(0xFF1F1A1B),
    onSurface = Color(0xFFECE0E1),
    onSurfaceVariant = Color(0xFFD5C1C4),
    surfaceVariant = Color(0xFF2B2223),
    outline = Color(0xFF9D8C8F),
    outlineVariant = Color(0xFF514345),
    surfaceContainerLowest = Color(0xFF1A1516),
    surfaceContainerLow = Color(0xFF1F1A1B),
    surfaceContainer = Color(0xFF241E1F),
    surfaceContainerHigh = Color(0xFF2F292A),
    surfaceContainerHighest = Color(0xFF3A3334),
)

private val sunsetRosePreview = ThemePreviewColors(
    lightColors = listOf(
        "主色" to Color(0xFF8C4A60),
        "背景" to Color(0xFFFFF8F8),
        "容器" to Color.White,
        "文字" to Color(0xFF201A1B),
        "次要" to Color(0xFF7D5A65),
        "边框" to Color(0xFFD0BFC2),
        "主色容器" to Color(0xFFFFD9E4),
        "第三色" to Color(0xFF8C4A60),
        "错误" to Color(0xFFBA1A1A),
        "表面变体" to Color(0xFFF0E4E4),
    ),
    darkColors = listOf(
        "主色" to Color(0xFFFFB1C8),
        "背景" to Color(0xFF1F1A1B),
        "容器" to Color(0xFF2B2223),
        "文字" to Color(0xFFECE0E1),
        "次要" to Color(0xFFE0BDC5),
        "边框" to Color(0xFF9D8C8F),
        "主色容器" to Color(0xFF5B3341),
        "第三色" to Color(0xFFFFB1C8),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFF3A3334),
    )
)

// ─── 曜石金沙 亮色 / 暗色 / 预览 ───

private val obsidianGoldLight = lightColorScheme(
    primary = Color(0xFF745B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDF91),
    onPrimaryContainer = Color(0xFF241A00),
    inversePrimary = Color(0xFFEBC248),
    secondary = Color(0xFF6B5E3E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5E1B9),
    onSecondaryContainer = Color(0xFF231B04),
    tertiary = Color(0xFF745B00),
    onTertiary = Color.White,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF221B00),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF221B00),
    onSurfaceVariant = Color(0xFF4C4635),
    surfaceVariant = Color(0xFFF7F0E0),
    outline = Color(0xFFCEC6AB),
    outlineVariant = Color(0xFFCEC6AB).copy(alpha = 0.5f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2E6),
    surfaceContainer = Color(0xFFF1ECE0),
    surfaceContainerHigh = Color(0xFFEBE6DA),
    surfaceContainerHighest = Color(0xFFE6E1D5),
)

private val obsidianGoldDark = darkColorScheme(
    primary = Color(0xFFEBC248),
    onPrimary = Color(0xFF3D2F00),
    primaryContainer = Color(0xFF545000),
    onPrimaryContainer = Color(0xFFFFDF91),
    inversePrimary = Color(0xFF745B00),
    secondary = Color(0xFFD4C48C),
    onSecondary = Color(0xFF3A3010),
    secondaryContainer = Color(0xFF524726),
    onSecondaryContainer = Color(0xFFF1DFA6),
    tertiary = Color(0xFFEBC248),
    onTertiary = Color(0xFF3D2F00),
    error = Color(0xFFB71C1C),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE9E2D0),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE9E2D0),
    onSurfaceVariant = Color(0xFFD0C8B0),
    surfaceVariant = Color(0xFF1C1C17),
    outline = Color(0xFF8E855C),
    outlineVariant = Color(0xFF4C4635),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0C0C08),
    surfaceContainer = Color(0xFF15150F),
    surfaceContainerHigh = Color(0xFF202019),
    surfaceContainerHighest = Color(0xFF2B2B23),
)

private val obsidianGoldPreview = ThemePreviewColors(
    lightColors = listOf(
        "主色" to Color(0xFF745B00),
        "背景" to Color(0xFFFFFBFF),
        "容器" to Color(0xFFF7F0E0),
        "文字" to Color(0xFF221B00),
        "次要" to Color(0xFF6B5E3E),
        "边框" to Color(0xFFCEC6AB),
        "主色容器" to Color(0xFFFFDF91),
        "第三色" to Color(0xFF745B00),
        "错误" to Color(0xFFBA1A1A),
        "表面变体" to Color(0xFFEBE6DA),
    ),
    darkColors = listOf(
        "主色" to Color(0xFFEBC248),
        "背景" to Color(0xFF000000),
        "容器" to Color(0xFF1C1C17),
        "文字" to Color(0xFFE9E2D0),
        "次要" to Color(0xFFD4C48C),
        "边框" to Color(0xFF8E855C),
        "主色容器" to Color(0xFF545000),
        "第三色" to Color(0xFFEBC248),
        "错误" to Color(0xFFFFB4AB),
        "表面变体" to Color(0xFF2B2B23),
    )
)