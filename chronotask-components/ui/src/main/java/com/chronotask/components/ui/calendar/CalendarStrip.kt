/*
 * CalendarStrip.kt
 *
 * 日历条组件 — 展示周历条、月份导航、选中日期高亮。
 * 属于 chronotask-components UI 模块中的日历子模块。
 *
 * 核心导出：
 *   - CalendarStrip — 可横向滑动的周历条 UI 组件
 *
 * 性能说明：
 *   - 周起始日与选中索引通过 remember(selectedDate) 缓存，避免每次重复创建 Calendar 实例
 *   - 使用 MILLIS_PER_DAY 常量消除散布各处的魔法数字
 */

package com.chronotask.components.ui.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.DateUtils
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator
import com.chronotask.components.ui.theme.LocaleManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 一天的毫秒数，用于日期与毫秒之间的换算，避免散落各处的魔法数字。 */
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000L

/**
 * 日历条组件 — 展示一周七天的横向可滑动条。
 *
 * 提供一个周视图的日历条，支持：
 * - 选中某一天（高亮显示）
 * - 今天与过去/未来的视觉区分
 * - 左右切换上一周 / 下一周
 * - 点击月份标题触发月份选择回调
 *
 * @param selectedDate 当前选中的日期（毫秒时间戳），以当天 00:00:00 为准
 * @param onDateSelected 用户点击某一天时的回调，回传选中日期的毫秒时间戳
 * @param onPrevWeek 点击左箭头时的回调，切换到上一周
 * @param onNextWeek 点击右箭头时的回调，切换到下一周
 * @param onMonthClick 点击月份标题行时的回调，默认空实现
 */
@Composable
fun CalendarStrip(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onMonthClick: () -> Unit = {}
) {
    val todayStart = DateUtils.getTodayStart()
    // 监听 locale 变化：切换语言后格式与语言立即更新
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    val monthYearFormat = remember(currentLocale) {
        SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(currentLocale, "yyyyMMM"), currentLocale)
    }
    val dayOfWeekFormat = remember(currentLocale) { SimpleDateFormat("EEE", currentLocale) }
    val dayFormat = remember(currentLocale) { SimpleDateFormat("dd", currentLocale) }

    // 将 weekStart 与 selectedIndex 包裹在 remember(selectedDate) 中，
    // 避免每次重组都重复创建 Calendar 实例，减少滚动时的对象分配。
    val (weekStart, selectedIndex) = remember(selectedDate) {
        val ws = Calendar.getInstance().apply {
            timeInMillis = DateUtils.getWeekStart(selectedDate)
        }
        val idx =
            ((selectedDate - DateUtils.getWeekStart(selectedDate)) / MILLIS_PER_DAY).toInt()
        ws to idx
    }

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 只在选中周六/周日时自动滚动，使用动画避免割裂感
    LaunchedEffect(selectedDate) {
        val scrollTarget = when {
            selectedIndex >= 5 -> with(density) { (60.dp * (selectedIndex - 4)).toPx().toInt() }
            else -> 0
        }
        scrollState.animateScrollTo(scrollTarget)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 月份标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickableNoIndicator { onMonthClick() }
            ) {
                Text(
                    text = monthYearFormat.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = stringResource(R.string.date_picker),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Row {
                IconButton(onClick = onPrevWeek, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.prev_week),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onNextWeek, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_week),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 星期日期条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..6) {
                val dayCal = (weekStart.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, i)
                }
                val dayStart = DateUtils.getDateStart(dayCal.timeInMillis)
                val isToday = dayStart == todayStart
                val isSelected = dayStart == selectedDate
                val isPast = dayStart < todayStart && !isToday

                val bgColor: Color
                val contentColor: Color

                when {
                    isSelected -> {
                        bgColor = MaterialTheme.colorScheme.primary
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    }
                    isPast -> {
                        bgColor = MaterialTheme.colorScheme.surfaceContainerLow
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    else -> {
                        bgColor = MaterialTheme.colorScheme.surface
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    }
                }

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .then(
                            if (isToday && !isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickableNoIndicator { onDateSelected(dayStart) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayOfWeekFormat.format(Date(dayStart)),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = if (isSelected) 1f else 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dayFormat.format(Date(dayStart)),
                            style = MaterialTheme.typography.titleLarge,
                            color = contentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isToday || isSelected) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.inversePrimary
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 今日休息切换 ───