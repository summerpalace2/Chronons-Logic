package com.chronotask.components.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TagEntity - 标签实体
 *
 * 核心职责：存储任务标签信息，用于任务分类与筛选。
 * 每个标签可包含名称、图标以及排序序号，供 UI 展示时使用。
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)

    /** 主键ID，自增 */
    val id: Long = 0,

    /** 标签名称 */
    val name: String,

    /** 标签图标（存储图标标识或资源名） */
    val icon: String = "",

    /** 排序序号，数值越小越靠前 */
    val sortOrder: Int = 0
)