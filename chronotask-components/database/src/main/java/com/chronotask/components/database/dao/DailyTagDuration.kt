package com.chronotask.components.database.dao

/** 某个自然日、某个标签的计时汇总，用于批量构建统计图表。 */
data class DailyTagDuration(
    val date: Long,
    val tagName: String,
    val totalSeconds: Long
)
