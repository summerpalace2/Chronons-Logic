package com.chronotask.components.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.chronotask.components.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * 标签 DAO
 *
 * 负责 [TagEntity] 表的增删改查，用于管理任务标签的持久化存储。
 * 标签用于对任务进行分类与筛选，支持排序字段（sortOrder）控制展示顺序。
 *
 * 在架构中属于数据访问层（DAO 层），由 Room 实现具体 SQL 操作。
 */
@Dao
interface TagDao {
    /**
     * 以 Flow 形式获取所有标签（按 sortOrder 升序、name 升序）
     *
     * 数据库变更时自动发射最新列表，适用于 UI 实时订阅。
     *
     * @return 发射 [TagEntity] 列表的 Flow
     */
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    /**
     * 同步获取所有标签（按 sortOrder 升序、name 升序）
     *
     * 挂起函数，直接返回列表，适用于一次性读取场景。
     *
     * @return 全部 [TagEntity] 列表
     */
    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllTagsSync(): List<TagEntity>

    /**
     * 插入一条标签
     *
     * 若主键冲突则覆盖已有记录，适用于标签改名或排序调整后的保存。
     *
     * @param tag 要插入的 [TagEntity] 实例
     * @return 新插入行的 rowId
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    /**
     * 更新一条已存在的标签
     *
     * 根据主键匹配并更新除主键外的所有字段。
     *
     * @param tag 要更新的 [TagEntity] 实例（主键必须已设置）
     */
    @Update
    suspend fun updateTag(tag: TagEntity)

    /**
     * 根据主键 ID 删除一条标签
     *
     * @param tagId 要删除的标签主键 ID
     */
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)
}