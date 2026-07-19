package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chronotask.components.database.entity.TaskRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 任务计时记录 DAO
 *
 * 负责 [TaskRecordEntity] 表的增删改查，持久化每次任务计时的时长与日期信息。
 * 支持按任务、日期、日期范围查询记录，并提供原子性的时长累加（UPSERT）操作，
 * 避免并发场景下出现重复记录。
 *
 * 在架构中属于数据访问层（DAO 层），由 Room 实现具体 SQL 操作。
 */
@Dao
interface TaskRecordDao {
    /**
     * 根据任务 ID 和日期获取单条计时记录
     *
     * @param taskId 任务主键 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 匹配的 [TaskRecordEntity]，若不存在则返回 null
     */
    @Query("SELECT * FROM task_records WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getRecordByTaskAndDate(taskId: Long, date: Long): TaskRecordEntity?

    /**
     * 获取指定日期的所有计时记录
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 该日期下的 [TaskRecordEntity] 列表
     */
    @Query("SELECT * FROM task_records WHERE date = :date")
    suspend fun getRecordsByDate(date: Long): List<TaskRecordEntity>

    /**
     * 以 Flow 形式获取指定日期的所有计时记录
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射 [TaskRecordEntity] 列表的 Flow
     */
    @Query("SELECT * FROM task_records WHERE date = :date")
    fun getRecordsByDateFlow(date: Long): Flow<List<TaskRecordEntity>>

    /**
     * 以 Flow 形式获取指定日期的累计计时时长（秒）
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射累计秒数的 Flow，若无记录则发射 null
     */
    @Query("SELECT SUM(durationSeconds) FROM task_records WHERE date = :date")
    fun getTotalDurationByDate(date: Long): Flow<Long?>

    /**
     * 获取某任务的全部计时记录（按日期倒序）
     *
     * @param taskId 任务主键 ID
     * @return 该任务的全部 [TaskRecordEntity] 列表，按 date 倒序
     */
    @Query("SELECT * FROM task_records WHERE taskId = :taskId ORDER BY date DESC")
    suspend fun getRecordsByTask(taskId: Long): List<TaskRecordEntity>

    /**
     * 获取指定日期范围内的所有计时记录
     *
     * @param startDate 范围起始日期（Unix 时间戳，毫秒，包含）
     * @param endDate 范围结束日期（Unix 时间戳，毫秒，包含）
     * @return 范围内的 [TaskRecordEntity] 列表
     */
    @Query("SELECT * FROM task_records WHERE date >= :startDate AND date < :endDate")
    suspend fun getRecordsByDateRange(startDate: Long, endDate: Long): List<TaskRecordEntity>

    /**
     * 根据任务 ID 和日期范围查询计时记录（按 taskId 过滤的范围查询）
     *
     * 该函数直接在 SQL 层面完成过滤，避免在 Kotlin 侧加载全量数据后手动筛选，
     * 适用于需要查询单个任务在某段时间内所有计时明细的场景。
     *
     * @param taskId 任务主键 ID
     * @param startDate 范围起始日期（Unix 时间戳，毫秒，包含）
     * @param endDate 范围结束日期（Unix 时间戳，毫秒，包含）
     * @return 该任务在指定范围内的 [TaskRecordEntity] 列表
     */
    @Query("SELECT * FROM task_records WHERE taskId = :taskId AND date >= :startDate AND date < :endDate")
    suspend fun getRecordsByTaskAndDateRange(taskId: Long, startDate: Long, endDate: Long): List<TaskRecordEntity>

    /**
     * 为指定任务和日期的累计时长增加 deltaSeconds 秒
     *
     * @param taskId 任务主键 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @param deltaSeconds 要累加的秒数
     * @return 受影响的行数，0 表示该任务+日期尚无记录
     */
    @Query("UPDATE task_records SET durationSeconds = durationSeconds + :deltaSeconds WHERE taskId = :taskId AND date = :date")
    suspend fun addDuration(taskId: Long, date: Long, deltaSeconds: Long): Int

    /**
     * 插入一条计时记录
     *
     * 若主键冲突则覆盖已有记录。
     *
     * @param record 要插入的 [TaskRecordEntity] 实例
     * @return 新插入行的 rowId
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TaskRecordEntity): Long

    /**
     * 原子 UPSERT：有则增量更新，无则插入
     *
     * 在事务中执行，防止并发导致重复行。先尝试累加时长，若受影响行数为 0 则插入新记录。
     *
     * @param taskId 任务主键 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @param deltaSeconds 要累加的秒数
     */
    @Transaction
    suspend fun upsertDuration(taskId: Long, date: Long, deltaSeconds: Long) {
        val affected = addDuration(taskId, date, deltaSeconds)
        if (affected == 0) {
            insertRecord(
                TaskRecordEntity(
                    taskId = taskId,
                    date = date,
                    durationSeconds = deltaSeconds
                )
            )
        }
    }

    /**
     * 更新笔记内容
     *
     * @param taskId 任务主键 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @param note 要写入的笔记内容
     * @return 受影响的行数
     */
    @Query("UPDATE task_records SET note = :note WHERE taskId = :taskId AND date = :date")
    suspend fun updateNote(taskId: Long, date: Long, note: String): Int

    /**
     * 原子 UPSERT（含笔记保存）：有则增量更新时长+写笔记，无则插入
     *
     * 在事务中执行，防止并发导致重复行。先尝试累加时长，
     * 若受影响行数为 0 则插入新记录（含笔记），否则更新已有记录的笔记。
     *
     * @param taskId 任务主键 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @param deltaSeconds 要累加的秒数
     * @param note 要写入的笔记内容
     */
    @Transaction
    suspend fun upsertDurationAndNote(taskId: Long, date: Long, deltaSeconds: Long, note: String) {
        val affected = addDuration(taskId, date, deltaSeconds)
        if (affected == 0) {
            insertRecord(
                TaskRecordEntity(
                    taskId = taskId,
                    date = date,
                    durationSeconds = deltaSeconds,
                    note = note
                )
            )
        } else {
            updateNote(taskId, date, note)
        }
    }

    /**
     * 根据主键 ID 更新计时时长
     *
     * @param id 记录主键 ID
     * @param durationSeconds 新的累计秒数
     */
    @Query("UPDATE task_records SET durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateDuration(id: Long, durationSeconds: Long)

    /**
     * 更新一条已存在的计时记录
     *
     * @param record 要更新的 [TaskRecordEntity] 实例（主键必须已设置）
     */
    @Update
    suspend fun updateRecord(record: TaskRecordEntity)

    /**
     * 根据主键 ID 删除一条计时记录
     *
     * @param id 要删除的记录主键 ID
     */
    @Query("DELETE FROM task_records WHERE id = :id")

    suspend fun deleteRecord(id: Long)

    /**
     * 多任务在日期范围内的总计时秒数（用于横向对比聚合）。
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM task_records WHERE taskId IN (:taskIds) AND date >= :startDate AND date < :endDate")
    suspend fun sumDurationBetweenByIds(taskIds: List<Long>, startDate: Long, endDate: Long): Long
}

