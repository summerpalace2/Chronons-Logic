package com.chronotask.pages.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.common.FormatUtils
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.components.ui.R
import com.chronotask.pages.stats.data.StatsPeriod
import com.chronotask.pages.stats.data.label
import com.chronotask.pages.stats.data.TagDistribution
import com.chronotask.pages.stats.viewmodel.StatsViewModel

/**
 * StatsScreen - 统计页
 *
 * 核心职责：展示统计数据，包括总览、趋势对比、折线图、饼图。
 * 布局结构（从上到下）：
 *   1. 时间段选择器（StatsPeriodSelector）
 *   2. 总览卡片（StatsOverviewCard）
 *   3. 趋势对比卡片（StatsTrendCard）
 *   4. 专注次数卡片（StatsFocusCard）
 *   5. 折线图卡片（StatsLineChartCard）
 *   6. 选中点的饼图（条件显示）
 *   7. 整体饼图（条件显示）
 *   8. 注释卡片
 *
 * 状态管理：所有数据状态上移至 StatsViewModel。
 * 本地 UI 状态（弹窗、选中点）使用 remember 管理。
 */
@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    // ── 本地 UI 状态 ──────────────────────────────────────────
    var showFlowInfo by remember { mutableStateOf(false) }
    var selectedPointTags by remember { mutableStateOf<List<TagDistribution>>(emptyList()) }
    var selectedPointLabel by remember { mutableStateOf("") }

    // 切换周期时清除选中点
    LaunchedEffect(selectedPeriod) {
        selectedPointTags = emptyList()
        selectedPointLabel = ""
    }

    // 语言切换时重新加载统计数据（图表标签、标签分布翻译）
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    LaunchedEffect(currentLocale) {
        viewModel.reloadOnLocaleChange()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 1. 时间段选择器 ──
            StatsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelect = viewModel::selectPeriod
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. 总览卡片 ──
            StatsOverviewCard(
                totalSeconds = state.periodTotalSeconds,
                avgSeconds = state.periodAvgSeconds
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 3. 趋势对比卡片 ──
            StatsTrendCard(
                prevTotalSeconds = state.prevPeriodTotalSeconds,
                avgTrend = state.avgTrend,
                selectedPeriod = selectedPeriod
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 4. 专注次数卡片 ──
            StatsFocusCard(
                focusCount = state.focusCount,
                onInfoClick = { showFlowInfo = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 5. 折线图卡片 ──
            StatsLineChartCard(
                chartData = state.chartData,
                selectedPeriod = selectedPeriod,
                onPointSelected = { _, point ->
                    if (point == null) {
                        selectedPointTags = emptyList()
                        selectedPointLabel = ""
                    } else {
                        selectedPointTags = point.tagDistributions
                        selectedPointLabel = point.label
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 6. 选中点的标签分类饼图 ──
            if (selectedPointTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                StatsPieChartSection(
                    title = stringResource(R.string.time_distribution, selectedPointLabel),
                    data = selectedPointTags
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 7. 整体分类统计（饼图） ──
            if (state.tagDistributions.isNotEmpty()) {
                StatsPieChartSection(
                    title = stringResource(R.string.tag_distribution),
                    data = state.tagDistributions
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 8. 注释卡片 ──
            StatsNoteCard()

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── 心流说明弹窗 ──
    if (showFlowInfo) {
        StatsFlowInfoDialog(onDismiss = { showFlowInfo = false })
    }
}

/**
 * StatsPeriodSelector - 时间段选择器
 *
 * 横向排列的周期切换按钮，选中项高亮显示。
 *
 * @param selectedPeriod 当前选中的周期
 * @param onPeriodSelect 选择周期回调
 */
@Composable
private fun StatsPeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelect: (StatsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                    .clickable { onPeriodSelect(period) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * StatsOverviewCard - 总览卡片
 *
 * 展示周期总时长和日均时长。
 *
 * @param totalSeconds 周期总秒数
 * @param avgSeconds 日均秒数
 */
@Composable
private fun StatsOverviewCard(
    totalSeconds: Long,
    avgSeconds: Long
) {
    StatsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.total_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FormatUtils.formatDuration(totalSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.daily_avg, FormatUtils.formatDuration(avgSeconds)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * StatsTrendCard - 趋势对比卡片
 *
 * 展示与上一周期的对比，包括百分比变化和趋势图标。
 *
 * @param prevTotalSeconds 上一周期总秒数
 * @param avgTrend 变化率（正数增长，负数下降）
 * @param selectedPeriod 当前周期（用于显示对比标签）
 */
@Composable
private fun StatsTrendCard(
    prevTotalSeconds: Long,
    avgTrend: Float,
    selectedPeriod: StatsPeriod
) {
    StatsCard {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = if (avgTrend >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.prev_period_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val trendLabel = when (selectedPeriod) {
                StatsPeriod.WEEK -> stringResource(R.string.vs_last_week)
                StatsPeriod.MONTH -> stringResource(R.string.vs_last_month)
                StatsPeriod.YEAR -> stringResource(R.string.vs_last_year)
            }
            val noDataLabel = when (selectedPeriod) {
                StatsPeriod.WEEK -> stringResource(R.string.last_week_short)
                StatsPeriod.MONTH -> stringResource(R.string.last_month_short)
                StatsPeriod.YEAR -> stringResource(R.string.last_year_short)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = FormatUtils.formatDuration(prevTotalSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (prevTotalSeconds > 0) {
                    val percentValue = (avgTrend * 100)
                    val percentText = if (percentValue >= 0) "+${String.format("%.1f", percentValue)}%"
                    else "${String.format("%.1f", percentValue)}%"
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (avgTrend >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (avgTrend >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = if (avgTrend > 0) stringResource(R.string.trend_up) else stringResource(R.string.trend_down),
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = if (avgTrend >= 0) 0f else 180f },
                        tint = if (avgTrend >= 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = noDataLabel + stringResource(R.string.no_data_compare),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * StatsFocusCard - 专注次数卡片
 *
 * 展示周期内的专注次数，附带信息图标可查看说明。
 *
 * @param focusCount 专注次数
 * @param onInfoClick 点击信息图标回调
 */
@Composable
private fun StatsFocusCard(
    focusCount: Int,
    onInfoClick: () -> Unit
) {
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.focus_count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Info,
                        contentDescription = stringResource(R.string.flow_info_hint),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onInfoClick() },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.focus_count_value, focusCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.flow_condition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * StatsLineChartCard - 折线图卡片
 *
 * 展示效率趋势折线图，支持点击数据点查看标签分布。
 *
 * @param chartData 折线图数据
 * @param selectedPeriod 当前统计周期
 * @param onPointSelected 数据点选中回调
 */
@Composable
private fun StatsLineChartCard(
    chartData: List<LineChartDataPoint>,
    selectedPeriod: StatsPeriod,
    onPointSelected: (Int, LineChartDataPoint?) -> Unit
) {
    StatsCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.efficiency_trend),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.rest_day_excluded),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            LineChart(
                data = chartData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                onPointSelected = onPointSelected
            )
        }
    }
}

/**
 * StatsPieChartSection - 饼图区块
 *
 * 展示标签分类分布饼图，带标题。
 *
 * @param title 区块标题
 * @param data 饼图数据（标签分布列表）
 */
@Composable
private fun StatsPieChartSection(
    title: String,
    data: List<TagDistribution>
) {
    StatsCard {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        PieChart(data = data)
    }
}

/**
 * StatsNoteCard - 注释卡片
 *
 * 底部说明文字，解释休息日排除规则。
 */
@Composable
private fun StatsNoteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = stringResource(R.string.rest_day_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * StatsFlowInfoDialog - 心流说明弹窗
 *
 * 解释专注次数的计算规则和提升策略。
 *
 * @param onDismiss 关闭弹窗回调
 */
@Composable
private fun StatsFlowInfoDialog(
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.focus_count_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.flow_info_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.flow_info_why_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.flow_info_why_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.flow_info_strategy_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.flow_info_strategy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.flow_info_daily_limit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.got_it))
            }
        }
    )
}