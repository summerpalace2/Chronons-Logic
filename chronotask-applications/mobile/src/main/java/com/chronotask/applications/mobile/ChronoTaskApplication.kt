package com.chronotask.applications.mobile

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.setApplication
import com.chronotask.components.database.entity.FocusSessionEntity
import com.chronotask.components.database.repository.FocusSessionRepository
import com.chronotask.components.database.repository.TaskRecordRepository
import com.chronotask.components.navigation.core.nav3.ensureCollectorsInitialized
import com.chronotask.components.ui.theme.AppLanguage
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.applications.mobile.service.TimerService
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Application — 应用进程级初始化
 *
 * 职责：
 * 1. attachBaseContext 恢复语言配置（直接用 base 参数读 SP，不依赖 appContext）
 * 2. 全局单例注入（appApplication, appContext, appLifecycle）
 * 3. 语言初始化
 * 4. 注册计时会话统一落库和 Foreground Service 回调
 * 5. ProcessLifecycleOwner 监听前后台切换 → TimerManager 和 TimerService
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

        TimerManager.onTimerStarted = { taskId ->
            runCatching { TimerService.start(this, taskId) }
                .onFailure { Log.e(TAG, "启动计时前台服务失败", it) }
        }
        TimerManager.onTimerStopped = {
            TimerService.stop(this)
        }
        TimerManager.onActiveSegmentStopped = { segmentInfo ->
            appIoScope.launch {
                val segment = segmentInfo.segment
                val durationSeconds = (segment.endWallMs - segment.startWallMs) / 1000L
                if (durationSeconds > TimerManager.FOCUS_SESSION_THRESHOLD_SECONDS) {
                    FocusSessionRepository.insert(
                        FocusSessionEntity(
                            taskId = segmentInfo.taskId,
                            date = segmentInfo.sessionStartDay,
                            sessionStartTime = segment.startWallMs,
                            sessionEndTime = segment.endWallMs,
                            durationSeconds = durationSeconds
                        )
                    )
                }
            }
        }
        TimerManager.onSessionStopped = { info ->
            appIoScope.launch {
                if (info.activeSegments.isEmpty()) {
                    TaskRecordRepository.saveTimerResultByDays(
                        taskId = info.taskId,
                        startMs = info.sessionStartWallTime,
                        endMs = info.stopWallMs
                    )
                } else {
                    info.activeSegments.forEach { segment ->
                        TaskRecordRepository.saveTimerResultByDays(
                            taskId = info.taskId,
                            startMs = segment.startWallMs,
                            endMs = segment.endWallMs
                        )
                    }
                }
            }
        }
        TimerManager.restorePersistedSession()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> TimerManager.onAppBackground()
                    Lifecycle.Event.ON_START -> {
                        TimerManager.onAppForeground()
                        TimerService.sync(this)
                    }
                    else -> {}
                }
            }
        )
    }

    private companion object {
        const val TAG = "ChronoTask"
    }
}
