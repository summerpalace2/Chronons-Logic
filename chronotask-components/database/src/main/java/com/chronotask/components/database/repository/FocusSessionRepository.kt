package com.chronotask.components.database.repository

import com.chronotask.components.common.appApplication
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.FocusSessionEntity

/** 专注会话仓库，统一封装会话记录的持久化和统计查询。 */
object FocusSessionRepository {
    private val dao = AppDatabase.getDatabase(appApplication).focusSessionDao()

    /**
     * 保存一次完整专注会话。
     *
     * @param session 已完成的会话实体。
     */
    suspend fun insert(session: FocusSessionEntity) {
        dao.insert(session)
    }

    /**
     * 统计时间范围内的专注会话数量。
     *
     * @param startDate 起始日期时间戳（含）。
     * @param endDate 结束日期时间戳（不含）。
     * @param thresholdSeconds 专注时长阈值，必须严格超过该值。
     */
    suspend fun countQualifiedByDateRange(
        startDate: Long,
        endDate: Long,
        thresholdSeconds: Long
    ): Int = dao.countQualifiedByDateRange(startDate, endDate, thresholdSeconds)
}
