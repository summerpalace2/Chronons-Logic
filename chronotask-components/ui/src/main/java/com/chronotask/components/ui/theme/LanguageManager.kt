
package com.chronotask.components.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.chronotask.components.common.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import androidx.core.content.edit


/**
 * LanguageManager.kt
 *
 * 语言枚举与全局语言切换模块。
 *
 * 本文件定义了 [AppLanguage] 语言枚举（包含各语言的自身显示名与 [java.util.Locale] 映射），
 * 以及 [LocaleManager] 单例对象——负责读取/应用/持久化 locale 配置，并向 Compose 层暴露
 * 可观察的 [StateFlow] 以驱动字符串资源的实时切换。
 *
 * 实现说明：
 * - [LocaleManager.applyLocale] 当前仍通过 [android.content.res.Resources.updateConfiguration]
 *   更新配置，该方法自 API 起已被标记为废弃。后续应迁移到
 *   `AppCompatDelegate.setApplicationLocales(LocaleListCompat)` 以符合官方推荐方案。
 * - 持久化统一写入 `chrono_preferences` SharedPreferences，键为 [LocaleManager.PREF_KEY]。
 */

/**
 * 语言枚举。
 *
 * 列出 ChronoTask 支持的语言选项，每个枚举值携带该语言在 UI 中展示的自身名称（如 "English" "日本語"）
 * 以及对应的 Java [Locale] 实例。
 *
 * @property displayName 语言在 UI 中展示的自身名称（原生文字）。
 * @property locale 该语言对应的 [java.util.Locale] 实例。
 */
enum class AppLanguage(val displayName: String, val locale: Locale) {
    CHINESE_SIMPLIFIED("简体中文", Locale.SIMPLIFIED_CHINESE),
    CHINESE_TRADITIONAL("繁體中文", Locale.TRADITIONAL_CHINESE),
    ENGLISH("English", Locale.ENGLISH),
    JAPANESE("日本語", Locale.JAPANESE);

    companion object {
        /** 默认语言（简体中文）。 */
        val default = CHINESE_SIMPLIFIED
    }
}

/**
 * 全局语言管理器（单例）
 *
 * 集中管理当前 locale 的读取、应用、持久化，并通过 [currentLocale] StateFlow 向 Compose 层
 * 推送变更，驱动 `stringResource()` 按新 locale 重新拉取字符串。
 *
 * 注意：对象初始化（[_currentLocale]）依赖 [com.chronotask.components.common.appContext]，
 * 必须在 `setApplication()` 之后才能访问此对象。`attachBaseContext` 阶段因为 appContext 不可用，
 * 调用方需自行读取 SharedPreferences（参见 [wrapContextWithLocale]）。
 */
object LocaleManager {
    /** 持久化语言索引的 SharedPreferences 键名。 */
    private const val PREF_KEY = "language_index"

    /**
     * 当前 locale 的可观察数据流。
     *
     * 语言切换时由 [updateLocale] 更新此值 → Compose 收集端重组 → `stringResource()` 按新 locale 读取。
     */
    private val _currentLocale = MutableStateFlow(getSavedLocale())

    /** 对外暴露的只读 [StateFlow]，供 Compose 层 collectAsState 使用。 */
    val currentLocale: StateFlow<Locale> = _currentLocale

    /**
     * 将目标 locale 应用到系统并持久化，但不触发 Compose 重组。
     *
     * 内部依次执行：
     * 1. 调用 [Locale.setDefault] 更新 JVM 默认 locale。
     * 2. 通过 `Configuration.setLocale` + [android.content.res.Resources.updateConfiguration]
     *   更新应用资源配置（已标记废弃，见下方说明）。
     * 3. 将语言索引写入 `chrono_preferences` SharedPreferences。
     *
     * ⚠️ 废弃提示：[android.content.res.Resources.updateConfiguration] 已被标记为废弃。
     * 建议未来替换为 `AppCompatDelegate.setApplicationLocales(LocaleListCompat)`，
     * 以获得更一致的多语言支持并消除弃用警告。
     *
     * @param locale 要应用的 [java.util.Locale]。
     */
    fun applyLocale(locale: Locale) {
        Locale.setDefault(locale)
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        // 持久化
        val idx = AppLanguage.entries.indexOfFirst { it.locale == locale }.coerceAtLeast(0)
        appContext.getSharedPreferences("chrono_preferences", Context.MODE_PRIVATE)
            .edit { putInt(PREF_KEY, idx) }
    }

    /**
     * 应用 locale 并触发 Compose 重组。
     *
     * 在 [applyLocale] 基础上额外更新 [_currentLocale]，使订阅了 [currentLocale] 的
     * Composable 立即重新组合。
     *
     * @param locale 要应用的 [java.util.Locale]。
     */
    fun updateLocale(locale: Locale) {
        applyLocale(locale)
        _currentLocale.value = locale
    }

    /**
     * 从 SharedPreferences 读取已保存的 locale。
     *
     * @return 保存的 [java.util.Locale]；若索引越界则回退到 [AppLanguage.default] 对应的 locale。
     */
    fun getSavedLocale(): Locale {
        val idx = appContext.getSharedPreferences("chrono_preferences", Context.MODE_PRIVATE)
            .getInt(PREF_KEY, 0)
        return AppLanguage.entries.getOrElse(idx) { AppLanguage.default }.locale
    }

    /**
     * 用 SharedPreferences 中的设置包装 Context，返回带有正确 Locale 的 ConfigurationContext。
     *
     * 直接读取 [context] 参数的 SharedPreferences，不依赖 appContext 全局，
     * 因此可在 `Application.attachBaseContext`（appContext 未初始化时）安全使用。
     *
     * @param context 待包装的 [Context]。
     * @return 已应用目标 locale 的 ConfigurationContext（API N+ 以上），或原地更新后的原 context。
     */
    fun wrapContextWithLocale(context: Context): Context {
        val idx = context.getSharedPreferences("chrono_preferences", Context.MODE_PRIVATE)
            .getInt(PREF_KEY, 0)
        val locale = AppLanguage.entries.getOrElse(idx) { AppLanguage.default }.locale
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}