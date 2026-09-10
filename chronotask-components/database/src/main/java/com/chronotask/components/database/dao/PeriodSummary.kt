package com.chronotask.components.database.dao

/** 指定时间范围内的计时汇总，避免为摘要统计加载原始记录。 */
data class PeriodSummary(
    val totalSeconds: Long,
    val workDays: Int
)
