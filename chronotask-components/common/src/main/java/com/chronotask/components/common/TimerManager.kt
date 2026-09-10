package com.chronotask.components.common

import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 维护全局唯一的计时会话，并为 UI、前台服务和数据库提供一致状态。
 * 状态转换由 TimerSessionStateMachine 负责，TimerManager 仅处理运行时依赖、协程与回调。
 */
object TimerManager {
    /** 单次有效计时必须严格超过 90 分钟，才计为一次专注。 */
    const val FOCUS_SESSION_THRESHOLD_SECONDS = 90L * 60L

    private val _runningTaskId = MutableStateFlow<Long?>(null)
    val runningTaskId: StateFlow<Long?> = _runningTaskId

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private var timerJob: Job? = null
    private var clock: TimerClock = AndroidTimerClock
    private var sessionStore: TimerSessionStore? = null
    private var sessionState: TimerSessionState? = null

    /** 跨天自动停止时通知页面更新 UI，参数为 taskId、startDay、stopWallMs、totalSeconds。 */
    var onDayRollover: (suspend (Long, Long, Long, Long) -> Unit)? = null

    /** 计时会话启动后通知前台服务启动通知。 */
    var onTimerStarted: ((Long) -> Unit)? = null

    /** 计时会话停止后通知前台服务移除通知。 */
    var onTimerStopped: (() -> Unit)? = null

    /** 会话停止后统一落库，避免 Home、Detail 和 Service 各自重复写入。 */
    var onSessionStopped: ((StopInfo) -> Unit)? = null

    /** 连续计时片段结束后通知持久化层。 */
    var onActiveSegmentStopped: ((ActiveSegmentInfo) -> Unit)? = null

    /**
     * Application 组合运行时依赖；测试可传入假时钟与内存仓库。
     * 运行中切换仓库会破坏恢复语义，因此明确禁止。
     */
    fun configureDependencies(
        sessionStore: TimerSessionStore,
        clock: TimerClock = AndroidTimerClock
    ) {
        check(sessionState == null) { "Cannot replace timer dependencies while a session is active" }
        this.sessionStore = sessionStore
        this.clock = clock
    }

    /** 启动指定任务的计时会话。 */
    fun startTimer(taskId: Long, offsetMinutes: Int = 0): Boolean {
        if (_runningTaskId.value == taskId) return false
        if (!isWithinTimingWindow(offsetMinutes)) return false

        stopTimer()
        resetTimer()
        val startWallMs = clock.currentTimeMillis()
        val dayStartOffsetMinutes = offsetMinutes.coerceIn(0, 1439)
        val state = TimerSessionStateMachine.start(
            taskId = taskId,
            nowWallMs = startWallMs,
            nowElapsedRealtimeMs = clock.elapsedRealtimeMillis(),
            sessionStartDay = DateUtils.getActiveDayMidnight(startWallMs, dayStartOffsetMinutes),
            dayStartOffsetMinutes = dayStartOffsetMinutes
        )
        publishState(state)
        persistSessionState()

        startTimerLoop()
        onTimerStarted?.invoke(taskId)
        return true
    }

    /** 暂停当前计时会话并保留已计时秒数。 */
    fun pauseTimer(): Boolean {
        val state = sessionState ?: return false
        if (state.isPaused) return false
        val transition = TimerSessionStateMachine.pause(
            state = state,
            nowWallMs = clock.currentTimeMillis(),
            nowElapsedRealtimeMs = clock.elapsedRealtimeMillis()
        )
        publishState(transition.state)
        transition.closedSegment?.let { notifyActiveSegmentStopped(transition.state, it) }
        persistSessionState()
        timerJob?.cancel()
        timerJob = null
        return true
    }

