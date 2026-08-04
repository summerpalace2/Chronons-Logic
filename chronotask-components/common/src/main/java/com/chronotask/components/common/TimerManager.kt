package com.chronotask.components.common

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * TimerManager.kt
 *
 * 核心职责：维护全局唯一的计时会话，并为 UI、前台服务和数据库提供一致状态。
 * 主要导出：计时启动/暂停/恢复/停止、前后台同步、会话恢复和计时状态流。
 *
 * 计时原则：
 * - 运行时基于 SystemClock.elapsedRealtime() 计算，不依赖轮询累加，避免表观回退。
 * - 会话起点不因页面切换或应用切后台而改变。
 * - SharedPreferences 只保存恢复会话所需的轻量基准，不把它当作计时器。
 * - Foreground Service 负责系统层保活和通知，TimerManager 仍是唯一业务状态源。
 */
object TimerManager {
    /** 单次有效计时必须严格超过 90 分钟，才计为一次专注。 */
    const val FOCUS_SESSION_THRESHOLD_SECONDS = 90L * 60L

    private const val SESSION_PREFERENCES = "timer_session"
    private const val KEY_ACTIVE = "active"
    private const val KEY_TASK_ID = "task_id"
    private const val KEY_START_WALL_TIME = "start_wall_time"
    private const val KEY_START_DAY = "start_day"
    private const val KEY_ELAPSED_SECONDS = "elapsed_seconds"
    private const val KEY_PAUSED = "paused"
    private const val KEY_ACTIVE_SEGMENT_START = "active_segment_start"
    private const val KEY_COMPLETED_SEGMENTS = "completed_segments"

    private val _runningTaskId = MutableStateFlow<Long?>(null)
    val runningTaskId: StateFlow<Long?> = _runningTaskId

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private var timerJob: Job? = null

    /** 单调时钟的会话起点，只在启动或恢复时建立。 */
    private var sessionStartElapsedRealtime: Long = 0L

    /** 墙上时钟的会话起点，用于按日切分和数据库落库。 */
    private var sessionStartWallTime: Long = 0L

    /** 当前会话所属日槽的起点，用于跨日检测。 */
    private var sessionStartDay: Long = 0L

    /** 当前尚未结束的有效计时区间起点；暂停时置为 0。 */
    private var activeSegmentStartWallTime: Long = 0L

    /** 已结束的有效计时区间，按原始墙上时间保存，支持暂停后的精确落库。 */
    private val completedSegments = mutableListOf<TimeSegment>()

    /** 跨天自动停止时通知页面更新 UI，参数为 taskId、startDay、stopWallMs、totalSeconds。 */
    var onDayRollover: (suspend (Long, Long, Long, Long) -> Unit)? = null

    /** 计时会话启动后通知前台服务启动通知。 */
    var onTimerStarted: ((Long) -> Unit)? = null

    /** 计时会话停止后通知前台服务移除通知。 */
    var onTimerStopped: (() -> Unit)? = null

    /** 会话停止后统一落库，避免 Home、Detail 和 Service 各自重复写入。 */
    var onSessionStopped: ((StopInfo) -> Unit)? = null

    /**
     * 连续计时片段结束后通知持久化层。
     *
     * 暂停和停止都会结束当前片段；恢复计时会创建新的片段，
     * 因此上层可以按每个片段独立判断是否达到专注阈值。
     */
    var onActiveSegmentStopped: ((ActiveSegmentInfo) -> Unit)? = null

    /**
     * 启动指定任务的计时会话。
     *
     * @param taskId 要计时的任务 ID。
     * @param offsetMinutes 用户配置的日槽起始偏移（分钟）。
     * @return 成功启动返回 true；同任务已运行或当前不允许计时返回 false。
     */
    fun startTimer(taskId: Long, offsetMinutes: Int = 0): Boolean {
        if (_runningTaskId.value == taskId) return false
        if (!isWithinTimingWindow(offsetMinutes)) return false

        stopTimer()
        resetTimer()
        _runningTaskId.value = taskId
        _isPaused.value = false
        sessionStartElapsedRealtime = SystemClock.elapsedRealtime()
        sessionStartWallTime = System.currentTimeMillis()
        sessionStartDay = DateUtils.getStartOfDay(sessionStartWallTime, offsetMinutes)
        activeSegmentStartWallTime = sessionStartWallTime
        completedSegments.clear()
        persistSessionState()

        startTimerLoop()
        onTimerStarted?.invoke(taskId)
        return true
    }

