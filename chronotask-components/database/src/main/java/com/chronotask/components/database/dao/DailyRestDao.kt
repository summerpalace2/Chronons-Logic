package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chronotask.components.database.entity.DailyRestEntity
import kotlinx.coroutines.flow.Flow

/**
 * 每日休息日配置 DAO
 *
 * 负责 [DailyRestEntity] 表的增删改查，用于管理用户标记的"休息日"信息。
 * 休息日功能允许用户在特定日期停用计时，适用于节假日或请假场景。
 *
 * 在架构中属于数据访问层（DAO 层），由 Room 实现具体 SQL 操作。
 */
@Dao
interface DailyRestDao {
    /**
     * 根据日期获取单条休息日配置
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 匹配的 [DailyRestEntity]，若不存在则返回 null
     */
    @Query("SELECT * FROM daily_rest WHERE date = :date LIMIT 1")
    suspend fun getRestByDate(date: Long): DailyRestEntity?

    /**
     * 以 Flow 形式根据日期获取单条休息日配置
     *
     * 当数据库中对应记录发生变化时，Flow 会自动发射最新值，适用于 UI 实时订阅。
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射 [DailyRestEntity?] 的 Flow，若不存在则发射 null
     */
    @Query("SELECT * FROM daily_rest WHERE date = :date LIMIT 1")
    fun getRestByDateFlow(date: Long): Flow<DailyRestEntity?>

    /**
     * 查询指定日期范围内的所有休息日
     *
     * @param startDate 范围起始日期（Unix 时间戳，毫秒，包含）
     * @param endDate 范围结束日期（Unix 时间戳，毫秒，包含）
     * @return 该范围内标记为休息日的 [DailyRestEntity] 列表
     */
    @Query("SELECT * FROM daily_rest WHERE date >= :startDate AND date <= :endDate AND isRestDay = 1")
    suspend fun getRestDaysInRange(startDate: Long, endDate: Long): List<DailyRestEntity>

    /**
     * 插入或更新一条休息日配置
     *
     * 若主键冲突则覆盖已有记录，适用于 toggle 场景（标记/取消休息日）。
     *
     * @param dailyRest 要持久化的 [DailyRestEntity] 实例
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setRestDay(dailyRest: DailyRestEntity)

    /**
     * 根据日期删除休息日配置
     *
     * @param date 要取消休息日标记的日期（Unix 时间戳，毫秒）
     */
    @Query("DELETE FROM daily_rest WHERE date = :date")
    suspend fun removeRestDay(date: Long)
}