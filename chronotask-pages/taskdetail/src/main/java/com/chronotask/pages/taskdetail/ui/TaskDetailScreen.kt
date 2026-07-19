package com.chronotask.pages.taskdetail.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.chronotask.components.ui.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.appDataStore
import com.chronotask.components.ui.picker.VerticalTimePickerDialog
import com.chronotask.pages.taskdetail.viewmodel.TaskDetailViewModel

/**
 * TaskDetailScreen - 任务详情页骨架
 *
 * 核心职责：管理页面状态（计时/笔记切换、时间选择器），
 *          组合 TimerContent / NotesContent 子视图。
 * 布局结构：
 *   1. 顶部导航条（TaskDetailHeader）
 *   2. 分段切换（TaskSegmentSwitch）
 *   3. 内容区（TimerContent / NotesContent）
 *   4. 时间选择器弹窗（覆盖层）
 *
 * 状态管理：数据状态全部上移至 ViewModel，UI 状态（分段选择、弹窗）使用本地 remember。
 */
@Composable
fun TaskDetailScreen(taskId: Long, date: Long = 0) {
    val viewModel: TaskDetailViewModel = viewModel { TaskDetailViewModel(taskId, date) }

    // ── ViewModel 状态收集 ─────────────────────────────────
    val task by viewModel.task.collectAsState()
    val tagName by viewModel.tagName.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val todayRecordSeconds by viewModel.todayRecordSeconds.collectAsState()
    val currentNote by viewModel.currentNote.collectAsState()
    val comparisonData by viewModel.comparisonData.collectAsState()

    // ── 派生状态 ──────────────────────────────────────────
    val effectiveDate = if (date > 0) date else DateUtils.getTodayStart()
    val totalSeconds = todayRecordSeconds + if (isRunning) elapsedSeconds else 0L
    val targetMinutes = task?.targetDurationMinutes

    // 当前所选日期是否为允许计时的日槽（基于 dayStartOffset 动态计算）
    val dayStartOffset by appDataStore.dayStartOffsetMinutes.collectAsState(initial = 0)
    val canTime = remember(dayStartOffset, effectiveDate) {
        val activeDay = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), dayStartOffset)
        effectiveDate == activeDay
    }

    // ── 本地 UI 状态 ──────────────────────────────────────
    var selectedSegment by remember { mutableStateOf(0) } // 0=计时, 1=笔记
    var showTimePicker by remember { mutableStateOf(false) }

    // 每次页面可见时刷新计时数据（处理从主页计时后返回的情况）
    LaunchedEffect(Unit) {
        viewModel.loadTodayRecord()
        viewModel.loadComparisonData()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. 顶部导航条
            TaskDetailHeader(
                title = task?.title ?: "",
                tagName = tagName,
                onBack = { com.chronotask.pages.taskdetail.api.TaskDetailArgument(taskId, effectiveDate).popBackStack() }
            )

            // 2. 分段切换（计时 / 笔记）
            TaskSegmentSwitch(
                selectedSegment = selectedSegment,
                onSegmentChange = { selectedSegment = it }
            )

            // 3. 分段内容
            AnimatedContent(
                targetState = selectedSegment,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "segment_content"
            ) { segment ->
                when (segment) {
                    0 -> TimerContent(
                        isRunning = isRunning,
                        elapsedSeconds = elapsedSeconds,
                        totalSeconds = totalSeconds,
                        targetMinutes = targetMinutes,
                        progress = targetMinutes?.let { tm ->
                            if (tm > 0) (totalSeconds.toFloat() / (tm * 60f)).coerceIn(0f, 1f) else null
                        },
                        animatedHourProgress = (totalSeconds % 3600).toFloat() / 3600f,
                        comparisonData = comparisonData,
                        onShowTimePicker = { showTimePicker = it },
                        viewModel = viewModel,
                        canTime = canTime
                    )
                    1 -> NotesContent(
                        entity = currentNote,
                        onSave = { title, content ->
                            viewModel.saveTaskNote(title, content)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 4. 覆盖层：时间选择器弹窗
        if (showTimePicker) {
            VerticalTimePickerDialog(
                initialHours = targetMinutes?.div(60) ?: 0,
                initialMinutes = targetMinutes?.rem(60) ?: 0,
                onConfirm = { hours, minutes ->
                    viewModel.updateTargetDuration(hours, minutes)
                    showTimePicker = false
                },
                onDismiss = { showTimePicker = false }
            )
        }
    }
}

/**
 * TaskDetailHeader - 顶部导航条
 *
 * 布局：[返回键] 任务标题 [标签名]
 *
 * @param title   任务标题
 * @param tagName 标签名称（为空则不显示）
 * @param onBack  返回回调
 */
@Composable
private fun TaskDetailHeader(
    title: String,
    tagName: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        if (tagName.isNotEmpty()) {
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = tagName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * TaskSegmentSwitch - 分段切换器（计时 / 笔记）
 *
 * @param selectedSegment 当前选中分段（0=计时, 1=笔记）
 * @param onSegmentChange 分段切换回调
 */
@Composable
private fun TaskSegmentSwitch(
    selectedSegment: Int,
    onSegmentChange: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp)
    ) {
        SegmentedButton(
            selected = selectedSegment == 0,
            onClick = { onSegmentChange(0) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text(stringResource(R.string.timer))
        }
        SegmentedButton(
            selected = selectedSegment == 1,
            onClick = { onSegmentChange(1) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text(stringResource(R.string.note_label))
        }
    }
}