package com.chronotask.components.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TimerSessionStateMachineTest {
    @Test
    fun pauseResumeStop_excludesPausedTimeAndKeepsBothActiveSegments() {
        val started = TimerSessionStateMachine.start(
            taskId = 7L,
            nowWallMs = 100_000L,
            nowElapsedRealtimeMs = 1_000L,
            sessionStartDay = 0L,
            dayStartOffsetMinutes = 0
        )

        val paused = TimerSessionStateMachine.pause(
            state = started,
            nowWallMs = 105_500L,
            nowElapsedRealtimeMs = 6_500L
        )
        val resumed = TimerSessionStateMachine.resume(
            state = paused.state,
            nowWallMs = 225_500L,
            nowElapsedRealtimeMs = 126_500L
        )
        val stopped = TimerSessionStateMachine.stop(
            state = resumed,
            nowWallMs = 232_600L,
            nowElapsedRealtimeMs = 133_600L
        )

        assertEquals(12L, stopped.totalSeconds)
        assertEquals(
            listOf(
                PersistedTimerSegment(100_000L, 105_500L),
                PersistedTimerSegment(225_500L, 232_600L)
            ),
            stopped.state.completedSegments
        )
        assertEquals(PersistedTimerSegment(225_500L, 232_600L), stopped.closedSegment)
    }

    @Test
    fun restore_usesTheGreaterOfPersistedAndWallClockElapsedTime() {
        val restored = TimerSessionStateMachine.restore(
            persisted = PersistedTimerSession(
                taskId = 7L,
                sessionStartWallTime = 1_000L,
                sessionStartDay = 0L,
                dayStartOffsetMinutes = 0,
                elapsedSeconds = 5L,
                isPaused = false,
                activeSegmentStartWallTime = 10_000L,
                completedSegments = listOf(PersistedTimerSegment(1_000L, 6_000L))
            ),
            nowWallMs = 19_000L,
            nowElapsedRealtimeMs = 20_000L,
            sessionStartDay = 0L,
            dayStartOffsetMinutes = 0
        )
        val refreshed = TimerSessionStateMachine.refresh(restored, 23_000L)

        assertEquals(14L, restored.elapsedSeconds)
        assertEquals(17L, refreshed.elapsedSeconds)
        assertFalse(restored.isPaused)
    }
}
