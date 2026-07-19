package com.chronotask.pages.home.ui

import com.chronotask.components.common.FormatUtils

/**
 * 首页格式化工具
 *
 * 职责：提供首页计时/时长格式化函数，委托给通用 FormatUtils。
 */

/**
 * 格式化时长显示（例如：1h 30m / 45s）
 *
 * @param totalSeconds 总秒数
 * @return 格式化后的时长字符串
 */
fun formatDuration(totalSeconds: Long): String = FormatUtils.formatDuration(totalSeconds)

/**
 * 格式化分钟数时长（例如：90m / 2h）
 *
 * @param totalMinutes 总分钟数
 * @return 格式化后的时长字符串
 */
fun formatDurationMinutes(totalMinutes: Int): String = FormatUtils.formatDurationMinutes(totalMinutes)
