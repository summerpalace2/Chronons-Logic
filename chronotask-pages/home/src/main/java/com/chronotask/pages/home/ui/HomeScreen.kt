package com.chronotask.pages.home.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.appDataStore
import com.chronotask.components.ui.R
import com.chronotask.components.ui.calendar.CalendarPickerDialog
import com.chronotask.components.ui.calendar.CalendarStrip
import com.chronotask.pages.home.viewmodel.HomeViewModel

/**
 * 首页 — 顶层脚手架
 *
 * 重组优化：
 * - selectedDate 在顶层 collectAsState，传递给子组件
 * - 各子组件各自 collectAsState 订阅所需状态，单一变化不会导致整个屏幕重绘
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {

    // 顶层收集 selectedDate，传递给需要的子组件
    val selectedDate by viewModel.selectedDate.collectAsState()
    val activeDay = viewModel.getActiveDay()

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Long?>(null) }


    // 同步工作日配置 → 休息日状态
    LaunchedEffect(selectedDate) {
        viewModel.syncWorkdayRestStatus(selectedDate)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            CalendarStrip(
                selectedDate = selectedDate,
                onDateSelected = { viewModel.selectDate(it) },
                onPrevWeek = { viewModel.moveDate(-1) },
                onNextWeek = { viewModel.moveDate(1) },
                onMonthClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            HomeTopSection(viewModel = viewModel, selectedDate = selectedDate)

            Spacer(modifier = Modifier.height(16.dp))

            TaskListSection(
                viewModel = viewModel,
                selectedDate = selectedDate,
                onDelete = { taskId ->
                    taskToDelete = taskId
                    showDeleteDialog = true
                }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }

        TaskListFab(viewModel = viewModel, modifier = Modifier.align(Alignment.BottomEnd))

    }

    if (showDatePicker) {
        CalendarPickerDialog(
            initialDate = selectedDate,
            onDateSelected = { viewModel.selectDate(it) },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showDeleteDialog && taskToDelete != null) {
        DeleteConfirmDialog(
            onConfirm = {
                taskToDelete?.let { viewModel.deleteTask(it) }
                showDeleteDialog = false
                taskToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                taskToDelete = null
            }
        )
    }
}

/**
 * FAB 悬浮按钮
 */
@Composable
private fun TaskListFab(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val isEditMode by viewModel.isEditMode.collectAsState()
    if (!isEditMode) {
        FloatingActionButton(
            onClick = { com.chronotask.pages.create.api.CreateArgument.navigateForCreate() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.padding(end = 16.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.new_task_cd)
            )
        }
    }
}

/**
 * 顶部区域 — 今日总用时 + 一键导入 + 休息切换
 *
 * @param viewModel 页面 ViewModel
 * @param selectedDate 当前选中的日期（由父组件传入，避免重复 collectAsState）
 */
@SuppressLint("UnrememberedMutableState")
@Composable
private fun HomeTopSection(viewModel: HomeViewModel, selectedDate: Long) {
    val todayTotal by viewModel.combinedTotal.collectAsState()
    val isTodayRest by viewModel.isTodayRest.collectAsState()
    val isToday = selectedDate == DateUtils.getTodayStart()

    val quickImportTasks by QuickImportManager.tasks.collectAsState(initial = emptyList())
    val quickImportEnabled by QuickImportManager.isEnabled.collectAsState(initial = false)
    val taskItems by viewModel.taskItems.collectAsState()
    val dayImported by viewModel.dayImportStatus.collectAsState()

    /**
     * 任务列表中实时检测到的 quick import 标题集合
     *  • 使用 derivedStateOf：随 taskItems 同步计算（零延迟，无闪跳）
     *  • 删除任务后集合自动收缩 → 按钮恢复可点
     */
    val detectedImports by derivedStateOf {
        val quickTitles = quickImportTasks.map { it.title }.toSet()
        if (quickTitles.isEmpty()) return@derivedStateOf emptySet<String>()
        val currentTitles = taskItems.map { it.task.title }.toSet()

        quickTitles.intersect(currentTitles)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = stringResource(R.string.today_total_time),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatDuration(todayTotal),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isTodayRest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 一键导入按钮 — 非当天也可使用
            if (quickImportEnabled) {
                val quickTitles = quickImportTasks.map { it.title }.toSet()
                // 已导入 = VM 状态 ∪ 标题检测
                val effectiveImports = detectedImports + if (selectedDate in dayImported) quickTitles else emptySet()
                val allImported = quickTitles.isNotEmpty() && effectiveImports.containsAll(quickTitles)

                TextButton(
                    onClick = {
                        quickImportTasks.forEach { qiTask ->
                            viewModel.importQuickTask(qiTask, selectedDate)
                        }
                    },
                    enabled = !allImported
                ) {
                    Text(
                        text = if (allImported) stringResource(R.string.imported) else stringResource(R.string.quick_import),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (allImported) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            TodayRestToggle(
                isRest = isTodayRest,
                onToggle = { viewModel.toggleTodayRest() }
            )
        }
    }
}


@Composable
private fun TaskListSection(
    viewModel: HomeViewModel,
    selectedDate: Long,
    onDelete: (Long) -> Unit
) {
    val taskItems by viewModel.taskItems.collectAsState()
    val activeDay = viewModel.getActiveDay()
    val isToday = selectedDate == activeDay
    val isEditMode by viewModel.isEditMode.collectAsState()
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.current_tasks),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(
            onClick = { viewModel.toggleEditMode() }
        ) {
            Text(
                text = if (isEditMode) stringResource(R.string.done) else stringResource(R.string.edit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    TaskList(
        items = taskItems,
        isToday = isToday,
        isEditMode = isEditMode,
        onStart = { viewModel.startTask(it) },
        onStop = { viewModel.stopTask() },
        onComplete = { viewModel.toggleTaskCompletion(it) },
        onDelete = onDelete,
        onTaskClick = { taskId ->
            if (!isEditMode) {
                com.chronotask.pages.taskdetail.api.TaskDetailArgument(taskId, selectedDate).navigate()
            }
        },
        onStartDenied = {
            Toast.makeText(context, context.getString(R.string.not_today_no_timer), Toast.LENGTH_SHORT).show()
        }
    )
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.confirm_delete),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.delete_task_msg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}



