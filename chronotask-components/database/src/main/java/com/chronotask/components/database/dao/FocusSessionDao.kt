package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chronotask.components.database.entity.FocusSessionEntity

/** 已完成专注会话的数据访问对象。 */
@Dao
interface FocusSessionDao {
    /**
     * 保存一次已完成的专注会话。
     *
     * @param session 已完成的会话实体；重复停止回调会被唯一索引安全忽略。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: FocusSessionEntity): Long

    /**
     * 统计时间范围内符合专注规则的会话数量。
     *
     * @param startDate 起始日期时间戳（含）。
     * @param endDate 结束日期时间戳（不含）。
     * @param thresholdSeconds 专注时长阈值，必须严格超过该值。
     */
    @Query(
        "SELECT COUNT(*) FROM focus_sessions " +
            "WHERE date >= :startDate AND date < :endDate " +
            "AND durationSeconds > :thresholdSeconds"
    )
    suspend fun countQualifiedByDateRange(
        startDate: Long,
        endDate: Long,
        thresholdSeconds: Long
    ): Int
}
