package com.chronotask.pages.taskdetail.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronotask.components.ui.R
import com.chronotask.components.common.appDataStore
import com.chronotask.components.ui.picker.VerticalTimePickerDialog
import com.chronotask.pages.taskdetail.data.TaskComparisonData
import com.chronotask.pages.taskdetail.viewmodel.TaskDetailViewModel

/**
 * TimerContent - 计时视图
 *
 * 核心职责：展示计时圆环、控制按钮（开始/停止）、目标时长卡片、横向对比数据。
 * 提供手动编辑时间和诚实提醒功能。
 *
 * 布局结构：
 *   1. 顶部操作栏（编辑 + Info）
 *   2. 计时圆环
 *   3. 控制按钮组
 *   4. 目标时长卡片
 *   5. 横向对比卡片（条件显示）
 *   6. 弹窗（手动编辑、诚实提醒）
 */
@Composable
internal fun TimerContent(
    isRunning: Boolean,
    elapsedSeconds: Long,
    totalSeconds: Long,
    targetMinutes: Int?,
    progress: Float?,
    animatedHourProgress: Float,
    comparisonData: TaskComparisonData,
    onShowTimePicker: (Boolean) -> Unit,
    viewModel: TaskDetailViewModel,
    canTime: Boolean = true
) {
    val horizontalComparison by appDataStore.horizontalComparison.collectAsState(initial = true)

    var showManualTimePicker by remember { mutableStateOf(false) }
    var showHonestyDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // 1. 顶部操作栏
        TimerActionBar(
            onEditClick = { showManualTimePicker = true },
            onInfoClick = { showHonestyDialog = true }
        )

        // 2. 计时圆环
        TimerRing(
            totalSeconds = totalSeconds,
            animatedHourProgress = animatedHourProgress,
            isRunning = isRunning
        )

        // 3. 控制按钮组
        TimerControls(
            isRunning = isRunning,
            onStart = { viewModel.startTimer() },
            onStop = { viewModel.stopTimer() },
            canTime = canTime
        )

        // 4. 目标时长卡片
        Spacer(modifier = Modifier.height(16.dp))
        TargetDurationCard(
            totalSeconds = totalSeconds,
            targetMinutes = targetMinutes,
            progress = progress,
            onEditTarget = { onShowTimePicker(true) }
        )

        // 5. 横向对比卡片
        if (horizontalComparison && (comparisonData.weekAvgSeconds > 0 || comparisonData.monthAvgSeconds > 0)) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalComparisonCard(comparisonData = comparisonData)
        }
    }

    // 6. 弹窗
    if (showManualTimePicker) {
        VerticalTimePickerDialog(
            initialHours = elapsedSeconds.toInt() / 3600,
            initialMinutes = (elapsedSeconds.toInt() % 3600) / 60,
            onConfirm = { hours, minutes ->
                viewModel.addManualTime(hours, minutes)
                showManualTimePicker = false
            },
            onDismiss = { showManualTimePicker = false }
        )
    }

    if (showHonestyDialog) {
        HonestyDialog(onDismiss = { showHonestyDialog = false })
    }
}

/**
 * TimerActionBar - 顶部操作栏（编辑 + Info）
 */
@Composable
private fun TimerActionBar(
    onEditClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
            onClick = onInfoClick,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "i",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * TimerRing - 计时圆环
 *
 * 核心职责：绘制圆环（背景环 + 进度弧）+ 中心时间显示。
 *
 * @param totalSeconds 总秒数
 * @param animatedHourProgress 当前小时内的进度（0~1）
 * @param isRunning 是否正在计时
 */
@Composable
private fun TimerRing(
    totalSeconds: Long,
    animatedHourProgress: Float,
    isRunning: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        val ringSize = 200.dp
        val animatedProgress by animateFloatAsState(
            targetValue = animatedHourProgress,
            animationSpec = tween(durationMillis = 500),
            label = "ring_progress"
        )
        val bgRingColor = Color(0xFFBDBDBD).copy(alpha = 0.12f)
        val ringProgressColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isRunning) 1f else 0.4f)
        val innerCircleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidth = 12.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                color = bgRingColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawCircle(
                color = innerCircleColor,
                radius = radius - strokeWidth,
                center = center
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = ringProgressColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDurationHHmmss(totalSeconds),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isRunning) 1f else 0.4f)
            )
        }
    }
}

/**
 * TimerControls - 计时控制按钮组（开始 + 停止）
 *
 * @param isRunning 是否正在计时
 * @param onStart 开始计时回调
 * @param onStop 停止计时回调
 */
@Composable
private fun TimerControls(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    canTime: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onStart,
            enabled = !isRunning && canTime,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.start))
        }
        Spacer(modifier = Modifier.width(16.dp))
        FilledTonalButton(
            onClick = onStop,
            enabled = isRunning,
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.stop))
        }
    }
}

/**
 * TargetDurationCard - 目标时长卡片
 *
 * 核心职责：展示当前进度（已用/目标）+ 进度条。
 *
 * @param totalSeconds 已用秒数
 * @param targetMinutes 目标分钟数（null 表示未设置）
 * @param progress 进度比例（0~1）
 * @param onEditTarget 编辑目标回调
 */
@Composable
private fun TargetDurationCard(
    totalSeconds: Long,
    targetMinutes: Int?,
    progress: Float?,
    onEditTarget: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.target_duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onEditTarget, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (targetMinutes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatDuration(totalSeconds), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = formatDurationMinutes(targetMinutes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${((progress ?: 0f) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Custom progress bar without indicator dot
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .drawWithContent {
                            // Draw track
                            drawRoundRect(
                                color = trackColor,
                                size = size,
                                cornerRadius = CornerRadius(size.height / 2)
                            )
                            // Draw progress
                            val barWidth = size.width * (progress ?: 0f)
                            drawRoundRect(
                                color = primaryColor,
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(size.height / 2)
                            )
                        }
                )
            }
        }
    }
}

/**
 * HorizontalComparisonCard - 横向对比卡片
 *
 * 核心职责：展示本周和本月的日均时长对比。
 *
 * @param comparisonData 对比数据
 */
@Composable
private fun HorizontalComparisonCard(comparisonData: TaskComparisonData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.horizontal_comparison_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.this_week_avg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatDuration(comparisonData.weekAvgSeconds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.this_month_avg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatDuration(comparisonData.monthAvgSeconds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * HonestyDialog - 诚实提醒弹窗
 *
 * 核心职责：提醒用户手动补录时间时要诚实。
 *
 * @param onDismiss 关闭回调
 */
@Composable
private fun HonestyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.got_it))
            }
        },
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = stringResource(R.string.note_editor_timer), fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                text = "如果当天完成任务时忘记计时，可以手动编辑大概时间，切记不要自己欺骗自己！",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
}