    /**
     * 暂停当前计时会话并保留已计时秒数。
     *
     * @return 当前是否成功从运行状态切换为暂停状态。
     */
    fun pauseTimer(): Boolean {
        if (_runningTaskId.value == null || _isPaused.value) return false
        refreshElapsed()
        val taskId = _runningTaskId.value ?: return false
        val pauseWallMs = System.currentTimeMillis()
        appendActiveSegment(pauseWallMs)?.let { segment ->
            notifyActiveSegmentStopped(taskId, sessionStartDay, segment)
        }
        activeSegmentStartWallTime = 0L
        _isPaused.value = true
        persistSessionState()
        timerJob?.cancel()
        timerJob = null
        return true
    }

    /**
     * 恢复已暂停的计时会话，并以当前单调时钟重新建立计算基准。
     *
     * @return 当前是否成功从暂停状态切换为运行状态。
     */
    fun resumeTimer(): Boolean {
        if (_runningTaskId.value == null || !_isPaused.value) return false
        sessionStartElapsedRealtime = SystemClock.elapsedRealtime() - _elapsedSeconds.value * 1000L
        activeSegmentStartWallTime = System.currentTimeMillis()
        _isPaused.value = false
        persistSessionState()
        startTimerLoop()
        return true
    }

    /** 启动唯一的前台刷新循环；已存在活动循环时不重复创建协程。 */
    private fun startTimerLoop() {
        if (_isPaused.value || timerJob?.isActive == true) return
        timerJob = appCoroutineScope.launch {
            while (isActive && _runningTaskId.value != null && !_isPaused.value) {
                refreshElapsed()
                try {
                    val offsetMinutes = appDataStore.dayStartOffsetMinutes.first()
                    val currentDay = DateUtils.getStartOfDay(System.currentTimeMillis(), offsetMinutes)
                    if (currentDay != sessionStartDay) {
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
        if (_runningTaskId.value == null || _isPaused.value) return
        val elapsed = (SystemClock.elapsedRealtime() - sessionStartElapsedRealtime) / 1000L
        _elapsedSeconds.value = elapsed.coerceAtLeast(_elapsedSeconds.value)
    }

    /** 检查当前时间是否位于允许计时的日槽窗口内。 */
    fun isWithinTimingWindow(offsetMinutes: Int): Boolean {
        val now = System.currentTimeMillis()
        val todayStart = DateUtils.getStartOfDay(now, offsetMinutes)
        return now >= todayStart
    }

    /**
     * 停止计时器并返回本次会话的总时长。
     *
     * @return taskId 与本次会话秒数；没有运行中的会话时返回 null。
     */
    fun stopTimer(): Pair<Long, Long>? {
        val info = stopSessionDetailed() ?: return null
        return info.taskId to info.totalSeconds
    }

    /** 获取当前会话启动日零点；未计时时返回 0。 */
    fun getSessionStartDay(): Long = if (_runningTaskId.value != null) sessionStartDay else 0L

    /**
     * 会话停止信息，供 UI 即时更新和 Repository 按日切分。
     *
     * @param taskId 本次会话所属任务 ID。
     * @param sessionStartDay 会话启动时所在日槽的起点。
     * @param stopWallMs 停止时的墙上时钟时间戳。
     * @param totalSeconds 本次会话的有效计时秒数。
     * @param sessionStartWallTime 本次会话真实启动时间戳。
     * @param activeSegments 本次会话实际处于运行状态的墙上时间区间。
     */
    data class StopInfo(
        val taskId: Long,
        val sessionStartDay: Long,
        val stopWallMs: Long,
        val totalSeconds: Long,
        val sessionStartWallTime: Long,
        val activeSegments: List<TimeSegment>
    )

    /**
     * 已结束的连续计时片段。
     *
     * @param taskId 片段所属任务 ID。
     * @param sessionStartDay 片段所属会话的日槽起点。
     * @param segment 片段的实际开始与结束时间。
     */
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

    /**
     * 停止计时并一次性清理内存状态、恢复状态和服务状态。
     *
     * @return 完整的 StopInfo；没有计时会话时返回 null。
     */
    fun stopSessionDetailed(): StopInfo? {
        val taskId = _runningTaskId.value ?: return null
        val stopWallMs = System.currentTimeMillis()
        if (!_isPaused.value) {
            appendActiveSegment(stopWallMs)?.let { segment ->
                notifyActiveSegmentStopped(taskId, sessionStartDay, segment)
            }
            activeSegmentStartWallTime = 0L
        }
        val totalSeconds = activeDurationMillis().coerceAtLeast(0L) / 1000L
        val info = StopInfo(
            taskId = taskId,
            sessionStartDay = sessionStartDay,
            stopWallMs = stopWallMs,
            totalSeconds = totalSeconds,
            sessionStartWallTime = sessionStartWallTime,
            activeSegments = completedSegments.toList()
        )

        timerJob?.cancel()
        timerJob = null
        _runningTaskId.value = null
        _isPaused.value = false
        _elapsedSeconds.value = info.totalSeconds
        sessionStartElapsedRealtime = 0L
        sessionStartWallTime = 0L
        sessionStartDay = 0L
        activeSegmentStartWallTime = 0L
        completedSegments.clear()
        clearPersistedSession()

        onSessionStopped?.invoke(info)
        onTimerStopped?.invoke()
        return info
    }

    /** 清理无活动会话的内存和持久化状态，不触发重复落库。 */
    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        _runningTaskId.value = null
        _elapsedSeconds.value = 0L
        _isPaused.value = false
        sessionStartElapsedRealtime = 0L
        sessionStartWallTime = 0L
        sessionStartDay = 0L
        activeSegmentStartWallTime = 0L
        completedSegments.clear()
        clearPersistedSession()
    }

    /** 应用进入后台时刷新并持久化当前基准；不取消计时循环，前台服务可继续使用同一状态源。 */
    fun onAppBackground() {
        if (_runningTaskId.value == null) return
        refreshElapsed()
        persistSessionState()
    }

    /** 应用回到前台时刷新状态并确保计时循环存在。 */
    fun onAppForeground() {
        if (_runningTaskId.value == null) return
        refreshElapsed()
        startTimerLoop()
    }

    /**
     * 从进程级持久化状态恢复未结束的计时会话。
     *
     * 恢复使用墙上时钟与已保存秒数的较大值，避免进程重建或设备重启后时间倒退。
     * 应在 Application 完成 appApplication 注入后调用一次。
     */
    fun restorePersistedSession() {
        if (_runningTaskId.value != null) return
        val preferences = sessionPreferences
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return

        val taskId = preferences.getLong(KEY_TASK_ID, -1L)
        val startWallTime = preferences.getLong(KEY_START_WALL_TIME, 0L)
        val startDay = preferences.getLong(KEY_START_DAY, 0L)
        if (taskId <= 0L || startWallTime <= 0L || startDay <= 0L) {
            clearPersistedSession()
            return
        }

        val savedElapsed = preferences.getLong(KEY_ELAPSED_SECONDS, 0L).coerceAtLeast(0L)
        _runningTaskId.value = taskId
        _isPaused.value = preferences.getBoolean(KEY_PAUSED, false)
        sessionStartWallTime = startWallTime
        sessionStartDay = startDay
        completedSegments.clear()
        completedSegments += parseCompletedSegments(
            preferences.getString(KEY_COMPLETED_SEGMENTS, null)
        )
        activeSegmentStartWallTime = preferences.getLong(KEY_ACTIVE_SEGMENT_START, 0L)
            .takeIf { it > 0L }
            ?: if (_isPaused.value) 0L else startWallTime
        val restoredElapsed = activeDurationMillis(System.currentTimeMillis()) / 1000L
        _elapsedSeconds.value = maxOf(savedElapsed, restoredElapsed)
        sessionStartElapsedRealtime = SystemClock.elapsedRealtime() - _elapsedSeconds.value * 1000L
        if (!_isPaused.value) startTimerLoop()
    }

    /** 获取当前计时会话的真实起始时间戳。 */
    fun getSessionStartTime(): Long = sessionStartWallTime

    /** 获取当前正在计时的任务 ID；无计时返回 null。 */
    fun getCurrentTaskId(): Long? = _runningTaskId.value

    /** 判断指定任务是否正在计时。 */
    fun isRunning(taskId: Long): Boolean = _runningTaskId.value == taskId

    /** 读取仅用于恢复会话的进程级轻量配置。 */
    private val sessionPreferences
        get() = appContext.getSharedPreferences(SESSION_PREFERENCES, Context.MODE_PRIVATE)

    /** 写入会话基准，避免进程被系统回收后丢失计时上下文。 */
    private fun persistSessionState() {
        val taskId = _runningTaskId.value ?: return
        sessionPreferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_TASK_ID, taskId)
            .putLong(KEY_START_WALL_TIME, sessionStartWallTime)
            .putLong(KEY_START_DAY, sessionStartDay)
            .putLong(KEY_ELAPSED_SECONDS, _elapsedSeconds.value)
            .putBoolean(KEY_PAUSED, _isPaused.value)
            .putLong(KEY_ACTIVE_SEGMENT_START, activeSegmentStartWallTime)
            .putString(
                KEY_COMPLETED_SEGMENTS,
                completedSegments.joinToString(";") { segment ->
                    "${segment.startWallMs}:${segment.endWallMs}"
                }
            )
            .apply()
    }

    /** 将当前有效区间封存，避免暂停时间被计算进数据库。 */
    private fun appendActiveSegment(endWallMs: Long): TimeSegment? {
        if (activeSegmentStartWallTime <= 0L || endWallMs <= activeSegmentStartWallTime) return null
        return TimeSegment(activeSegmentStartWallTime, endWallMs).also { segment ->
            completedSegments += segment
        }
    }

    /** 通知上层当前连续计时片段已经结束。 */
    private fun notifyActiveSegmentStopped(
        taskId: Long,
        sessionStartDay: Long,
        segment: TimeSegment
    ) {
        onActiveSegmentStopped?.invoke(
            ActiveSegmentInfo(
                taskId = taskId,
                sessionStartDay = sessionStartDay,
                segment = segment
            )
        )
    }

    /** 计算已完成区间与当前运行区间的总毫秒数，避免逐段取整造成累计误差。 */
    private fun activeDurationMillis(nowWallMs: Long = System.currentTimeMillis()): Long {
        val completedMillis = completedSegments.sumOf { segment ->
            (segment.endWallMs - segment.startWallMs).coerceAtLeast(0L)
        }
        val activeMillis = if (!_isPaused.value && activeSegmentStartWallTime > 0L) {
            (nowWallMs - activeSegmentStartWallTime).coerceAtLeast(0L)
        } else {
            0L
        }
        return completedMillis + activeMillis
    }

    /** 解析进程重建前保存的有效计时区间。 */
    private fun parseCompletedSegments(serializedSegments: String?): List<TimeSegment> {
        return serializedSegments.orEmpty()
            .split(';')
            .mapNotNull { segmentText ->
                val values = segmentText.split(':')
                if (values.size != 2) return@mapNotNull null
                val startWallMs = values[0].toLongOrNull() ?: return@mapNotNull null
                val endWallMs = values[1].toLongOrNull() ?: return@mapNotNull null
                if (endWallMs <= startWallMs) null else TimeSegment(startWallMs, endWallMs)
            }
    }

    /** 删除已经结束的会话恢复信息。 */
    private fun clearPersistedSession() {
        sessionPreferences.edit().clear().apply()
    }
}
