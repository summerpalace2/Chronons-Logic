package com.chronotask.components.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate6To7_keepsExistingRecordsAndCreatesFocusSessionsTable() {
        migrationHelper.createDatabase(TEST_DATABASE, 6).apply {
            execSQL(
                "INSERT INTO tasks " +
                    "(id, title, tagId, targetDurationMinutes, isCompleted, createdDate, scheduledDate, sortOrder) " +
                    "VALUES (1, 'Resume preparation', NULL, NULL, 0, 100, 100, 0)"
            )
            execSQL(
                "INSERT INTO task_records (id, taskId, date, durationSeconds, note) " +
                    "VALUES (1, 1, 100, 1500, 'migration fixture')"
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            AppDatabase.MIGRATION_6_7
        )
        try {
            assertEquals(1, migratedDatabase.count("tasks"))
            assertEquals(1, migratedDatabase.count("task_records"))
            assertTrue(migratedDatabase.hasTable("focus_sessions"))
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migrate7To8_createsDateIndex() {
        migrationHelper.createDatabase(TEST_DATABASE, 7).close()

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            AppDatabase.MIGRATION_7_8
        )
        try {
            assertTrue(migratedDatabase.hasIndex("task_records", "index_task_records_date"))
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migrate8To9_normalizesLegacyBusinessDayKeys() {
        val legacyBoundary = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JANUARY, 12, 1, 0, 0)
        }.timeInMillis
        val expectedDate = Calendar.getInstance().apply {
            timeInMillis = legacyBoundary
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        migrationHelper.createDatabase(TEST_DATABASE, 8).apply {
            execSQL(
                "INSERT INTO tasks " +
                    "(id, title, tagId, targetDurationMinutes, isCompleted, createdDate, scheduledDate, sortOrder) " +
                    "VALUES (1, 'Legacy task', NULL, NULL, 0, 100, 100, 0)"
            )
            execSQL(
                "INSERT INTO task_records (id, taskId, date, durationSeconds, note) " +
                    "VALUES (1, 1, $legacyBoundary, 900, '')"
            )
            execSQL(
                "INSERT INTO focus_sessions (id, taskId, date, sessionStartTime, sessionEndTime, durationSeconds) " +
                    "VALUES (1, 1, $legacyBoundary, $legacyBoundary, ${legacyBoundary + 900_000}, 900)"
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            AppDatabase.MIGRATION_8_9
        )
        try {
            assertEquals(expectedDate, migratedDatabase.valueForId("task_records", 1))
            assertEquals(expectedDate, migratedDatabase.valueForId("focus_sessions", 1))
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migrate9To10_addsQuickImportIdempotencyIndex() {
        migrationHelper.createDatabase(TEST_DATABASE, 9).apply {
            execSQL(
                "INSERT INTO tasks " +
                    "(id, title, tagId, targetDurationMinutes, isCompleted, createdDate, scheduledDate, sortOrder) " +
                    "VALUES (1, 'Existing task', NULL, NULL, 0, 100, 100, 0)"
            )
            close()
        }

        val migratedDatabase = migrationHelper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            AppDatabase.MIGRATION_9_10
        )
        try {
            assertEquals(1, migratedDatabase.count("tasks"))
            assertTrue(
                migratedDatabase.hasIndex(
                    "tasks",
                    "index_tasks_scheduledDate_quickImportKey"
                )
            )
        } finally {
            migratedDatabase.close()
        }
    }

    private fun SupportSQLiteDatabase.count(tableName: String): Int =
        query("SELECT COUNT(*) FROM $tableName").use { cursor ->
            check(cursor.moveToFirst())
            cursor.getInt(0)
        }

    private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean =
        query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            cursor.moveToFirst()
        }

    private fun SupportSQLiteDatabase.hasIndex(tableName: String, indexName: String): Boolean =
        query("PRAGMA index_list($tableName)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            generateSequence { if (cursor.moveToNext()) cursor.getString(nameColumn) else null }
                .any { it == indexName }
        }

    private fun SupportSQLiteDatabase.valueForId(tableName: String, id: Long): Long =
        query("SELECT date FROM $tableName WHERE id = ?", arrayOf(id)).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DATABASE = "app_database_migration_test"
    }
}
