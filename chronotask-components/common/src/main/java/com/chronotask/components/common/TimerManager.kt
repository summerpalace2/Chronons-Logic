package com.chronotask.components.common

import kotlinx.coroutines.Job
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.appDataStore
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * TimerManager — 全局计时器核心
 *
 * 职责：管理单一任务的计时状态，基于 wall-clock 计算经过时间（不受系统时间调整影响）。
 * 设计理念：
 * - elapsedSeconds 由 timerStartTime 实时推导（非累加），避免长跑漂移
 * - 每秒更新 elapsedSeconds 供 UI 显示
 * - 通过 foregroundEvent 通知 ViewModel 从 DB 拉取最新持久化数据
 * - 应用切后台时主动保存当前进度，防止进程被杀导致数据丢失
 */
object TimerManager {
    private val _runningTaskId = MutableStateFlow<Long?>(null)
    val runningTaskId: StateFlow<Long?> = _runningTaskId

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private var timerJob: Job? = null
    private val writeMutex = Mutex()
    private var timerStartTime: Long = 0L
    private var lastSavedSeconds: Long = 0L
    /** 当前会话启动当天的零点毫秒时间戳，跨天拆分时使用 */
    private var sessionStartDay: Long = 0L

    /** 前台回流事件 — 应用从后台恢复时通知 ViewModel 重新加载 DB 数据 */
    private val _foregroundEvent = MutableSharedFlow<Unit>()
    val foregroundEvent: SharedFlow<Unit> = _foregroundEvent.asSharedFlow()

    /**
     * 持久化回调 — Application 层注册，将 (taskId, deltaSeconds) 写入 DB
     */
    private var saveCallback: suspend (Long, Long) -> Unit = { _, _ -> }

    /**
     * 跨天自动停止时的回调 — HomeViewModel 注册，刷新 UI 状态。
     * 参数为 (taskId, sessionStartDay, stopWallMs, totalSeconds)。
     */
    var onDayRollover: (suspend (Long, Long, Long, Long) -> Unit)? = null

    /**
     * 注册计时持久化回调，应在 Application.onCreate 中调用
     *
     * @param callback 接收 (taskId, deltaSeconds) 的挂起函数
     */
    fun setSaveCallback(callback: suspend (Long, Long) -> Unit) {
        saveCallback = callback
    }

    /**
     * 启动计时器
     *
     * 基于 System.currentTimeMillis() 记录起始时间，每秒更新 elapsedSeconds。
     * 若同一任务已在运行则忽略；否则先停止旧计时再重新开始。
     *
     * @param taskId 要计时的任务 ID
     */
    fun startTimer(taskId: Long, offsetMinutes: Int = 0): Boolean {
        if (_runningTaskId.value == taskId) return false
        if (!isWithinTimingWindow(offsetMinutes)) return false
        stopTimer()
        resetTimer()
        _runningTaskId.value = taskId
        timerStartTime = System.currentTimeMillis()
        lastSavedSeconds = 0L
        sessionStartDay = DateUtils.getStartOfDay(timerStartTime, offsetMinutes)

        timerJob = appCoroutineScope.launch {
            while (true) {
                _elapsedSeconds.value = (System.currentTimeMillis() - timerStartTime) / 1000
                // 检测跨天：读取用户配置的 dayStartOffset，当日槽变化时自动停止
                try {
                    val offsetMinutes = appDataStore.dayStartOffsetMinutes.first()
                    val currentDay = DateUtils.getStartOfDay(System.currentTimeMillis(), offsetMinutes)
                    if (currentDay != sessionStartDay) {
                        val info = stopSessionDetailed()
                        if (info != null) {
                            onDayRollover?.invoke(info.taskId, info.sessionStartDay, info.stopWallMs, info.totalSeconds)
                        }
                        return@launch
                    }
                } catch (_: Exception) { /* ignore rollover check errors */ }
                delay(1000)
            }
        }
        return true
    }

    /** 检查当前时间是否在允许计时的窗口内（当前时间 >= 当天起始偏移） */
    fun isWithinTimingWindow(offsetMinutes: Int): Boolean {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.getStartOfDay(now, offsetMinutes)
        return now >= todayStart
    }


