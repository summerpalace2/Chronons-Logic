package com.chronotask.pages.stats.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appApplication
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.components.common.appIoScope
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.repository.FocusSessionRepository
import com.chronotask.components.ui.R
import com.chronotask.pages.stats.data.StatsPeriod
import com.chronotask.pages.stats.data.StatsState
import com.chronotask.pages.stats.data.TagDistribution
import com.chronotask.pages.stats.ui.LineChartDataPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * StatsViewModel - 统计页状态管理
 *
 * 核心职责：管理统计数据加载、周期切换、图表数据生成。
 *
 * 协程策略：
 * - loadStats() 涉及数据库读取，使用 appIoScope 保活
 * - generateChartData() 是纯计算，直接在协程内执行
 */
class StatsViewModel : BaseViewModel() {
    private val db = AppDatabase.getDatabase(appApplication)
    private val recordDao = db.taskRecordDao()
    private val tagDao = db.tagDao()
    private val taskDao = db.taskDao()
    private val restDao = db.dailyRestDao()

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod

    init {
        loadStats()
    }

    /**
     * 切换统计周期
     * @param period 目标周期（WEEK / MONTH / YEAR）
     */
    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
        loadStats()
    }

    /**
     * 加载统计数据
     *
     * 流程：
     * 1. 计算当前周期和上一周期的时间范围
     * 2. 读取数据库记录
     * 3. 计算总时长、日均、工作天数
     * 4. 计算标签分布和图表数据
     * 5. 更新 StateFlow
     */

    /**
     * 当系统语言切换时重新加载统计数据
     *
     * 使图表标签（天/周/月）和标签分布语言与当前 locale 一致。
     */
    fun reloadOnLocaleChange() {
        loadStats()
    }

    private fun loadStats() {
        appIoScope.launch {
            val period = _selectedPeriod.value
            val now = System.currentTimeMillis()
            val (start, end) = getPeriodRange(period, now)
            val prevStart = start - (end - start)

            // 读取数据库记录
            val records = recordDao.getRecordsByDateRange(start, end)
            val prevRecords = recordDao.getRecordsByDateRange(prevStart, start)
            val focusCount = FocusSessionRepository.countQualifiedByDateRange(
                startDate = start,
                endDate = end,
                thresholdSeconds = TimerManager.FOCUS_SESSION_THRESHOLD_SECONDS
            )

            // 聚合计算
            val totalSeconds = records.sumOf { it.durationSeconds }
            val prevTotalSeconds = prevRecords.sumOf { it.durationSeconds }

            // 日均计算：从周期开始到今天的实际天数（起始日为第1天）
            val daysElapsed = ((now - start) / (24 * 60 * 60 * 1000)).toInt() + 1
            val avgSeconds = if (records.isNotEmpty()) totalSeconds / daysElapsed else 0L
            val workDays = records.map { it.date }.distinct().size

            // 生成图表和分布数据
            val tagDistributions = calculateTagDistribution(records)
            val chartData = generateChartData(period, start, end)
            val trend = if (prevTotalSeconds > 0)
                (totalSeconds - prevTotalSeconds).toFloat() / prevTotalSeconds
            else 0f

            _state.value = StatsState(
                periodTotalSeconds = totalSeconds,
                periodAvgSeconds = avgSeconds,
                prevPeriodTotalSeconds = prevTotalSeconds,
                periodWorkDays = workDays,
                focusCount = focusCount,
                tagDistributions = tagDistributions,
                chartData = chartData,
                avgTrend = trend
            )
        }
    }

    /**
     * 计算周期时间范围
     * @param period 统计周期
     * @param now 当前时间戳（毫秒）
     * @return Pair<startMs, endMs>
     */
    private fun getPeriodRange(period: StatsPeriod, now: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return when (period) {
            StatsPeriod.WEEK -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val offset = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -offset)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 7)
                Pair(start, cal.timeInMillis)
            }
            StatsPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                Pair(start, cal.timeInMillis)
            }
            StatsPeriod.YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                Pair(start, cal.timeInMillis)
            }
        }
    }

    /**
     * 计算标签时长分布
     *
     * 按 taskId 聚合后查找 tag，避免同名 tag 重复。
     *
     * @param records 当前周期的任务记录列表
     * @return 按时长降序排列的标签分布列表
     */
    private suspend fun calculateTagDistribution(
        records: List<com.chronotask.components.database.entity.TaskRecordEntity>
    ): List<TagDistribution> {
        if (records.isEmpty()) return emptyList()
        val totalSeconds = records.sumOf { it.durationSeconds }
        if (totalSeconds == 0L) return emptyList()
        val tags = tagDao.getAllTagsSync()
        return records.groupBy { it.taskId }
            .mapNotNull { (taskId, taskRecords) ->
                val task = taskDao.getTaskById(taskId) ?: return@mapNotNull null
                val tag = tags.find { it.id == task.tagId }
                val seconds = taskRecords.sumOf { it.durationSeconds }
                Pair(tag?.name ?: appApplication.getString(R.string.uncategorized), seconds)
            }
            .groupBy { it.first }
            .map { (tagName, entries) ->
                val totalSec = entries.sumOf { it.second }
                TagDistribution(
                    tagName = tagName,
                    totalSeconds = totalSec,
                    percentage = totalSec.toFloat() / totalSeconds
                )
            }
            .sortedByDescending { it.totalSeconds }
    }

    /**
     * 生成折线图数据
     *
     * 按周期类型分组：周视图按天、月视图按周、年视图按月。
     * 每个数据点包含该时长的标签分布和休息日状态。
     *
     * @param period 统计周期
     * @param start 周期开始时间戳
     * @param end 周期结束时间戳
     * @return 折线图数据点列表
     */
    private suspend fun generateChartData(
        period: StatsPeriod,
        start: Long,
        end: Long
    ): List<LineChartDataPoint> {
        val tags = tagDao.getAllTagsSync()
        val points = mutableListOf<LineChartDataPoint>()
        var index = 1

        when (period) {
            StatsPeriod.WEEK, StatsPeriod.MONTH -> {
                val step = when (period) {
                    StatsPeriod.WEEK -> 24 * 60 * 60 * 1000L
                    StatsPeriod.MONTH -> 7 * 24 * 60 * 60 * 1000L
                    else -> 24 * 60 * 60 * 1000L
                }
                val labelUnit = if (period == StatsPeriod.WEEK) appApplication.getString(R.string.day) else appApplication.getString(R.string.week)
                var current = start
                while (current < end) {
                    val next = minOf(current + step, end)
                    addChartPoint(points, current, next, tags, appApplication.getString(if (period == StatsPeriod.WEEK) R.string.chart_day_label else R.string.chart_week_label, index.toString()))
                    index++
                    current = next
                }
            }

            StatsPeriod.YEAR -> {
                // 按自然月分组，避免出现"第13月"
                val cal = Calendar.getInstance()
                cal.timeInMillis = start
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                while (cal.timeInMillis < end) {
                    val monthStart = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val monthEnd = minOf(cal.timeInMillis, end)
                    addChartPoint(points, monthStart, monthEnd, tags, appApplication.getString(R.string.chart_month_label, index.toString()))
                    index++
                }
            }
        }
        return points
    }

    /**
     * 生成单个折线图数据点并添加到列表
     *
     * 提取公共逻辑，消除 WEEK/MONTH/YEAR 分支中的重复代码。
     *
     * @param points 目标列表
     * @param rangeStart 时间窗口开始
     * @param rangeEnd 时间窗口结束
     * @param tags 所有标签列表（避免重复查询）
     * @param index 当前序号
     * @param label 数据点标签文本
     */
    private suspend fun addChartPoint(
        points: MutableList<LineChartDataPoint>,
        rangeStart: Long,
        rangeEnd: Long,
        tags: List<com.chronotask.components.database.entity.TagEntity>,
        label: String
    ) {
        val records = recordDao.getRecordsByDateRange(rangeStart, rangeEnd)
        val totalSeconds = records.sumOf { it.durationSeconds }
        val tagDistributions = calculatePointTagDistribution(records, totalSeconds, tags)
        val isRestDay = restDao.getRestByDate(rangeStart)?.isRestDay ?: false
        points.add(
            LineChartDataPoint(
                label = label,
                value = totalSeconds,
                tagDistributions = tagDistributions,
                isRestDay = isRestDay
            )
        )
    }

    /**
     * 计算单个数据点的标签分布
     *
     * @param records 该时间窗口内的记录
     * @param totalSeconds 该窗口的总秒数
     * @param tags 所有标签列表（避免重复查询）
     * @return 标签分布列表
     */
    private suspend fun calculatePointTagDistribution(
        records: List<com.chronotask.components.database.entity.TaskRecordEntity>,
        totalSeconds: Long,
        tags: List<com.chronotask.components.database.entity.TagEntity>
    ): List<TagDistribution> {
        if (records.isEmpty() || totalSeconds <= 0) return emptyList()
        return records.groupBy { it.taskId }
            .mapNotNull { (taskId, taskRecords) ->
                val task = taskDao.getTaskById(taskId) ?: return@mapNotNull null
                val tag = tags.find { it.id == task.tagId }
                val seconds = taskRecords.sumOf { it.durationSeconds }
                Pair(tag?.name ?: appApplication.getString(R.string.uncategorized), seconds)
            }
            .groupBy { it.first }
            .map { (tagName, entries) ->
                val totalSec = entries.sumOf { it.second }
                TagDistribution(
                    tagName = tagName,
                    totalSeconds = totalSec,
                    percentage = totalSec.toFloat() / totalSeconds
                )
            }
            .sortedByDescending { it.totalSeconds }
    }
}
