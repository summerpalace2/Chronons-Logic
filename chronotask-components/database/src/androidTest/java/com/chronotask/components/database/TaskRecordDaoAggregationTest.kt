package com.chronotask.components.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.entity.TaskRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskRecordDaoAggregationTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aggregations_returnOnlyRecordsInsideRange() = runBlocking {
        val taskId = database.taskDao().insertTask(TaskEntity(title = "Statistics fixture"))
        database.taskRecordDao().insertRecord(TaskRecordEntity(taskId = taskId, date = 100, durationSeconds = 30))
        database.taskRecordDao().insertRecord(TaskRecordEntity(taskId = taskId, date = 200, durationSeconds = 40))
        database.taskRecordDao().insertRecord(TaskRecordEntity(taskId = taskId, date = 300, durationSeconds = 50))

        val summary = database.taskRecordDao().getPeriodSummary(startDate = 100, endDate = 300)

        assertEquals(70L, summary.totalSeconds)
        assertEquals(2, summary.workDays)

        assertEquals(
            listOf(100L to 30L, 200L to 40L),
            database.taskRecordDao().getDailyDurations(startDate = 100, endDate = 300)
                .map { it.date to it.totalSeconds }
        )
        assertEquals(
            listOf(
                Triple(100L, "Uncategorized", 30L),
                Triple(200L, "Uncategorized", 40L)
            ),
            database.taskRecordDao().getDailyTagDurations(
                startDate = 100,
                endDate = 300,
                uncategorizedName = "Uncategorized"
            ).map { Triple(it.date, it.tagName, it.totalSeconds) }
        )
    }
}
