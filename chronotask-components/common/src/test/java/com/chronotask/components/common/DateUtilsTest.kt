package com.chronotask.components.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {
    @Test
    fun splitByDay_splitsDurationAtMidnightWithoutLosingSeconds() {
        val dayStart = DateUtils.getDateStart(System.currentTimeMillis())
        val start = dayStart + 23 * 60 * 60 * 1000L + 59 * 60 * 1000L + 30 * 1000L
        val end = DateUtils.getNextDayStart(dayStart) + 60 * 1000L + 30 * 1000L

        assertEquals(
            listOf(dayStart to 30L, DateUtils.getNextDayStart(dayStart) to 90L),
            DateUtils.splitByDay(start, end)
        )
    }

    @Test
    fun splitByBusinessDay_assignsTimeBeforeCustomBoundaryToPreviousBusinessDate() {
        val offsetMinutes = 60
        val today = DateUtils.getDateStart(System.currentTimeMillis())
        val boundary = today + offsetMinutes * 60 * 1000L
        val start = boundary - 30 * 60 * 1000L
        val end = boundary + 30 * 60 * 1000L
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        assertEquals(
            listOf(yesterday to 30 * 60L, today to 30 * 60L),
            DateUtils.splitByBusinessDay(start, end, offsetMinutes)
        )
    }
}