    /**
     * 停止计时器并返回未保存的时长
     *
     * @return Pair(taskId, unsavedDelta)，用于调用方（ViewModel）执行最终持久化；
     *         null 表示没有在计时的任务
     */
    fun stopTimer(): Pair<Long, Long>? {
        val taskId = _runningTaskId.value ?: return null
        _runningTaskId.value = null
        timerJob?.cancel()
        timerJob = null
        val elapsed = (System.currentTimeMillis() - timerStartTime) / 1000
        val unsaved = elapsed - lastSavedSeconds
        _elapsedSeconds.value = elapsed
        lastSavedSeconds = 0L
        return taskId to unsaved
    }

    /**
     * 获取当前会话启动日零点（用于跨天拆分）。
     * 未在计时时返回 0。
     */
    fun getSessionStartDay(): Long = if (_runningTaskId.value != null) sessionStartDay else 0L

    /**
     * 会话停止信息（用于跨天按日拆分）。
     *
     * @param taskId      任务 ID
     * @param sessionStartDay 会话启动当天零点时间戳
     * @param stopWallMs  会话停止时的物理时间戳
     * @param totalSeconds 本会话总经过秒数
     */
    data class StopInfo(
        val taskId: Long,
        val sessionStartDay: Long,
        val stopWallMs: Long,
        val totalSeconds: Long
    )

    /**
     * 停止计时器并返回完整的会话停止信息。
     *
     * 与 stopTimer() 等价但额外返回 sessionStartDay + stopWallMs，
     * 便于 Repository 按天切分累加。
     *
     * @return StopInfo，无计时返回 null
     */
    fun stopSessionDetailed(): StopInfo? {
        val taskId = _runningTaskId.value ?: return null
        _runningTaskId.value = null
        timerJob?.cancel()
        timerJob = null
        val stopWallMs = System.currentTimeMillis()
        val elapsed = (stopWallMs - timerStartTime) / 1000
        _elapsedSeconds.value = elapsed
        lastSavedSeconds = 0L
        val info = StopInfo(taskId, sessionStartDay, stopWallMs, elapsed)
        sessionStartDay = 0L
        return info
    }

    /**
     * 重置计时器状态（清零 elapsedSeconds 和 runningTaskId）
     * 注意：stopTimer 前已经返回未保存数据，reset 只是清理本地状态
     */
    fun resetTimer() {
        _runningTaskId.value = null
        _elapsedSeconds.value = 0
    }

    /**
     * 获取当前未保存的时长增量，供 ViewModel 做中间持久化
     *
     * 将 lastSavedSeconds 推进到当前值，下次调用只返回新增量。
     *
     * @return Pair(taskId, deltaSeconds)，无未保存数据时返回 null
     */
    fun getUnsavedDelta(): Pair<Long, Long>? {
        val taskId = _runningTaskId.value ?: return null
        val elapsed = (System.currentTimeMillis() - timerStartTime) / 1000
        val delta = elapsed - lastSavedSeconds
        if (delta <= 0) return null
        lastSavedSeconds = elapsed
        return taskId to delta
    }

    /**
     * 应用切后台时的数据处理
     *
     * 获取未保存增量 → 通过 saveCallback 写入 DB → 重置 timerStartTime
     * 这样即使进程被杀，下次启动也能从 DB 恢复正确总时长。
     *
     * 写入失败时回退 lastSavedSeconds，下次重试。
     */
    fun onAppBackground() {
        val (taskId, delta) = getUnsavedDelta() ?: return
        if (delta <= 0) return
        appIoScope.launch {
            writeMutex.withLock {
                try {
                    saveCallback(taskId, delta)
                    // 写入成功，重置起点：elapsedSeconds 保持当前值，不再重复保存
                    timerStartTime = System.currentTimeMillis()
                    lastSavedSeconds = 0L
                } catch (e: Exception) {
                    // 写入失败，回退 lastSavedSeconds 以便下次重试
                    lastSavedSeconds -= delta
                }
            }
        }
    }

    /**
     * 应用从后台恢复前台时调用
     *
     * 发出 foregroundEvent 通知 ViewModel 从 DB 重新拉取最新总时长，
     * 确保 UI 显示与持久化数据一致。
     */
    fun onAppForeground() {
        _foregroundEvent.tryEmit(Unit)
    }

    /** 获取当前计时会话的起始时间戳 */
    fun getSessionStartTime(): Long = timerStartTime

    /** 获取当前正在计时的任务 ID，无计时返回 null */
    fun getCurrentTaskId(): Long? = _runningTaskId.value

    /** 判断指定任务是否正在计时 */
    fun isRunning(taskId: Long): Boolean = _runningTaskId.value == taskId
}
