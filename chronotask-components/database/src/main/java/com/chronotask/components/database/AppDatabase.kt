package com.chronotask.components.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chronotask.components.database.dao.DailyRestDao
import com.chronotask.components.database.dao.FocusSessionDao
import com.chronotask.components.database.dao.NoteHistoryDao
import com.chronotask.components.database.dao.TagDao
import com.chronotask.components.database.dao.TaskDao
import com.chronotask.components.database.dao.TaskRecordDao
import com.chronotask.components.database.entity.DailyRestEntity
import com.chronotask.components.database.entity.FocusSessionEntity
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.database.entity.TagEntity
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.entity.TaskRecordEntity

/**
 * AppDatabase - 应用数据库主类
 *
 * 核心职责：作为 Room 数据库的统一入口，管理所有实体、DAO 以及版本迁移。
 * 当前版本为 7，包含 6 张表：tasks、task_records、tags、daily_rest、note_history、focus_sessions。
 * 使用单例模式通过 [getDatabase] 获取数据库实例。
 */
@Database(
    entities = [
        TaskEntity::class,
        TaskRecordEntity::class,
        TagEntity::class,
        DailyRestEntity::class,
        NoteHistoryEntity::class,
        FocusSessionEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskRecordDao(): TaskRecordDao
    abstract fun tagDao(): TagDao
    abstract fun dailyRestDao(): DailyRestDao
    abstract fun noteHistoryDao(): NoteHistoryDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * MIGRATION_1_2 - 新增任务计划日期字段
         *
         * 变更：在 tasks 表上新增 scheduledDate 列并建立索引，用于日程管理。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledDate INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_scheduledDate ON tasks(scheduledDate)")
            }
        }

        /**
         * MIGRATION_2_3 - 新增 note_history 表
         *
         * 变更：创建 note_history 表用于存储计时笔记历史，包含 task 外键约束。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS note_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "taskId INTEGER NOT NULL," +
                        "date INTEGER NOT NULL," +
                        "sessionStartTime INTEGER NOT NULL," +
                        "durationSeconds INTEGER NOT NULL," +
                        "note TEXT NOT NULL DEFAULT ''," +
                        "FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_note_history_taskId_date_sessionStartTime " +
                        "ON note_history(taskId, date, sessionStartTime)"
                )
            }
        }

        /**
         * MIGRATION_3_4 - note_history 表新增 title 字段
         *
         * 变更：在 note_history 表上新增 title 列，用于存储笔记标题。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_history ADD COLUMN title TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * MIGRATION_4_5 - 移除外键约束
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE note_history_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "taskId INTEGER NOT NULL," +
                        "date INTEGER NOT NULL," +
                        "sessionStartTime INTEGER NOT NULL," +
                        "durationSeconds INTEGER NOT NULL," +
                        "note TEXT NOT NULL DEFAULT ''," +
                        "title TEXT NOT NULL DEFAULT '')"
                )
                db.execSQL(
                    "INSERT INTO note_history_new (id, taskId, date, sessionStartTime, durationSeconds, note, title) " +
                        "SELECT id, taskId, date, sessionStartTime, durationSeconds, note, title FROM note_history"
                )
                db.execSQL("DROP TABLE note_history")
                db.execSQL("ALTER TABLE note_history_new RENAME TO note_history")
                db.execSQL("CREATE UNIQUE INDEX index_note_history_taskId_date_sessionStartTime ON note_history(taskId, date, sessionStartTime)")
                db.execSQL("CREATE INDEX index_note_history_taskId ON note_history(taskId)")
            }
        }

        /**
         * MIGRATION_5_6 - 新增 sourceTaskTitle 字段
         *
         * 用途：标识笔记来源任务标题。任务笔记保存时填入，全局笔记 Tab 保存时为空。
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_history ADD COLUMN sourceTaskTitle TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * MIGRATION_6_7 - 新增已完成专注会话表。
         *
         * 旧版只有按天汇总数据，无法可靠恢复历史会话边界，因此不对旧数据做猜测性回填。
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS focus_sessions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "taskId INTEGER NOT NULL," +
                        "date INTEGER NOT NULL," +
                        "sessionStartTime INTEGER NOT NULL," +
                        "sessionEndTime INTEGER NOT NULL," +
                        "durationSeconds INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_date ON focus_sessions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_taskId ON focus_sessions(taskId)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_focus_sessions_taskId_sessionStartTime_sessionEndTime " +
                        "ON focus_sessions(taskId, sessionStartTime, sessionEndTime)"
                )
            }
        }

        /**
         * 获取数据库单例实例
         *
         * 使用双重检查锁模式保证全局唯一实例，并通过 Room 构建器注册所有迁移。
         * 迁移范围覆盖版本 1→2、2→3、3→4、4→5、5→6、6→7。
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronotask_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
