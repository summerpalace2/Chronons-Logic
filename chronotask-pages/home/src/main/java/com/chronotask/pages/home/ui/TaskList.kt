package com.chronotask.pages.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chronotask.components.ui.R
import com.chronotask.pages.home.data.TaskItemState

// TaskList
// 角色：任务列表容器，提供：
//   ① 空列表占位文案（无任务时渲染提示）
//   ② 有任务时纵向排列 TaskItem + 分隔线
//
// 设计选择：
//   • 列表本身不关心删除/完成等逻辑，职责单一：只负责「排列」
//   • 所有事件处理通过 lambda 交还给父组件 → 保持可复用性

/**
 * 任务列表容器
 *
 * @param items         任务条目列表
 * @param isToday       是否选中今天（控制计时按钮的启用态）
 * @param isEditMode    编辑模式（控制 TaskItem 是否显示删除图标）
 * @param onStart       开始计时回调
 * @param onStop        停止计时回调
 * @param onComplete    切换完成状态回调
 * @param onDelete      删除任务回调
 * @param onTaskClick   任务卡片点击回调（跳转到详情页）
 * @param onStartDenied 非今天日期拒绝计时的兜底回调（弹 Toast）
 */
@Composable
fun TaskList(
    items: List<TaskItemState>,
    isToday: Boolean,
    isEditMode: Boolean = false,
    onStart: (Long) -> Unit,
    onStop: () -> Unit,
    onComplete: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onTaskClick: (Long) -> Unit,
    onStartDenied: () -> Unit = {}
) {
    if (items.isEmpty()) {
        // ─── 空状态 ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_active_tasks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        // ─── 任务列表 + 分隔线 ───
        Column {
            items.forEach { item ->
                TaskItem(
                    item = item,
                    isToday = isToday,
                    isEditMode = isEditMode,
                    onStart = { onStart(item.task.id) },
                    onStop = onStop,
                    onComplete = { onComplete(item.task.id) },
                    onDelete = { onDelete(item.task.id) },
                    onClick = { onTaskClick(item.task.id) },
                    onStartDenied = onStartDenied
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}