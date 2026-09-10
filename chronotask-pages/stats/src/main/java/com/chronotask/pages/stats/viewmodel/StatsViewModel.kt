package com.chronotask.pages.stats.viewmodel

import androidx.lifecycle.viewModelScope
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appApplication
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.components.common.base.BaseViewModel
import com.chronotask.components.database.AppDatabase
import com.chronotask.components.database.dao.DailyTagDuration
import com.chronotask.components.database.repository.FocusSessionRepository
import com.chronotask.components.ui.R
import com.chronotask.pages.stats.data.StatsPeriod
import com.chronotask.pages.stats.data.StatsPeriodRangeCalculator
import com.chronotask.pages.stats.data.StatsState
import com.chronotask.pages.stats.data.TagDistribution
import com.chronotask.pages.stats.ui.LineChartDataPoint
import kotlinx.coroutines.Job
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
 * - loadStats() 由 viewModelScope 管理；新请求会取消旧请求
 * - generateChartData() 先批量读取日摘要，再在内存组合图表数据点
 */
class StatsViewModel : BaseViewModel() {
    private val db = AppDatabase.getDatabase(appApplication)
    private val recordDao = db.taskRecordDao()
    private val restDao = db.dailyRestDao()

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.WEEK)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod

    private val requestGate = StatsRequestGate()
    private var statsLoadJob: Job? = null

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
        // 周期切换和语言切换都只关心最新结果；版本检查同时覆盖不及时响应取消的底层读取。
        val requestId = requestGate.beginRequest()
        statsLoadJob?.cancel()
        val period = _selectedPeriod.value
        statsLoadJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentRange = StatsPeriodRangeCalculator.current(period, now)
            val start = currentRange.start
            val end = currentRange.end
            val prevStart = StatsPeriodRangeCalculator.previous(period, start).start

            // 统计卡片只读取聚合结果，不加载整批计时记录。
            val summary = recordDao.getPeriodSummary(start, end)
            val prevSummary = recordDao.getPeriodSummary(prevStart, start)
            val focusCount = FocusSessionRepository.countQualifiedByDateRange(
                startDate = start,
                endDate = end,
                thresholdSeconds = TimerManager.FOCUS_SESSION_THRESHOLD_SECONDS
            )

            val totalSeconds = summary.totalSeconds
            val prevTotalSeconds = prevSummary.totalSeconds

            // 日均计算：从周期开始到今天的实际天数（起始日为第1天）
            val daysElapsed = ((now - start) / (24 * 60 * 60 * 1000)).toInt() + 1
            val avgSeconds = if (totalSeconds > 0L) totalSeconds / daysElapsed else 0L
            val workDays = summary.workDays

            // 生成图表和分布数据
            val tagDistributions = loadTagDistribution(start, end, totalSeconds)
            val chartData = generateChartData(period, start, end)
            val trend = if (prevTotalSeconds > 0)
                (totalSeconds - prevTotalSeconds).toFloat() / prevTotalSeconds
            else 0f

            if (!requestGate.isLatest(requestId)) return@launch
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
     * 从数据库加载标签时长分布。
     *
     * 标签关联和时长聚合均在 SQL 中完成，避免按任务逐条回查数据库。
     *
     * @return 按时长降序排列的标签分布列表
     */
    private suspend fun loadTagDistribution(
        startDate: Long,
        endDate: Long,
        totalSeconds: Long
    ): List<TagDistribution> {
        if (totalSeconds <= 0L) return emptyList()
        return recordDao.getTagDurationsByDateRange(
            startDate = startDate,
            endDate = endDate,
            uncategorizedName = appApplication.getString(R.string.uncategorized)
        ).map { row ->
            TagDistribution(
                tagName = row.tagName,
                totalSeconds = row.totalSeconds,
                percentage = row.totalSeconds.toFloat() / totalSeconds
            )
        }
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
        val chartData = ChartDataSource(
            dailyDurations = recordDao.getDailyDurations(start, end).associate { it.date to it.totalSeconds },
            dailyTagDurations = recordDao.getDailyTagDurations(
                startDate = start,
                endDate = end,
                uncategorizedName = appApplication.getString(R.string.uncategorized)
            ).groupBy { it.date },
            restDays = if (period == StatsPeriod.WEEK) {
                restDao.getRestDaysInRange(start, end - 1).map { it.date }.toSet()
            } else {
                emptySet()
            }
        )
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
                    addChartPoint(chartData, points, current, next, appApplication.getString(if (period == StatsPeriod.WEEK) R.string.chart_day_label else R.string.chart_week_label, index.toString()))
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
                    addChartPoint(chartData, points, monthStart, monthEnd, appApplication.getString(R.string.chart_month_label, index.toString()))
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
     * @param chartData 当前图表预取的日摘要
     * @param points 目标列表
     * @param rangeStart 时间窗口开始
     * @param rangeEnd 时间窗口结束
     * @param label 数据点标签文本
     */
    private fun addChartPoint(
        chartData: ChartDataSource,
        points: MutableList<LineChartDataPoint>,
        rangeStart: Long,
        rangeEnd: Long,
        label: String
    ) {
        val totalSeconds = chartData.dailyDurations.entries
            .asSequence()
            .filter { (date, _) -> date >= rangeStart && date < rangeEnd }
            .sumOf { it.value }
        val tagDistributions = chartData.tagDistribution(rangeStart, rangeEnd, totalSeconds)
        val isRestDay = rangeStart in chartData.restDays
        points.add(
            LineChartDataPoint(
                label = label,
                value = totalSeconds,
                tagDistributions = tagDistributions,
                isRestDay = isRestDay
            )
        )
    }

    private data class ChartDataSource(
        val dailyDurations: Map<Long, Long>,
        val dailyTagDurations: Map<Long, List<DailyTagDuration>>,
        val restDays: Set<Long>
    ) {
        fun tagDistribution(
            rangeStart: Long,
            rangeEnd: Long,
            totalSeconds: Long
        ): List<TagDistribution> {
            if (totalSeconds <= 0L) return emptyList()
            val totalsByTag = mutableMapOf<String, Long>()
            dailyTagDurations.forEach { (date, durations) ->
                if (date in rangeStart until rangeEnd) {
                    durations.forEach { duration ->
                        totalsByTag.merge(duration.tagName, duration.totalSeconds, Long::plus)
                    }
                }
            }
            return totalsByTag.map { (tagName, seconds) ->
                TagDistribution(
                    tagName = tagName,
                    totalSeconds = seconds,
                    percentage = seconds.toFloat() / totalSeconds
                )
            }.sortedByDescending { it.totalSeconds }
        }
    }
}
