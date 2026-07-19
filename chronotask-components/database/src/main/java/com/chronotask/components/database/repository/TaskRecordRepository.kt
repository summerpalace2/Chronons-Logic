package com.chronotask.components.database.repository

import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.entity.TaskRecordEntity
import com.chronotask.components.common.appApplication
import kotlinx.coroutines.flow.Flow

/**
 * 任务记录仓库 - 单例模式 (object)
 *
 * 负责 task_records 表的读写操作，提供计时记录的增删改查、时长统计、
 * 按日期/任务/范围查询，以及周/月平均时长的计算。
 * 采用单例模式，无状态，仅委托调用 DAO。
 */
object TaskRecordRepository {
    private val dao = AppDatabase.getDatabase(appApplication).taskRecordDao()

    /**
     * 获取指定任务在指定日期的记录
     * @param taskId 任务 ID
     * @param date   当日零点的毫秒时间戳
     * @return 对应的记录实体，不存在时返回 null
     */
    suspend fun getByTaskAndDate(taskId: Long, date: Long) = dao.getRecordByTaskAndDate(taskId, date)

    /**
     * 获取指定日期的所有记录
     * @param date 当日零点的毫秒时间戳
     * @return 该日期下所有任务的记录列表
     */
    suspend fun getByDate(date: Long) = dao.getRecordsByDate(date)

    /**
     * 获取指定日期的所有记录（响应式 Flow）
     * @param date 当日零点的毫秒时间戳
     * @return 发射该日期下所有任务记录列表的 Flow
     */
    fun getByDateFlow(date: Long): Flow<List<TaskRecordEntity>> = dao.getRecordsByDateFlow(date)

    /**
     * 获取指定日期的总计时时长
     * @param date 当日零点的毫秒时间戳
     * @return 发射当日所有任务总秒数的 Flow，无记录时发射 null
     */
    fun getTotalDurationByDate(date: Long): Flow<Long?> = dao.getTotalDurationByDate(date)

    /**
     * 获取指定任务的所有记录
     * @param taskId 任务 ID
     * @return 该任务下所有记录列表，按日期降序排列
     */
    suspend fun getByTask(taskId: Long) = dao.getRecordsByTask(taskId)

    /**
     * 获取日期范围内的所有记录
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 符合条件的记录列表
     */
    suspend fun getByDateRange(startDate: Long, endDate: Long) = dao.getRecordsByDateRange(startDate, endDate)

    /**
     * 插入一条计时记录
     * @param record 待插入的任务记录实体
     * @return 新插入记录的主键 ID
     */
    suspend fun insert(record: TaskRecordEntity) = dao.insertRecord(record)

    /**
     * 更新记录（完整实体替换）
     * @param record 待更新的任务记录实体（需包含主键 ID）
     */
    suspend fun update(record: TaskRecordEntity) = dao.updateRecord(record)

    /**
     * 更新记录时长
     * @param id               记录 ID
     * @param durationSeconds  新的时长（秒）
     */
    suspend fun updateDuration(id: Long, durationSeconds: Long) = dao.updateDuration(id, durationSeconds)

    /**
     * 删除指定记录
     * @param id 记录 ID
     */
    suspend fun delete(id: Long) = dao.deleteRecord(id)

    /**
     * 保存计时结果到数据库（不含笔记）
     * 内部通过 DAO 原子 UPSERT 实现：有记录则增量累加时长，无记录则插入
     * @param taskId         任务 ID
     * @param dateStart      计时开始日期（当日零点毫秒时间戳）
     * @param elapsedSeconds 本次计时的秒数
     */
    suspend fun saveTimerResult(taskId: Long, dateStart: Long, elapsedSeconds: Long) {
        dao.upsertDuration(taskId, dateStart, elapsedSeconds)
    }

