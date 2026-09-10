package com.chronotask.pages.stats.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StatsPeriodRangeCalculatorTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun previousMonth_usesPreviousNaturalMonthWhenLengthsDiffer() {
        val current = StatsPeriodRangeCalculator.current(StatsPeriod.MONTH, utcMillis(2026, 3, 15), utc)
        val previous = StatsPeriodRangeCalculator.previous(StatsPeriod.MONTH, current.start, utc)

        assertEquals(utcMillis(2026, 3, 1), current.start)
        assertEquals(utcMillis(2026, 4, 1), current.end)
        assertEquals(utcMillis(2026, 2, 1), previous.start)
        assertEquals(current.start, previous.end)
    }

    @Test
    fun previousYear_usesPreviousNaturalYearAcrossLeapYear() {
        val current = StatsPeriodRangeCalculator.current(StatsPeriod.YEAR, utcMillis(2024, 6, 1), utc)
        val previous = StatsPeriodRangeCalculator.previous(StatsPeriod.YEAR, current.start, utc)

        assertEquals(utcMillis(2024, 1, 1), current.start)
        assertEquals(utcMillis(2025, 1, 1), current.end)
        assertEquals(utcMillis(2023, 1, 1), previous.start)
        assertEquals(current.start, previous.end)
    }

    @Test
    fun currentWeek_startsOnMondayAndPreviousWeekEndsAtCurrentStart() {
        val current = StatsPeriodRangeCalculator.current(StatsPeriod.WEEK, utcMillis(2026, 3, 15), utc)
        val previous = StatsPeriodRangeCalculator.previous(StatsPeriod.WEEK, current.start, utc)

        assertEquals(utcMillis(2026, 3, 9), current.start)
        assertEquals(utcMillis(2026, 3, 16), current.end)
        assertEquals(utcMillis(2026, 3, 2), previous.start)
        assertEquals(current.start, previous.end)
    }

    private fun utcMillis(year: Int, month: Int, dayOfMonth: Int): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, dayOfMonth, 0, 0, 0)
        }.timeInMillis
}
