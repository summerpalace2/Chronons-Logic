package com.chronotask.components.common

/**
 * 时长格式化工具类
 *
 * 用法：
 * ```kotlin
 * FormatUtils.formatDuration(3661)      // "1h 1m"
 * FormatUtils.formatDurationMinutes(90) // "1h 30m"
 * FormatUtils.formatTimeLabel(3661)     // "1h1m"
 * ```
 */
object FormatUtils {

    /**
     * 将秒数格式化为可读时长字符串（自动处理千小时单位）
     * 例：3661 → "1h 1m"，360000 → "100h 0m"，3600000 → "1kh"
     */
    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours >= 1000 -> {
                val kh = hours / 1000f
                val khStr = if (kh == kh.toLong().toFloat()) "${kh.toLong()}kh" else String.format("%.1fkh", kh)
                if (minutes > 0) "$khStr ${minutes}m" else khStr
            }
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    /**
     * 将分钟数格式化为可读时长字符串
     */
    fun formatDurationMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours >= 1000 -> {
                val kh = hours / 1000f
                if (kh == kh.toLong().toFloat()) "${kh.toLong()}kh" else String.format("%.1fkh", kh)
            }
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    /**
     * 格式化时间为图表标签（紧凑格式）
     * 例：3661 → "1h1m"，360000 → "100h"，3600000 → "1kh"
     */
    fun formatTimeLabel(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours >= 1000 -> {
                val kh = hours / 1000f
                if (kh == kh.toLong().toFloat()) "${kh.toLong()}kh" else String.format("%.1fkh", kh)
            }
            hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "0"
        }
    }

    /**
     * 计算进度百分比（0f..1f）
     *
     * @param currentSeconds 当前用时（秒）
     * @param targetMinutes 目标时长（分钟），null 表示无限制
     * @return 进度值 0f..1f，无限制时返回 null
     */
    fun calculateProgress(currentSeconds: Long, targetMinutes: Int?): Float? {
        return targetMinutes?.let { tm ->
            if (tm > 0) (currentSeconds.toFloat() / (tm * 60f)).coerceIn(0f, 1f) else null
        }
    }

    /**
     * 判断是否已超过目标时长
     *
     * @param currentSeconds 当前用时（秒）
     * @param targetMinutes 目标时长（分钟），null 表示无限制
     */
    fun isExceeded(currentSeconds: Long, targetMinutes: Int?): Boolean {
        return targetMinutes != null && currentSeconds > (targetMinutes * 60L)
    }
}
