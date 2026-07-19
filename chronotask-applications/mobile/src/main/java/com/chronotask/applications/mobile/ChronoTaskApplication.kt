package com.chronotask.applications.mobile

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.setApplication
import com.chronotask.components.database.repository.TaskRecordRepository
import com.chronotask.components.navigation.core.nav3.ensureCollectorsInitialized
import com.chronotask.components.ui.theme.AppLanguage
import com.chronotask.components.ui.theme.LocaleManager
import java.util.Locale

/**
 * Application — 应用进程级初始化
 *
 * 职责：
 * 1. attachBaseContext 恢复语言配置（直接用 base 参数读 SP，不依赖 appContext）
 * 2. 全局单例注入（appApplication, appContext, appLifecycle）
 * 3. 语言初始化
 * 4. TimerManager 持久化回调注册
 * 5. ProcessLifecycleOwner 监听前后台切换 → TimerManager
 *
 * 注意：attachBaseContext 执行时 appApplication 尚未初始化（setApplication 在 onCreate），
 *       因此不能调用 LocaleManager（内部依赖 appContext），必须直接用 context 读 SP。
 */
class ChronoTaskApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        val ctx = base ?: this
        val langIdx = ctx.getSharedPreferences("chrono_preferences", Context.MODE_PRIVATE)
            .getInt("language_index", 0)
        val locale = AppLanguage.entries.getOrElse(langIdx) { AppLanguage.default }.locale
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(ctx.createConfigurationContext(config))
    }

    override fun onCreate() {
        super.onCreate()
        setApplication(this)
        ensureCollectorsInitialized()
        LocaleManager.applyLocale(LocaleManager.getSavedLocale())

        TimerManager.setSaveCallback { taskId, deltaSeconds ->
            val today = DateUtils.getDateStart(System.currentTimeMillis())
            TaskRecordRepository.saveTimerResult(taskId, today, deltaSeconds)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> TimerManager.onAppBackground()
                    Lifecycle.Event.ON_START -> TimerManager.onAppForeground()
                    else -> {}
                }
            }
        )
    }
}
