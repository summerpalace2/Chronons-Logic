package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 已完成的专注会话。
 *
 * 与按天汇总的 TaskRecordEntity 分开存储，确保统计可以按一次完整会话计数，
 * 而不是把同一天的多个计时片段合并后再猜测专注次数。
 */
@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["date"]),
        Index(value = ["taskId"]),
        Index(value = ["taskId", "sessionStartTime", "sessionEndTime"], unique = true)
    ]
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    /** 会话记录主键。 */
    val id: Long = 0,

    /** 本次会话所属任务 ID。 */
    val taskId: Long,

    /** 会话开始日期的零点时间戳，用于统计周期归属。 */
    val date: Long,

    /** 本次会话实际开始时间戳。 */
    val sessionStartTime: Long,

    /** 用户点击结束时的时间戳。 */
    val sessionEndTime: Long,

    /** 本次会话的有效计时秒数，暂停时间不计入。 */
    val durationSeconds: Long
)
