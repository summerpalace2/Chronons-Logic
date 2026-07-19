package com.chronotask.components.database.repository

import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.Flow

/**
 * 笔记历史仓库 - 单例模式 (object)
 *
 * 负责 note_history 表的读写操作，提供笔记的增删改查、按任务/日期/标题搜索，
 * 以及响应式 Flow 查询。笔记用于记录每次计时过程中的文本内容。
 * 采用单例模式，无状态，仅委托调用 DAO。
 */
object NoteHistoryRepository {
    private val dao = AppDatabase.getDatabase(appApplication).noteHistoryDao()

    /**
     * 插入一条笔记记录
     * @param entity 待插入的笔记实体
     * @return 新插入记录的主键 ID
     */
    suspend fun insertNote(entity: NoteHistoryEntity): Long = dao.insertNote(entity)

    /**
     * 按任务 ID 获取所有笔记（响应式 Flow）
     * @param taskId 任务 ID
     * @return 发射该任务下所有笔记列表的 Flow
     */
    fun getByTask(taskId: Long): Flow<List<NoteHistoryEntity>> = dao.getByTaskFlow(taskId)

    /**
     * 按任务 ID 和日期获取笔记（响应式 Flow）
     * @param taskId 任务 ID
     * @param date   当日零点的毫秒时间戳
     * @return 发射符合条件笔记列表的 Flow
     */
    fun getByTaskAndDate(taskId: Long, date: Long): Flow<List<NoteHistoryEntity>> =
        dao.getByTaskAndDateFlow(taskId, date)

    /**
     * 更新笔记内容
     * @param id   笔记记录 ID
     * @param note 新的笔记文本
     */
    suspend fun updateNote(id: Long, note: String) = dao.updateNote(id, note)

    /**
     * 更新笔记标题
     * @param id    笔记记录 ID
     * @param title 新的标题
     */
    suspend fun updateTitle(id: Long, title: String) = dao.updateTitle(id, title)

    /**
     * 同时更新笔记内容和标题
     * @param id   笔记记录 ID
     * @param note 新的笔记文本
     * @param title 新的标题
     */
    suspend fun updateNoteAndTitle(id: Long, note: String, title: String) = dao.updateNoteAndTitle(id, note, title)

    /**
     * 删除指定笔记
     * @param id 笔记记录 ID
     */
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    /**
     * 按 ID 获取单条笔记
     * @param id 笔记记录 ID
     * @return 对应实体，不存在时返回 null
     */
    suspend fun getNoteById(id: Long): NoteHistoryEntity? = dao.getNoteById(id)

    /**
     * 按任务 ID 模糊搜索笔记
     * @param taskId 任务 ID
     * @param query  搜索关键词
     * @return 匹配的笔记列表
     */
    suspend fun searchNotes(taskId: Long, query: String): List<NoteHistoryEntity> =
        dao.searchNotes(taskId, query)

    /**
     * 删除指定任务下的所有笔记
     * @param taskId 任务 ID
     */
    suspend fun deleteByTask(taskId: Long) = dao.deleteByTask(taskId)

    /**
     * 获取所有笔记（响应式 Flow）
     * @return 发射全部笔记列表的 Flow
     */
    fun getAllNotesFlow(): Flow<List<NoteHistoryEntity>> = dao.getAllNotesFlow()

    /**
     * 全局模糊搜索笔记（不限任务）
     * @param query 搜索关键词
     * @return 匹配的笔记列表
     */
    suspend fun searchAllNotes(query: String): List<NoteHistoryEntity> = dao.searchAllNotes(query)

    /**
     * 同时更新笔记标题与来源任务标题
     * @param id               笔记记录 ID
     * @param title            新的笔记标题
     * @param sourceTaskTitle  来源任务标题
     */
    suspend fun updateTitleAndSource(id: Long, title: String, sourceTaskTitle: String) = dao.updateTitleAndSource(id, title, sourceTaskTitle)

    /**
     * 按标题模糊查询 (笔记页搜索)
     * 空字符串时返回 title 为空 (未命名) 的笔记
     * @param query 搜索关键词
     * @return 匹配的笔记列表
     */
    suspend fun searchByTitle(query: String): List<NoteHistoryEntity> = dao.searchByTitle(query)

    /**
     * 按日期查询笔记 (日历筛选)
     * @param date 当日零点的毫秒时间戳
     * @return 该日期下的笔记列表
     */
    suspend fun getByDate(date: Long): List<NoteHistoryEntity> = dao.getByDate(date)
}