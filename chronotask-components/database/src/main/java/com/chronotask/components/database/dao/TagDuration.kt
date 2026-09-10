package com.chronotask.components.database.dao

/** 数据库聚合后的标签时长行，供统计层转换为 UI 模型。 */
data class TagDuration(
    val tagName: String,
    val totalSeconds: Long
)
