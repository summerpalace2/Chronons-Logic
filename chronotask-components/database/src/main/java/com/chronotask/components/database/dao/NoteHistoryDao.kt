package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chronotask.components.database.entity.NoteHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 笔记历史 DAO
 *
 * 负责 [NoteHistoryEntity] 表的增删改查，用于管理用户在任务过程中记录的笔记历史。
 * 每条笔记关联一个任务（taskId）和日期（date），支持按任务、日期、标题、内容等维度检索。
 *
 * 在架构中属于数据访问层（DAO 层），由 Room 实现具体 SQL 操作。
 */
@Dao
interface NoteHistoryDao {

    /**
     * 插入一条笔记记录
     *
     * 若主键冲突则覆盖已有记录。
     *
     * @param entity 要插入的 [NoteHistoryEntity] 实例
     * @return 新插入行的 rowId
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entity: NoteHistoryEntity): Long

    /**
     * 以 Flow 形式获取某任务下的所有笔记（按会话开始时间倒序）
     *
     * @param taskId 目标任务 ID
     * @return 发射 [NoteHistoryEntity] 列表的 Flow
     */
    @Query("SELECT * FROM note_history WHERE taskId = :taskId ORDER BY sessionStartTime DESC")
    fun getByTaskFlow(taskId: Long): Flow<List<NoteHistoryEntity>>

    /**
     * 以 Flow 形式获取某任务在指定日期的笔记（按会话开始时间倒序）
     *
     * @param taskId 目标任务 ID
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 发射 [NoteHistoryEntity] 列表的 Flow
     */
    @Query("SELECT * FROM note_history WHERE taskId = :taskId AND date = :date ORDER BY sessionStartTime DESC")
    fun getByTaskAndDateFlow(taskId: Long, date: Long): Flow<List<NoteHistoryEntity>>

    /**
     * 更新指定笔记的内容字段
     *
     * @param id 笔记主键 ID
     * @param note 新的笔记内容
     * @return 受影响的行数
     */
    @Query("UPDATE note_history SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String): Int

    /**
     * 更新指定笔记的标题字段
     *
     * @param id 笔记主键 ID
     * @param title 新的标题
     * @return 受影响的行数
     */
    @Query("UPDATE note_history SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String): Int

    /**
     * 同时更新指定笔记的内容和标题
     *
     * @param id 笔记主键 ID
     * @param note 新的笔记内容
     * @param title 新的标题
     * @return 受影响的行数
     */
    @Query("UPDATE note_history SET note = :note, title = :title WHERE id = :id")
    suspend fun updateNoteAndTitle(id: Long, note: String, title: String): Int

    /**
     * 同时更新指定笔记的标题和来源任务标题
     *
     * @param id 笔记主键 ID
     * @param title 新的标题
     * @param sourceTaskTitle 新的来源任务标题
     * @return 受影响的行数
     */
    @Query("UPDATE note_history SET title = :title, sourceTaskTitle = :sourceTaskTitle WHERE id = :id")
    suspend fun updateTitleAndSource(id: Long, title: String, sourceTaskTitle: String): Int

    /**
     * 根据主键 ID 删除一条笔记
     *
     * @param id 要删除的笔记主键 ID
     */
    @Query("DELETE FROM note_history WHERE id = :id")
    suspend fun deleteNote(id: Long)

    /**
     * 根据主键 ID 获取单条笔记
     *
     * @param id 笔记主键 ID
     * @return 匹配的 [NoteHistoryEntity]，若不存在则返回 null
     */
    @Query("SELECT * FROM note_history WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteHistoryEntity?

    /**
     * 在当前任务下按内容模糊搜索笔记
     *
     * @param taskId 目标任务 ID
     * @param query 搜索关键词
     * @return 匹配的 [NoteHistoryEntity] 列表，按 sessionStartTime 倒序
     */
    @Query("SELECT * FROM note_history WHERE taskId = :taskId AND note != '' AND note LIKE '%' || :query || '%' ORDER BY sessionStartTime DESC")
    suspend fun searchNotes(taskId: Long, query: String): List<NoteHistoryEntity>

    /**
     * 以 Flow 形式获取所有笔记（按会话开始时间倒序）
     *
     * @return 发射全部 [NoteHistoryEntity] 的 Flow
     */
    @Query("SELECT * FROM note_history ORDER BY sessionStartTime DESC")
    fun getAllNotesFlow(): Flow<List<NoteHistoryEntity>>

    /**
     * 跨所有任务按内容模糊搜索笔记
     *
     * @param query 搜索关键词
     * @return 匹配的 [NoteHistoryEntity] 列表，按 sessionStartTime 倒序
     */
    @Query("SELECT * FROM note_history WHERE note LIKE '%' || :query || '%' ORDER BY sessionStartTime DESC")
    suspend fun searchAllNotes(query: String): List<NoteHistoryEntity>

    /**
     * 删除指定任务下的所有笔记
     *
     * @param taskId 目标任务 ID
     */
    @Query("DELETE FROM note_history WHERE taskId = :taskId")
    suspend fun deleteByTask(taskId: Long)

    /**
     * 按标题模糊查询 (笔记页搜索)
     *
     * 当 query 为空字符串时，返回 title 为空（未命名）的笔记；
     * 否则按标题模糊匹配。
     *
     * @param query 搜索关键词，空字符串返回未命名笔记
     * @return 匹配的 [NoteHistoryEntity] 列表，按 sessionStartTime 倒序
     */
    @Query("SELECT * FROM note_history WHERE CASE WHEN :query = '' THEN title = '' ELSE title LIKE '%' || :query || '%' END ORDER BY sessionStartTime DESC")
    suspend fun searchByTitle(query: String): List<NoteHistoryEntity>

    /**
     * 按日期查询笔记 (日历筛选)
     *
     * @param date 目标日期（Unix 时间戳，毫秒）
     * @return 该日期下的 [NoteHistoryEntity] 列表，按 sessionStartTime 倒序
     */
    @Query("SELECT * FROM note_history WHERE date = :date ORDER BY sessionStartTime DESC")
    suspend fun getByDate(date: Long): List<NoteHistoryEntity>
}