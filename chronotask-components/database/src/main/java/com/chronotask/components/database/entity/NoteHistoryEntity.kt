package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * NoteHistoryEntity - 计时笔记历史实体
 *
 * 核心职责：存储每次番茄钟计时的笔记记录（TickTick 模式：每次计时 = 一条笔记）。
 * 主要字段：taskId + date + sessionStartTime 唯一标识一次计时会话。
 *
 * 设计说明：
 *   - taskId = -1 表示全局笔记（不关联任何任务，独立存在于笔记Tab）
 *   - taskId > 0 表示任务关联笔记（taskdetail 页面中展示的笔记）
 *   - 不使用外键约束，因为 taskId=-1 的合法全局笔记会违反 FK 约束
 *   - 级联删除由 Repository.deleteByTask() 手动处理
 */
@Entity(
    tableName = "note_history",
    indices = [
        Index(value = ["taskId", "date", "sessionStartTime"], unique = true),
        Index(value = ["taskId"])
    ]
)
data class NoteHistoryEntity(
    @PrimaryKey(autoGenerate = true)

    /** 主键ID，自增 */
    val id: Long = 0,

    /** 关联任务ID：-1=全局笔记，>0=任务关联笔记 */
    val taskId: Long,

    /** 当天零点时间戳 */
    val date: Long,

    /** 本次计时开始时间（精确到毫秒） */
    val sessionStartTime: Long,

    /** 本次计时时长（秒） */
    val durationSeconds: Long,

    /** 本次计时的笔记内容 */
    val note: String = "",

    /** 笔记标题（首次保存时从首行提取） */
    val title: String = "",

    /** 任务来源标题：任务笔记保存时填入关联任务标题，全局笔记 Tab 保存时为空 */
    val sourceTaskTitle: String = ""
)