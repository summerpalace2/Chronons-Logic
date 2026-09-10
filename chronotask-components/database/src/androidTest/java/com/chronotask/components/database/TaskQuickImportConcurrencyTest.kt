package com.chronotask.components.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chronotask.components.database.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskQuickImportConcurrencyTest {
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
    fun concurrentQuickImports_insertOnlyOneTaskForTheSameBusinessDay() = runBlocking {
        val task = TaskEntity(
            title = "Daily planning",
            scheduledDate = 100L,
            quickImportKey = "Daily planning"
        )

        val insertedIds = coroutineScope {
            List(2) {
                async(Dispatchers.Default) {
                    database.taskDao().insertQuickImportTask(task)
                }
            }.awaitAll()
        }

        assertEquals(1, insertedIds.count { it != -1L })
        assertEquals(1, database.taskDao().getByTitles(listOf("Daily planning")).size)
    }

    @Test
    fun ordinaryTasks_canStillUseTheSameTitle() = runBlocking {
        val task = TaskEntity(title = "Read", scheduledDate = 100L)

        database.taskDao().insertTask(task)
        database.taskDao().insertTask(task)

        assertEquals(2, database.taskDao().getByTitles(listOf("Read")).size)
    }
}
