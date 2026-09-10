package com.chronotask.components.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerManagerDependencyTest {
    private val clock = FakeTimerClock(
        wallTimeMillis = 1_767_225_600_000L,
        elapsedRealtimeMillis = 1_000L
    )
    private val sessionStore = InMemoryTimerSessionStore()
    private var originalTimerStarted: ((Long) -> Unit)? = null
    private var originalTimerStopped: (() -> Unit)? = null
    private var originalSessionStopped: ((TimerManager.StopInfo) -> Unit)? = null
    private var originalActiveSegmentStopped: ((TimerManager.ActiveSegmentInfo) -> Unit)? = null
    private var originalDayRollover: (suspend (Long, Long, Long, Long) -> Unit)? = null

    @Before
    fun setUp() {
        TimerManager.resetTimer()
        originalTimerStarted = TimerManager.onTimerStarted
        originalTimerStopped = TimerManager.onTimerStopped
        originalSessionStopped = TimerManager.onSessionStopped
        originalActiveSegmentStopped = TimerManager.onActiveSegmentStopped
        originalDayRollover = TimerManager.onDayRollover
        TimerManager.onTimerStarted = null
        TimerManager.onTimerStopped = null
        TimerManager.onSessionStopped = null
        TimerManager.onActiveSegmentStopped = null
        TimerManager.onDayRollover = null
        TimerManager.configureDependencies(sessionStore = sessionStore, clock = clock)
    }

    @After
    fun tearDown() {
        TimerManager.resetTimer()
        TimerManager.configureDependencies(
            sessionStore = SharedPreferencesTimerSessionStore(
                InstrumentationRegistry.getInstrumentation().targetContext
            ),
            clock = AndroidTimerClock
        )
        TimerManager.onTimerStarted = originalTimerStarted
        TimerManager.onTimerStopped = originalTimerStopped
        TimerManager.onSessionStopped = originalSessionStopped
        TimerManager.onActiveSegmentStopped = originalActiveSegmentStopped
        TimerManager.onDayRollover = originalDayRollover
    }

    @Test
    fun pause_usesInjectedClockAndPersistsSessionThroughInjectedStore() {
        assertTrue(TimerManager.startTimer(taskId = 42L, offsetMinutes = 60))

        clock.advanceBy(5_000L)
        assertTrue(TimerManager.pauseTimer())

        assertEquals(5L, TimerManager.elapsedSeconds.value)
        val persisted = sessionStore.read()
        assertNotNull(persisted)
        assertEquals(42L, persisted?.taskId)
        assertEquals(5L, persisted?.elapsedSeconds)
        assertTrue(persisted?.isPaused == true)
        assertEquals(1, persisted?.completedSegments?.size)

        TimerManager.resetTimer()
        assertFalse(sessionStore.hasSession())
    }

    @Test
    fun restorePersistedPausedSession_resumesWithoutCountingThePausedGap() {
        val sessionStartWallTime = clock.currentTimeMillis() - 5_000L
        val dayStartOffsetMinutes = 60
        sessionStore.seed(
            PersistedTimerSession(
                taskId = 42L,
                sessionStartWallTime = sessionStartWallTime,
                sessionStartDay = DateUtils.getActiveDayMidnight(
                    sessionStartWallTime,
                    dayStartOffsetMinutes
                ),
                dayStartOffsetMinutes = dayStartOffsetMinutes,
                elapsedSeconds = 5L,
                isPaused = true,
                activeSegmentStartWallTime = 0L,
                completedSegments = listOf(
                    PersistedTimerSegment(sessionStartWallTime, clock.currentTimeMillis())
                )
            )
        )

        TimerManager.restorePersistedSession()

        assertEquals(42L, TimerManager.getCurrentTaskId())
        assertTrue(TimerManager.isPaused.value)
        assertEquals(5L, TimerManager.elapsedSeconds.value)
        assertEquals(
            DateUtils.getActiveDayMidnight(sessionStartWallTime, dayStartOffsetMinutes),
            TimerManager.getSessionStartDay()
        )

        clock.advanceBy(60_000L)
        assertTrue(TimerManager.resumeTimer())
        clock.advanceBy(7_000L)
        assertTrue(TimerManager.pauseTimer())

        assertEquals(12L, TimerManager.elapsedSeconds.value)
        assertEquals(2, sessionStore.read()?.completedSegments?.size)
    }

    private class FakeTimerClock(
        private var wallTimeMillis: Long,
        private var elapsedRealtimeMillis: Long
    ) : TimerClock {
        override fun currentTimeMillis(): Long = wallTimeMillis

        override fun elapsedRealtimeMillis(): Long = elapsedRealtimeMillis

        fun advanceBy(millis: Long) {
            wallTimeMillis += millis
            elapsedRealtimeMillis += millis
        }
    }

    private class InMemoryTimerSessionStore : TimerSessionStore {
        private var session: PersistedTimerSession? = null

        override fun read(): PersistedTimerSession? = session

        override fun save(session: PersistedTimerSession) {
            this.session = session.copy(completedSegments = session.completedSegments.toList())
        }

        override fun clear() {
            session = null
        }

        fun seed(session: PersistedTimerSession) {
            this.session = session.copy(completedSegments = session.completedSegments.toList())
        }

        fun hasSession(): Boolean = session != null
    }
}
