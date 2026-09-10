package com.chronotask.components.database

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chronotask.components.database.entity.FocusSessionEntity
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.entity.TaskRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDeletionDataIntegrityTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingStoppedTask_removesRecordsAndFocusSessionsTogether() = runBlocking {
        val taskId = database.taskDao().insertTask(TaskEntity(title = "Task to delete"))
        database.taskRecordDao().insertRecord(
            TaskRecordEntity(taskId = taskId, date = 100, durationSeconds = 120)
        )
        database.focusSessionDao().insert(
            FocusSessionEntity(
                taskId = taskId,
                date = 100,
                sessionStartTime = 100,
                sessionEndTime = 5_600_100,
                durationSeconds = 5_600
            )
        )

        database.withTransaction {
            database.focusSessionDao().deleteByTaskId(taskId)
            database.taskDao().deleteTask(taskId)
        }

        assertEquals(null, database.taskDao().getTaskById(taskId))
        assertEquals(null, database.taskRecordDao().getRecordByTaskAndDate(taskId, 100))
        assertEquals(
            0,
            database.focusSessionDao().countQualifiedByDateRange(
                startDate = 0,
                endDate = 200,
                thresholdSeconds = 0
            )
        )
    }
}
