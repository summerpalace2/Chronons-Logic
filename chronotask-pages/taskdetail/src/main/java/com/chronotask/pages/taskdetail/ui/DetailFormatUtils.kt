package com.chronotask.pages.taskdetail.ui

import com.chronotask.components.common.FormatUtils

/**
 * DetailFormatUtils - 任务详情页专用格式化工具
 *
 *
 */

/**
 * 格式化秒数为可读时长 (委托 FormatUtils)
 */
fun formatDuration(totalSeconds: Long): String = FormatUtils.formatDuration(totalSeconds)

/**
 * 格式化分钟数为可读时长 (委托 FormatUtils)
 */
fun formatDurationMinutes(totalMinutes: Int): String = FormatUtils.formatDurationMinutes(totalMinutes)

/**
 * 格式化秒数为 HH:MM:SS 格式（始终显示时分秒）
 * @param totalSeconds 总秒数
 * @return "01:30:45" 格式字符串
 */
fun formatDurationHHmmss(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
