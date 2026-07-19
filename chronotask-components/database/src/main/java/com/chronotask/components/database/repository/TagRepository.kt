package com.chronotask.components.database.repository

import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.TagEntity
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.Flow

/**
 * 标签仓库 - 单例模式 (object)
 *
 * 负责 tag 表的读写操作，提供标签的增删改查及全量标签的响应式 Flow 查询。
 * 标签用于对任务进行分类和筛选。
 * 采用单例模式，无状态，仅委托调用 DAO。
 */
object TagRepository {
    private val dao = AppDatabase.getDatabase(appApplication).tagDao()

    /**
     * 全量标签的响应式 Flow
     * @return 发射所有标签列表的 Flow
     */
    val allTags: Flow<List<TagEntity>> = dao.getAllTags()

    /**
     * 插入一条标签
     * @param tag 待插入的标签实体
     * @return 新插入标签的主键 ID
     */
    suspend fun insertTag(tag: TagEntity): Long = dao.insertTag(tag)

    /**
     * 更新标签信息
     * @param tag 待更新的标签实体（需包含主键 ID）
     */
    suspend fun updateTag(tag: TagEntity) = dao.updateTag(tag)

    /**
     * 删除指定标签
     * @param tagId 标签 ID
     */
    suspend fun deleteTag(tagId: Long) = dao.deleteTag(tagId)
}