    /** 恢复已暂停的计时会话，并以当前单调时钟重新建立计算基准。 */
    fun resumeTimer(): Boolean {
        val state = sessionState ?: return false
        if (!state.isPaused) return false
        publishState(
            TimerSessionStateMachine.resume(
                state = state,
                nowWallMs = clock.currentTimeMillis(),
                nowElapsedRealtimeMs = clock.elapsedRealtimeMillis()
            )
        )
        persistSessionState()
        startTimerLoop()
        return true
    }

    /** 启动唯一的前台刷新循环；已存在活动循环时不重复创建协程。 */
    private fun startTimerLoop() {
        if (sessionState?.isPaused != false || timerJob?.isActive == true) return
        timerJob = appCoroutineScope.launch {
            while (isActive && sessionState?.isPaused == false) {
                refreshElapsed()
                try {
                    val state = sessionState ?: return@launch
                    val currentDay = DateUtils.getActiveDayMidnight(
                        clock.currentTimeMillis(),
                        state.dayStartOffsetMinutes
                    )
                    if (currentDay != state.sessionStartDay) {
                        val info = stopSessionDetailed()
                        if (info != null) {
                            onDayRollover?.invoke(
                                info.taskId,
                                info.sessionStartDay,
                                info.stopWallMs,
                                info.totalSeconds
                            )
                        }
                        return@launch
                    }
                } catch (_: Exception) {
                    // 日槽读取失败不应中断计时，下一轮继续检查。
                }
                delay(1000)
            }
        }
    }

    /** 根据同一个会话起点刷新当前秒数，确保轮询间隔不会造成时间回退。 */
    private fun refreshElapsed() {
        val state = sessionState ?: return
        if (state.isPaused) return
        publishState(TimerSessionStateMachine.refresh(state, clock.elapsedRealtimeMillis()))
    }

    /** 检查当前时间是否位于允许计时的日槽窗口内。 */
    fun isWithinTimingWindow(offsetMinutes: Int): Boolean {
        val now = clock.currentTimeMillis()
        val todayStart = DateUtils.getStartOfDay(now, offsetMinutes)
        return now >= todayStart
    }

    /** 停止计时器并返回本次会话的总时长。 */
    fun stopTimer(): Pair<Long, Long>? {
        val info = stopSessionDetailed() ?: return null
        return info.taskId to info.totalSeconds
    }

    /** 获取当前会话启动日零点；未计时时返回 0。 */
    fun getSessionStartDay(): Long = sessionState?.sessionStartDay ?: 0L

    /** 会话停止信息，供 UI 即时更新和 Repository 按日切分。 */
    data class StopInfo(
        val taskId: Long,
        val sessionStartDay: Long,
        val stopWallMs: Long,
        val totalSeconds: Long,
        val sessionStartWallTime: Long,
        val activeSegments: List<TimeSegment>,
        val dayStartOffsetMinutes: Int
    )

    /** 已结束的连续计时片段。 */
    data class ActiveSegmentInfo(
        val taskId: Long,
        val sessionStartDay: Long,
        val segment: TimeSegment
    )

    /** 一个有效计时区间，结束时间不包含暂停或页面不可见造成的空闲时间。 */
    data class TimeSegment(
        val startWallMs: Long,
        val endWallMs: Long
    )

    /** 停止计时并一次性清理内存状态、恢复状态和服务状态。 */
    fun stopSessionDetailed(): StopInfo? {
        val state = sessionState ?: return null
        val stopWallMs = clock.currentTimeMillis()
        val stopped = TimerSessionStateMachine.stop(
            state = state,
            nowWallMs = stopWallMs,
            nowElapsedRealtimeMs = clock.elapsedRealtimeMillis()
        )
        stopped.closedSegment?.let { notifyActiveSegmentStopped(stopped.state, it) }
        val info = StopInfo(
            taskId = stopped.state.taskId,
            sessionStartDay = stopped.state.sessionStartDay,
            stopWallMs = stopWallMs,
            totalSeconds = stopped.totalSeconds,
            sessionStartWallTime = stopped.state.sessionStartWallTime,
            activeSegments = stopped.state.completedSegments.map { it.toTimeSegment() },
            dayStartOffsetMinutes = stopped.state.dayStartOffsetMinutes
        )

        timerJob?.cancel()
        timerJob = null
        sessionState = null
        _runningTaskId.value = null
        _isPaused.value = false
        _elapsedSeconds.value = info.totalSeconds
        clearPersistedSession()

        onSessionStopped?.invoke(info)
        onTimerStopped?.invoke()
        return info
    }

