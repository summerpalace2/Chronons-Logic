package com.chronotask.pages.stats.data

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.chronotask.pages.stats.ui.LineChartDataPoint

/**
 * StatsPeriod - 统计周期枚举
 *
 * @param labelRes 周期显示名称的字符串资源 ID（周 / 月 / 年）；通过 [label] 在 Compose 层获取实际字符串。
 */
enum class StatsPeriod(@StringRes val labelRes: Int) {
    WEEK(com.chronotask.components.ui.R.string.week),
    MONTH(com.chronotask.components.ui.R.string.month),
    YEAR(com.chronotask.components.ui.R.string.year)
}

/**
 * Compose 扩展：根据当前 locale 实时读取周期显示名称。
 *
 * 使用 [stringResource] 而非缓存的 [StatsPeriod.labelRes] 字符串，确保切换语言后立即刷新。
 */
@Composable
fun StatsPeriod.label(): String = stringResource(id = labelRes)

/**
 * StatsState - 统计页完整状态（不可变数据类）
 *
 * 包含统计页所有展示数据，由 StatsViewModel 统一更新。
 *
 * @param periodTotalSeconds 当前周期总秒数
 * @param periodAvgSeconds 日均秒数
 * @param prevPeriodTotalSeconds 上一周期总秒数（用于趋势对比）
 * @param periodWorkDays 工作天数（有记录的天数）
 * @param focusCount 专注次数（记录条数）
 * @param tagDistributions 整体标签时长分布
 * @param chartData 折线图数据
 * @param avgTrend 日均变化率（正数增长，负数下降）
 */
data class StatsState(
    val periodTotalSeconds: Long = 0,
    val periodAvgSeconds: Long = 0,
    val prevPeriodTotalSeconds: Long = 0,
    val periodWorkDays: Int = 0,
    val focusCount: Int = 0,
    val tagDistributions: List<TagDistribution> = emptyList(),
    val chartData: List<LineChartDataPoint> = emptyList(),
    val avgTrend: Float = 0f
)

/**
 * TagDistribution - 标签时长分布
 *
 * @param tagName 标签名称
 * @param totalSeconds 该标签的总秒数
 * @param percentage 占总时长的比例（0~1）
 */
data class TagDistribution(
    val tagName: String,
    val totalSeconds: Long,
    val percentage: Float
)