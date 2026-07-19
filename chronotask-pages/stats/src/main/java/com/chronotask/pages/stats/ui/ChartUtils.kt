package com.chronotask.pages.stats.ui

import com.chronotask.components.common.FormatUtils

/**
 * ChartUtils - 图表工具函数
 *
 * 核心职责：提供图表辅助函数（时间标签格式化、Y 轴步长计算）。
 * 主要导出：formatTimeLabel, calculateYAxisSteps。
 */

/**
 * 格式化时间标签
 *
 * 将秒数转换为可读的时间字符串（如 "1h30m"），委托 FormatUtils 实现。
 *
 * @param seconds 总秒数
 * @return 格式化后的时间字符串
 */
internal fun formatTimeLabel(seconds: Long): String = FormatUtils.formatTimeLabel(seconds)

/**
 * 计算 Y 轴步长
 *
 * 根据最大值自动选取"漂亮时间步长"，确保刻度标签为单一单位（全分钟/全小时）。
 *
 * 进位规则：
 * - < 30min → 10min 步长
 * - 30min ~ 6h → 1h 步长
 * - 6h ~ 10h → 3h 步长
 * - 10h ~ 20h → 5h 步长
 * - 20h ~ 100h → 20h 步长
 * - 100h ~ 200h → 50h 步长
 * - > 200h → 200h 步长（最高单位）
 *
 * @param maxValue 最大 Y 值（秒）
 * @param period 统计周期
 * @return 从 0 开始的刻度列表
 */
internal fun calculateYAxisSteps(maxValue: Long): List<Long> {
    if (maxValue <= 0L) return listOf(0L, 600L) // 无数据时默认 10 分钟刻度

    // 根据最大值确定步长（秒）
    val step = when {
        maxValue < 30 * 60L -> 10 * 60L           // < 30min → 10min
        maxValue < 6 * 3600L -> 1 * 3600L          // 30min ~ 6h → 1h
        maxValue < 10 * 3600L -> 3 * 3600L         // 6h ~ 10h → 3h
        maxValue < 20 * 3600L -> 5 * 3600L         // 10h ~ 20h → 5h
        maxValue < 100 * 3600L -> 20 * 3600L       // 20h ~ 100h → 20h
        maxValue <= 200 * 3600L -> 50 * 3600L      // 100h ~ 200h → 50h
        else -> 200 * 3600L                        // > 200h → 200h
    }

    // 从 0 开始生成刻度，确保覆盖 maxValue
    val steps = mutableListOf<Long>()
    var current = 0L
    while (current <= maxValue + step) {
        steps.add(current)
        current += step
    }
    return steps
}