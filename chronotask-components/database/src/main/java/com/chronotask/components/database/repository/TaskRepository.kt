package com.chronotask.components.database.repository

import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.Flow

/**
 * 任务仓库 - 单例模式 (object)
 *
 * 负责 tasks 表的读写操作，提供任务的增删改查、按日期筛选、
 * 以及全量/活跃任务的响应式 Flow 查询。
 * 采用单例模式，无状态，仅委托调用 DAO。
 */
object TaskRepository {
    private val dao = AppDatabase.getDatabase(appApplication).taskDao()

    /**
     * 全量任务的响应式 Flow
     * @return 发射所有任务列表的 Flow，按完成状态、排序权重、创建时间排列
     */
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()

    /**
     * 未完成任务的响应式 Flow
     * @return 发射活跃（未完成）任务列表的 Flow，按排序权重、创建时间排列
     */
    val activeTasks: Flow<List<TaskEntity>> = dao.getActiveTasks()

    /**
     * 按 ID 获取单个任务
     * @param taskId 任务 ID
     * @return 对应实体，不存在时返回 null
     */
    suspend fun getById(taskId: Long) = dao.getTaskById(taskId)

    /**
     * 插入一条任务
     * @param task 待插入的任务实体
     * @return 新插入任务的主键 ID
     */
    suspend fun insert(task: TaskEntity) = dao.insertTask(task)

    /**
     * 更新任务信息
     * @param task 待更新的任务实体（需包含主键 ID）
     */
    suspend fun update(task: TaskEntity) = dao.updateTask(task)

    /**
     * 删除指定任务
     * @param taskId 任务 ID
     */
    suspend fun delete(taskId: Long) = dao.deleteTask(taskId)

    /**
     * 按日期获取当天安排的任务列表（响应式 Flow）
     * @param date 当日零点的毫秒时间戳
     * @return 发射当天任务列表的 Flow
     */
    fun getTasksByDate(date: Long) = dao.getTasksByDate(date)
}