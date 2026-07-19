package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chronotask.components.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * 任务 DAO
 *
 * 负责 [TaskEntity] 表的增删改查，是计时任务核心数据的唯一数据访问入口。
 * 支持按日期筛选任务、区分活跃/已完成任务，以及流式订阅数据库变更。
 *
 * 在架构中属于数据访问层（DAO 层），由 Room 实现具体 SQL 操作。
 */
@Dao
interface TaskDao {
    /**
     * 以 Flow 形式获取所有任务（按是否完成、排序、创建时间排序）
     *
     * @return 发射全部 [TaskEntity] 列表的 Flow
     */
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, sortOrder ASC, createdDate DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * 以 Flow 形式获取所有未完成任务（活跃任务）
     *
     * @return 发射未完成 [TaskEntity] 列表的 Flow
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY sortOrder ASC, createdDate DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>

    /**
     * 以 Flow 形式获取指定日期的未完成任务
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射该日期未完成 [TaskEntity] 列表的 Flow
     */
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date AND isCompleted = 0 ORDER BY sortOrder ASC, createdDate DESC")
    fun getTasksByDate(date: Long): Flow<List<TaskEntity>>

    /**
     * 以 Flow 形式获取指定日期的全部任务（含已完成）
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射该日期全部 [TaskEntity] 列表的 Flow
     */
    @Query("SELECT * FROM tasks WHERE scheduledDate = :date ORDER BY isCompleted ASC, sortOrder ASC, createdDate DESC")
    fun getAllTasksByDate(date: Long): Flow<List<TaskEntity>>

    /**
     * 根据主键 ID 获取单个任务
     *
     * @param taskId 任务主键 ID
     * @return 匹配的 [TaskEntity]，若不存在则返回 null
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    /**
     * 插入一条任务
     *
     * 若主键冲突则覆盖已有记录。
     *
     * @param task 要插入的 [TaskEntity] 实例
     * @return 新插入行的 rowId
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    /**
     * 更新一条已存在的任务
     *
     * 根据主键匹配并更新字段。
     *
     * @param task 要更新的 [TaskEntity] 实例（主键必须已设置）
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * 根据主键 ID 删除一条任务
     *
     * @param taskId 要删除的任务主键 ID
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("SELECT id FROM tasks WHERE title IN (:titles)")
    suspend fun getIdsByTitles(titles: List<String>): List<Long>

    @Query("SELECT * FROM tasks WHERE title IN (:titles)")
    suspend fun getByTitles(titles: List<String>): List<TaskEntity>
}
