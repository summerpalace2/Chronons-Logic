package com.chronotask.components.common

/**
 * 与 Android、协程和持久化无关的计时会话状态。
 *
 * TimerManager 负责驱动此状态并将其发布给 UI；所有时长和片段转换集中在这里，
 * 因此暂停、恢复和进程重建恢复可以通过 JVM 单测验证。
 */
internal data class TimerSessionState(
    val taskId: Long,
    val sessionStartElapsedRealtime: Long,
    val sessionStartWallTime: Long,
    val sessionStartDay: Long,
    val dayStartOffsetMinutes: Int,
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val activeSegmentStartWallTime: Long,
    val completedSegments: List<PersistedTimerSegment>
)

/** 一次状态转换中刚刚结束的连续计时片段。 */
internal data class TimerSessionTransition(
    val state: TimerSessionState,
    val closedSegment: PersistedTimerSegment?
)

/** 停止会话时保留的状态与总有效时长。 */
internal data class StoppedTimerSession(
    val state: TimerSessionState,
    val totalSeconds: Long,
    val closedSegment: PersistedTimerSegment?
)

/** 计时会话的纯状态转换规则。 */
internal object TimerSessionStateMachine {
    fun start(
        taskId: Long,
        nowWallMs: Long,
        nowElapsedRealtimeMs: Long,
        sessionStartDay: Long,
        dayStartOffsetMinutes: Int
    ): TimerSessionState = TimerSessionState(
        taskId = taskId,
        sessionStartElapsedRealtime = nowElapsedRealtimeMs,
        sessionStartWallTime = nowWallMs,
        sessionStartDay = sessionStartDay,
        dayStartOffsetMinutes = dayStartOffsetMinutes.coerceIn(0, 1439),
        elapsedSeconds = 0L,
        isPaused = false,
        activeSegmentStartWallTime = nowWallMs,
        completedSegments = emptyList()
    )

    fun refresh(
        state: TimerSessionState,
        nowElapsedRealtimeMs: Long
    ): TimerSessionState {
        if (state.isPaused) return state
        val elapsed = ((nowElapsedRealtimeMs - state.sessionStartElapsedRealtime) / 1000L)
            .coerceAtLeast(0L)
        return state.copy(elapsedSeconds = maxOf(state.elapsedSeconds, elapsed))
    }

    fun pause(
        state: TimerSessionState,
        nowWallMs: Long,
        nowElapsedRealtimeMs: Long
    ): TimerSessionTransition {
        val refreshed = refresh(state, nowElapsedRealtimeMs)
        val (nextState, closedSegment) = closeActiveSegment(refreshed, nowWallMs)
        return TimerSessionTransition(
            state = nextState.copy(isPaused = true, activeSegmentStartWallTime = 0L),
            closedSegment = closedSegment
        )
    }

    fun resume(
        state: TimerSessionState,
        nowWallMs: Long,
        nowElapsedRealtimeMs: Long
    ): TimerSessionState = state.copy(
        sessionStartElapsedRealtime = nowElapsedRealtimeMs - state.elapsedSeconds * 1000L,
        isPaused = false,
        activeSegmentStartWallTime = nowWallMs
    )

    fun stop(
        state: TimerSessionState,
        nowWallMs: Long,
        nowElapsedRealtimeMs: Long
    ): StoppedTimerSession {
        val refreshed = refresh(state, nowElapsedRealtimeMs)
        val transition = if (refreshed.isPaused) {
            TimerSessionTransition(refreshed, null)
        } else {
            val (nextState, closedSegment) = closeActiveSegment(refreshed, nowWallMs)
            TimerSessionTransition(
                state = nextState.copy(activeSegmentStartWallTime = 0L),
                closedSegment = closedSegment
            )
        }
        val totalSeconds = activeDurationMillis(transition.state, nowWallMs) / 1000L
        return StoppedTimerSession(transition.state, totalSeconds, transition.closedSegment)
    }

    fun restore(
        persisted: PersistedTimerSession,
        nowWallMs: Long,
        nowElapsedRealtimeMs: Long,
        sessionStartDay: Long,
        dayStartOffsetMinutes: Int
    ): TimerSessionState {
        val completedSegments = persisted.completedSegments.filter { segment ->
            segment.endWallMs > segment.startWallMs
        }
        val activeSegmentStart = persisted.activeSegmentStartWallTime
            .takeIf { it > 0L }
            ?: if (persisted.isPaused) 0L else persisted.sessionStartWallTime
        val initialState = TimerSessionState(
            taskId = persisted.taskId,
            sessionStartElapsedRealtime = 0L,
            sessionStartWallTime = persisted.sessionStartWallTime,
            sessionStartDay = sessionStartDay,
            dayStartOffsetMinutes = dayStartOffsetMinutes.coerceIn(0, 1439),
            elapsedSeconds = persisted.elapsedSeconds.coerceAtLeast(0L),
            isPaused = persisted.isPaused,
            activeSegmentStartWallTime = activeSegmentStart,
            completedSegments = completedSegments
        )
        val restoredSeconds = maxOf(
            initialState.elapsedSeconds,
            activeDurationMillis(initialState, nowWallMs) / 1000L
        )
        return initialState.copy(
            sessionStartElapsedRealtime = nowElapsedRealtimeMs - restoredSeconds * 1000L,
            elapsedSeconds = restoredSeconds
        )
    }

    fun snapshot(state: TimerSessionState): PersistedTimerSession = PersistedTimerSession(
        taskId = state.taskId,
        sessionStartWallTime = state.sessionStartWallTime,
        sessionStartDay = state.sessionStartDay,
        dayStartOffsetMinutes = state.dayStartOffsetMinutes,
        elapsedSeconds = state.elapsedSeconds,
        isPaused = state.isPaused,
        activeSegmentStartWallTime = state.activeSegmentStartWallTime,
        completedSegments = state.completedSegments
    )

    private fun closeActiveSegment(
        state: TimerSessionState,
        endWallMs: Long
    ): Pair<TimerSessionState, PersistedTimerSegment?> {
        val startWallMs = state.activeSegmentStartWallTime
        if (startWallMs <= 0L || endWallMs <= startWallMs) return state to null
        val closedSegment = PersistedTimerSegment(startWallMs, endWallMs)
        return state.copy(completedSegments = state.completedSegments + closedSegment) to closedSegment
    }

    private fun activeDurationMillis(state: TimerSessionState, nowWallMs: Long): Long {
        val completedMillis = state.completedSegments.sumOf { segment ->
            (segment.endWallMs - segment.startWallMs).coerceAtLeast(0L)
        }
        val activeMillis = if (!state.isPaused && state.activeSegmentStartWallTime > 0L) {
            (nowWallMs - state.activeSegmentStartWallTime).coerceAtLeast(0L)
        } else {
            0L
        }
        return completedMillis + activeMillis
    }
}
