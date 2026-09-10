package com.chronotask.pages.stats.data

import java.util.Calendar
import java.util.TimeZone

/** 统计周期的本地自然日范围，所有范围均为 [start, end) 。 */
data class StatsDateRange(
    val start: Long,
    val end: Long
)

/** 统一计算当前与上一自然统计周期，避免用毫秒长度倒推月/年边界。 */
object StatsPeriodRangeCalculator {
    fun current(
        period: StatsPeriod,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): StatsDateRange {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = when (period) {
            StatsPeriod.WEEK -> {
                val offset = if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6
                else calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -offset)
                calendar.timeInMillis
            }

            StatsPeriod.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }

            StatsPeriod.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.timeInMillis
            }
        }
        val end = Calendar.getInstance(timeZone).apply {
            timeInMillis = start
            when (period) {
                StatsPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, 1)
                StatsPeriod.MONTH -> add(Calendar.MONTH, 1)
                StatsPeriod.YEAR -> add(Calendar.YEAR, 1)
            }
        }.timeInMillis
        return StatsDateRange(start, end)
    }

    fun previous(
        period: StatsPeriod,
        currentStart: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): StatsDateRange {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentStart
            when (period) {
                StatsPeriod.WEEK -> add(Calendar.WEEK_OF_YEAR, -1)
                StatsPeriod.MONTH -> add(Calendar.MONTH, -1)
                StatsPeriod.YEAR -> add(Calendar.YEAR, -1)
            }
        }
        return StatsDateRange(start = calendar.timeInMillis, end = currentStart)
    }
}
