package com.chronotask.pages.home.data

import com.chronotask.components.database.entity.TaskEntity

/**
 * TaskItemState - 任务条目展示状态（不可变数据类，在 UI 层直接渲染）
 *
 * @param task                原始任务实体
 * @param tagName             关联标签名称（无标签时为空串）
 * @param todayDurationSeconds 今日累计秒数（已记录 + 正在进行）
 * @param isRunning           当前是否正在计时
 * @param isOverTarget        是否已超出目标时长
 */
data class TaskItemState(
    val task: TaskEntity,
    val tagName: String = "",
    val todayDurationSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val isOverTarget: Boolean = false
)
