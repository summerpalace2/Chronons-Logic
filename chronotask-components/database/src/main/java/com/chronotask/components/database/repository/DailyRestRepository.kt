package com.chronotask.components.database.repository

import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.DailyRestEntity
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.Flow

/**
 * 每日休息日仓库 - 单例模式 (object)
 *
 * 负责 daily_rest 表的读写操作，提供休息日的查询、设置与删除功能。
 * 通过 DAO 将每日休息状态（isRestDay）持久化，供排班/统计模块使用。
 * 采用单例模式，无状态，仅委托调用 DAO。
 */
object DailyRestRepository {
    private val dao = AppDatabase.getDatabase(appApplication).dailyRestDao()

    /**
     * 按日期查询休息日记录
     * @param date 当日零点的毫秒时间戳
     * @return 对应的实体，不存在时返回 null
     */
    suspend fun getByDate(date: Long) = dao.getRestByDate(date)

    /**
     * 按日期查询休息日记录（响应式 Flow）
     * @param date 当日零点的毫秒时间戳
     * @return 发射对应实体的 Flow，无记录时发射 null
     */
    fun getByDateFlow(date: Long): Flow<DailyRestEntity?> = dao.getRestByDateFlow(date)

    /**
     * 设置指定日期是否为休息日
     * @param date    当日零点的毫秒时间戳
     * @param isRest  true 表示休息日，false 表示工作日
     */
    suspend fun setRestDay(date: Long, isRest: Boolean) {
        dao.setRestDay(DailyRestEntity(date = date, isRestDay = isRest))
    }

    /**
     * 移除指定日期的休息日标记
     * @param date 当日零点的毫秒时间戳
     */
    suspend fun removeRestDay(date: Long) = dao.removeRestDay(date)
}