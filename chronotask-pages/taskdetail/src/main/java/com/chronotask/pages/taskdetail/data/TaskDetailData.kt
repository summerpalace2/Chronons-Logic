package com.chronotask.pages.taskdetail.data

/**
 * HistorySection - 按日期分组的笔记历史
 *
 * @param dateLabel 分组标题（如 "今天"、"昨天"、"7月15日"）
 * @param items     该分组下的笔记列表
 */
data class HistorySection(
    val dateLabel: String,
    val items: List<com.chronotask.components.database.entity.NoteHistoryEntity>
)

/**
 * TaskComparisonData - 横向对比数据
 *
 * @param weekAvgSeconds  本周平均每日秒数
 * @param monthAvgSeconds 本月平均每日秒数
 */
data class TaskComparisonData(
    val weekAvgSeconds: Long = 0,
    val monthAvgSeconds: Long = 0
)
