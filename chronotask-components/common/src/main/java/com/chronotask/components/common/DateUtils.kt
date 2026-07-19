package com.chronotask.components.common

import java.util.Calendar

object DateUtils {

    /**
     * 获取今天的零点时间戳
     */
    fun getTodayStart(): Long = getDateStart(System.currentTimeMillis())

    /**
     * 获取指定时间当天的零点时间戳
     */
    fun getDateStart(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 获取本周一的零点时间戳
     *
     * @param timeMillis 参考时间，默认为当前时间
     */
    fun getWeekStart(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // 如果计算出的周一在未来，则回退一周
        if (cal.timeInMillis > timeMillis) {
            cal.add(Calendar.WEEK_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    /**
     * 获取本月1日的零点时间戳
     *
     * @param timeMillis 参考时间，默认为当前时间
     */
    fun getMonthStart(timeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 获取指定时间所在周的所有日期的零点时间戳列表（周一到周日）
     *
     * @param timeMillis 参考时间，默认为当前时间
     * @return 7个元素的列表，分别是周一到周日的零点时间戳
     */
    fun getWeekDays(timeMillis: Long = System.currentTimeMillis()): List<Long> {
        val weekStart = getWeekStart(timeMillis)
        return (0..6).map { i ->
            weekStart + i * (24 * 60 * 60 * 1000L)
        }
    }

    fun getNextDayStart(dayStart: Long): Long = dayStart + 24L * 60 * 60 * 1000

    /**
     * 按用户自定义的"一天开始偏移"计算时间戳所属的日槽零点。
     */
    fun getStartOfDay(timeMillis: Long, offsetMinutes: Int): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        cal.add(Calendar.MINUTE, -offsetMinutes)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val baseDay = cal.timeInMillis
        cal.add(Calendar.MINUTE, offsetMinutes)
        return cal.timeInMillis
    }

    /**
     * 返回当前时间所在的"可计时日"的零时戳（基于 offset 判断当日归属）。
     * 例：offset=60 (1:00)
     *   - 当前 7/17 1:24 -> 7/17 0:00
     *   - 当前 7/17 0:30 -> 7/16 0:00
     */
    fun getActiveDayMidnight(timeMillis: Long, offsetMinutes: Int): Long {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (currentMinutes < offsetMinutes) cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun splitByDay(startMs: Long, endMs: Long): List<Pair<Long, Long>> {
        require(endMs >= startMs) { "endMs must >= startMs" }
        val result = mutableListOf<Pair<Long, Long>>()
        var dayStart = getDateStart(startMs)
        while (dayStart < endMs) {
            val next = getNextDayStart(dayStart)
            val segEnd = if (next < endMs) next else endMs
            val segStart = if (dayStart < startMs) startMs else dayStart
            val seconds = (segEnd - segStart) / 1000L
            if (seconds > 0) result.add(dayStart to seconds)
            dayStart = next
        }
        return result
    }
}
