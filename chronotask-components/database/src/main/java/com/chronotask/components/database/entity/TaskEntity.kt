package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TaskEntity - 任务实体
 *
 * 核心职责：存储用户创建的任务信息，包括标题、关联标签、目标时长、完成状态等。
 * 任务可关联一个标签（tagId），标签删除时自动置空（SET_NULL）。
 * 任务可指定计划日期（scheduledDate），用于日程管理。
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["tagId"]), Index(value = ["scheduledDate"])]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)

    /** 主键ID，自增 */
    val id: Long = 0,

    /** 任务标题 */
    val title: String,

    /** 关联标签ID，标签删除时自动置空 */
    val tagId: Long? = null,

    /** 目标时长（分钟），用于番茄钟计时目标 */
    val targetDurationMinutes: Int? = null,

    /** 是否已完成 */
    val isCompleted: Boolean = false,

    /** 任务创建时间戳 */
    val createdDate: Long = System.currentTimeMillis(),

    /** 计划日期（当天零点时间戳），0 表示未指定 */
    val scheduledDate: Long = 0L,

    /** 排序序号，数值越小越靠前 */
    val sortOrder: Int = 0
)