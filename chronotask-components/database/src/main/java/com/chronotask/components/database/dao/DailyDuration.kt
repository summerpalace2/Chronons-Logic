package com.chronotask.components.database.dao

/** 某个自然日的计时汇总，用于批量构建统计图表。 */
data class DailyDuration(
    val date: Long,
    val totalSeconds: Long
)