    /**
     * 保存计时结果 + 笔记到数据库（原子操作）
     * 有记录则增量更新时长并写入笔记，无记录则插入含笔记的新记录
     * @param taskId         任务 ID
     * @param dateStart      计时开始日期（当日零点毫秒时间戳）
     * @param elapsedSeconds 本次计时的秒数
     * @param note           本次计时关联的笔记内容
     */
    suspend fun saveTimerResultWithNote(taskId: Long, dateStart: Long, elapsedSeconds: Long, note: String) {
        dao.upsertDurationAndNote(taskId, dateStart, elapsedSeconds, note)
    }

    /**
     * 仅更新指定任务、指定日期的笔记内容
     * @param taskId 任务 ID
     * @param date   当日零点毫秒时间戳
     * @param note   新的笔记内容
     */
    suspend fun updateNote(taskId: Long, date: Long, note: String) {
        dao.updateNote(taskId, date, note)
    }

    /**
     * 获取本周平均每日时长（秒）
     * 从本周一零点到现在，总和 / 实际天数
     * 在数据库层按任务 ID + 日期范围过滤，避免全量拉取
     * @param taskId 任务 ID
     * @param today  今日零点的毫秒时间戳
     * @return 本周平均每日秒数，无记录时返回 0
     */
    suspend fun getWeekAverage(taskId: Long, today: Long): Long {
        val weekStart = com.chronotask.components.common.DateUtils.getWeekStart(today)
        val tomorrow = today + 24 * 60 * 60 * 1000
        val taskRecords = dao.getRecordsByTaskAndDateRange(taskId, weekStart, tomorrow)
        if (taskRecords.isEmpty()) return 0
        val days = ((today - weekStart) / (24 * 60 * 60 * 1000)).toInt() + 1
        return taskRecords.sumOf { it.durationSeconds } / days
    }

    /**
     * 获取本月平均每日时长（秒）
     * 从本月1号零点到现在，总和 / 实际天数
     * 在数据库层按任务 ID + 日期范围过滤，避免全量拉取
     * @param taskId 任务 ID
     * @param today  今日零点的毫秒时间戳
     * @return 本月平均每日秒数，无记录时返回 0
     */
    suspend fun getMonthAverage(taskId: Long, today: Long): Long {
        val monthStart = com.chronotask.components.common.DateUtils.getMonthStart(today)
        val tomorrow = today + 24 * 60 * 60 * 1000
        val taskRecords = dao.getRecordsByTaskAndDateRange(taskId, monthStart, tomorrow)
        if (taskRecords.isEmpty()) return 0
        val days = ((today - monthStart) / (24 * 60 * 60 * 1000)).toInt() + 1
        return taskRecords.sumOf { it.durationSeconds } / days
    }

    /**
     * 按天切分并保存计时结果，独立原子累加。
     */
    suspend fun saveTimerResultByDays(taskId: Long, startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        val splits = com.chronotask.components.common.DateUtils.splitByDay(startMs, endMs)
        for ((dayStart, seconds) in splits) {
            dao.upsertDuration(taskId, dayStart, seconds)
        }
    }

    /**
     * 多任务本周日均秒数。
     */
    suspend fun getWeekAverageByIds(taskIds: List<Long>, today: Long): Long {
        if (taskIds.isEmpty()) return 0
        val weekStart = com.chronotask.components.common.DateUtils.getWeekStart(today)
        val total = dao.sumDurationBetweenByIds(taskIds, weekStart, today)
        if (total <= 0) return 0
        val days = ((today - weekStart) / (24 * 60 * 60 * 1000L)).toInt() + 1
        return total / days
    }

    /**
     * 多任务本月日均秒数。
     */
    suspend fun getMonthAverageByIds(taskIds: List<Long>, today: Long): Long {
        if (taskIds.isEmpty()) return 0
        val monthStart = com.chronotask.components.common.DateUtils.getMonthStart(today)
        val total = dao.sumDurationBetweenByIds(taskIds, monthStart, today)
        if (total <= 0) return 0
        val days = ((today - monthStart) / (24 * 60 * 60 * 1000L)).toInt() + 1
        return total / days
    }
}