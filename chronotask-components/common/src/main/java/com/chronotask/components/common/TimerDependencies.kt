package com.chronotask.components.common

import android.content.Context
import android.os.SystemClock

/** 计时器所需的墙上时钟与单调时钟。 */
interface TimerClock {
    fun currentTimeMillis(): Long
    fun elapsedRealtimeMillis(): Long
}

/** Android 运行时的真实时钟实现。 */
object AndroidTimerClock : TimerClock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()
}

/** 持久化会话中的一个有效计时片段。 */
data class PersistedTimerSegment(
    val startWallMs: Long,
    val endWallMs: Long
)

/** 进程重建后恢复计时所需的最小会话快照。 */
data class PersistedTimerSession(
    val taskId: Long,
    val sessionStartWallTime: Long,
    val sessionStartDay: Long,
    val dayStartOffsetMinutes: Int?,
    val elapsedSeconds: Long,
    val isPaused: Boolean,
    val activeSegmentStartWallTime: Long,
    val completedSegments: List<PersistedTimerSegment>
)

/** 计时会话的持久化边界，可在测试中替换为内存实现。 */
interface TimerSessionStore {
    fun read(): PersistedTimerSession?

    fun save(session: PersistedTimerSession)

    fun clear()
}

/** 基于 SharedPreferences 的 Android 持久化实现。 */
class SharedPreferencesTimerSessionStore(context: Context) : TimerSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        SESSION_PREFERENCES,
        Context.MODE_PRIVATE
    )

    override fun read(): PersistedTimerSession? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val taskId = preferences.getLong(KEY_TASK_ID, -1L)
        val startWallTime = preferences.getLong(KEY_START_WALL_TIME, 0L)
        val startDay = preferences.getLong(KEY_START_DAY, 0L)
        if (taskId <= 0L || startWallTime <= 0L || startDay <= 0L) {
            clear()
            return null
        }

        return PersistedTimerSession(
            taskId = taskId,
            sessionStartWallTime = startWallTime,
            sessionStartDay = startDay,
            dayStartOffsetMinutes = if (preferences.contains(KEY_DAY_START_OFFSET_MINUTES)) {
                preferences.getInt(KEY_DAY_START_OFFSET_MINUTES, 0).coerceIn(0, 1439)
            } else {
                null
            },
            elapsedSeconds = preferences.getLong(KEY_ELAPSED_SECONDS, 0L).coerceAtLeast(0L),
            isPaused = preferences.getBoolean(KEY_PAUSED, false),
            activeSegmentStartWallTime = preferences.getLong(KEY_ACTIVE_SEGMENT_START, 0L),
            completedSegments = parseSegments(preferences.getString(KEY_COMPLETED_SEGMENTS, null))
        )
    }

    override fun save(session: PersistedTimerSession) {
        preferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_TASK_ID, session.taskId)
            .putLong(KEY_START_WALL_TIME, session.sessionStartWallTime)
            .putLong(KEY_START_DAY, session.sessionStartDay)
            .putLong(KEY_ELAPSED_SECONDS, session.elapsedSeconds)
            .putBoolean(KEY_PAUSED, session.isPaused)
            .putLong(KEY_ACTIVE_SEGMENT_START, session.activeSegmentStartWallTime)
            .putString(
                KEY_COMPLETED_SEGMENTS,
                session.completedSegments.joinToString(";") { segment ->
                    "${segment.startWallMs}:${segment.endWallMs}"
                }
            )
            .also { editor ->
                session.dayStartOffsetMinutes?.let {
                    editor.putInt(KEY_DAY_START_OFFSET_MINUTES, it.coerceIn(0, 1439))
                } ?: editor.remove(KEY_DAY_START_OFFSET_MINUTES)
            }
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private fun parseSegments(serializedSegments: String?): List<PersistedTimerSegment> =
        serializedSegments.orEmpty()
            .split(';')
            .mapNotNull { segmentText ->
                val values = segmentText.split(':')
                if (values.size != 2) return@mapNotNull null
                val startWallMs = values[0].toLongOrNull() ?: return@mapNotNull null
                val endWallMs = values[1].toLongOrNull() ?: return@mapNotNull null
                if (endWallMs <= startWallMs) null else PersistedTimerSegment(startWallMs, endWallMs)
            }

    private companion object {
        const val SESSION_PREFERENCES = "timer_session"
        const val KEY_ACTIVE = "active"
        const val KEY_TASK_ID = "task_id"
        const val KEY_START_WALL_TIME = "start_wall_time"
        const val KEY_START_DAY = "start_day"
        const val KEY_DAY_START_OFFSET_MINUTES = "day_start_offset_minutes"
        const val KEY_ELAPSED_SECONDS = "elapsed_seconds"
        const val KEY_PAUSED = "paused"
        const val KEY_ACTIVE_SEGMENT_START = "active_segment_start"
        const val KEY_COMPLETED_SEGMENTS = "completed_segments"
    }
}