    /** 清理无活动会话的内存和持久化状态，不触发重复落库。 */
    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        sessionState = null
        _runningTaskId.value = null
        _elapsedSeconds.value = 0L
        _isPaused.value = false
        clearPersistedSession()
    }

    /** 应用进入后台时刷新并持久化当前基准。 */
    fun onAppBackground() {
        if (sessionState == null) return
        refreshElapsed()
        persistSessionState()
    }

    /** 应用回到前台时刷新状态并确保计时循环存在。 */
    fun onAppForeground() {
        if (sessionState == null) return
        refreshElapsed()
        startTimerLoop()
    }

    /** 从进程级持久化状态恢复未结束的计时会话。 */
    fun restorePersistedSession() {
        if (sessionState != null) return
        val persistedSession = requireSessionStore().read() ?: return
        val dayStartOffsetMinutes = persistedSession.dayStartOffsetMinutes ?: run {
            // 兼容旧会话：旧版 start_day 保存的是 X:00 边界，分钟部分就是原日界。
            Calendar.getInstance().apply { timeInMillis = persistedSession.sessionStartDay }
                .let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
        }
        val state = TimerSessionStateMachine.restore(
            persisted = persistedSession,
            nowWallMs = clock.currentTimeMillis(),
            nowElapsedRealtimeMs = clock.elapsedRealtimeMillis(),
            sessionStartDay = DateUtils.getActiveDayMidnight(
                persistedSession.sessionStartWallTime,
                dayStartOffsetMinutes
            ),
            dayStartOffsetMinutes = dayStartOffsetMinutes
        )
        publishState(state)
        if (!state.isPaused) startTimerLoop()
    }

    /** 获取当前计时会话的真实起始时间戳。 */
    fun getSessionStartTime(): Long = sessionState?.sessionStartWallTime ?: 0L

    /** 获取当前正在计时的任务 ID；无计时返回 null。 */
    fun getCurrentTaskId(): Long? = sessionState?.taskId

    /** 判断指定任务是否正在计时。 */
    fun isRunning(taskId: Long): Boolean = sessionState?.taskId == taskId

    /** 写入会话基准，避免进程被系统回收后丢失计时上下文。 */
    private fun persistSessionState() {
        val state = sessionState ?: return
        requireSessionStore().save(TimerSessionStateMachine.snapshot(state))
    }

    private fun publishState(state: TimerSessionState) {
        sessionState = state
        _runningTaskId.value = state.taskId
        _elapsedSeconds.value = state.elapsedSeconds
        _isPaused.value = state.isPaused
    }

    private fun requireSessionStore(): TimerSessionStore = checkNotNull(sessionStore) {
        "TimerManager dependencies must be configured before use"
    }

    /** 删除已经结束的会话恢复信息。 */
    private fun clearPersistedSession() {
        sessionStore?.clear()
    }

    /** 通知上层当前连续计时片段已经结束。 */
    private fun notifyActiveSegmentStopped(
        state: TimerSessionState,
        segment: PersistedTimerSegment
    ) {
        onActiveSegmentStopped?.invoke(
            ActiveSegmentInfo(
                taskId = state.taskId,
                sessionStartDay = state.sessionStartDay,
                segment = segment.toTimeSegment()
            )
        )
    }

    private fun PersistedTimerSegment.toTimeSegment(): TimeSegment =
        TimeSegment(startWallMs, endWallMs)
}
