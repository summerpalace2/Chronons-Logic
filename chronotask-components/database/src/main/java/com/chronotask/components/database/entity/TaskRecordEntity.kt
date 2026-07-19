package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TaskRecordEntity - 任务计时记录实体
 *
 * 核心职责：存储任务每日的累计计时数据。
 * 每条记录对应一个任务在某一天的计时汇总（总时长 + 备注）。
 * 当任务删除时，关联记录自动级联删除（CASCADE）。
 */
@Entity(
    tableName = "task_records",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId", "date"])]
)
data class TaskRecordEntity(
    @PrimaryKey(autoGenerate = true)

    /** 主键ID，自增 */
    val id: Long = 0,

    /** 关联任务ID，任务删除时级联删除本记录 */
    val taskId: Long,

    /** 记录日期（当天零点时间戳） */
    val date: Long,

    /** 当日累计计时时长（秒） */
    val durationSeconds: Long = 0,

    /** 当日计时备注 */
    val note: String = ""
)