package com.chronotask.pages.settings.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.QuickImportTask
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator
import com.chronotask.pages.create.api.CreateArgument
import kotlinx.coroutines.launch


/**
 * QuickImportTaskItem - 一键导入模板卡片
 *
 * 临摹 TaskItem 布局的只读模板卡片。
 * 保留：标题 / 标签 / 总时间（位置、字号、尺寸与 TaskItem 一致）。
 * 移除：进度条 / 计时功能 / Play-Stop-Complete 按钮。
 *
 * @param task 导入模板数据
 * @param index 条目索引
 * @param isEditMode 是否处于编辑模式（控制删除按钮显示）
 * @param tags 标签列表（用于显示标签名称）
 * @param onDelete 删除回调
 */

@Composable
fun QuickImportTaskItem(
    task: QuickImportTask,
    index: Int,
    isEditMode: Boolean,
    tags: List<com.chronotask.components.database.entity.TagEntity>,
    onDelete: (Int) -> Unit,
    onEdit: (Int) -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左：标题 + 标签
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifBlank { stringResource(R.string.unnamed_task) },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickableNoIndicator { onEdit(index) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tags.firstOrNull { it.id == task.tagId }?.name ?: stringResource(R.string.uncategorized),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 右：总时间
            Column(horizontalAlignment = Alignment.End) {
                val mins = task.targetMinutes
                if (mins != null && mins > 0) {
                    val h = mins / 60
                    val m = mins % 60
                    val timeText = "" + h + "h " + m + "m"
                    val displayText = when {
                        h > 0 && m > 0 -> "" + h + "h " + m + "m"
                        h > 0 -> "" + h + "h"
                        else -> "" + m + "m"
                    }
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.target),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "∞",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.unlimited),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isEditMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 编辑按钮
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickableNoIndicator { /* 编辑功能预留 */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 删除按钮
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickableNoIndicator { onDelete(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            }
    }
}


/**
 * QuickImportEditDialog - 一键导入条目编辑弹窗
 *
 * 允许用户修改任务标题、目标时长（分钟）和关联标签。
 * 确认时调用 onConfirm 回传更新后的 QuickImportTask。
 *
 * @param task 当前编辑的任务条目
 * @param tags 所有可用标签列表
 * @param onConfirm 确认回调（回传更新后的 QuickImportTask）
 * @param onDismiss 取消/关闭弹窗回调
 */
@Composable
fun QuickImportEditDialog(
    task: QuickImportTask,
    tags: List<com.chronotask.components.database.entity.TagEntity>,
    onConfirm: (QuickImportTask) -> Unit,
    onDismiss: () -> Unit
) {
    var title by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(task.title) }
    var minutesText by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(task.targetMinutes?.toString() ?: "")
    }
    var selectedTagId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(task.tagId) }

    // 提前计算缺省标题（避免 onClick λ 调用 @Composable 函数）
    val fallbackTitle = title.ifBlank { stringResource(R.string.unnamed_task) }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.edit_task),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.target_duration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 标签选择
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val isSelected = selectedTagId == tag.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickableNoIndicator { selectedTagId = if (isSelected) null else tag.id }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    val mins = minutesText.trim().toIntOrNull()
                    onConfirm(
                        QuickImportTask(
                            title = fallbackTitle,
                            targetMinutes = mins,
                            tagId = selectedTagId
                        )
                    )
                }) {
                    Text(
                        text = stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// QuickImportEditor — 一键-import 编辑器全屏页面

/**
 * @param onBack 返回设置页回调
 */
@Composable
/**
 * QuickImportEditor - 一键导入编辑器全屏页面
 *
 * 展示一键导入模板列表，支持编辑模式（删除条目）和 FAB 添加新条目。
 *
 * @param onBack 返回设置页回调
 */
fun QuickImportEditor(
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 加载标签列表（用于显示任务对应的标签名称）
    val db = com.chronotask.components.database.AppDatabase.getDatabase(com.chronotask.components.common.appApplication)
    val tags by db.tagDao().getAllTags().collectAsState(initial = emptyList())

    // 编辑模式状态：控制删除按钮显示
    var isEditMode by remember { mutableStateOf(false) }

    // 编辑弹窗状态
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Flow 订阅 DataStore，Create 页面保存后自动刷新
    val tasks by QuickImportManager.tasks.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.current_tasks),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { isEditMode = !isEditMode }) {
                    Text(
                        text = stringResource(R.string.edit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 任务列表
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_quick_import),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(tasks) { index, task ->
                        QuickImportTaskItem(
                            task = task,
                            index = index,
                            isEditMode = isEditMode,
                            tags = tags,
                            onDelete = { idx ->
                                scope.launch { QuickImportManager.removeTask(idx) }
                            },
                            onEdit = { idx -> editingIndex = idx }
                        )
                    }
                }
            }
        }

        // 编辑弹窗
        if (editingIndex != null) {
            val idx = editingIndex!!
            val task = tasks[idx]
            QuickImportEditDialog(
                task = task,
                tags = tags,
                onConfirm = { updated ->
                    scope.launch { QuickImportManager.updateTask(idx, updated) }
                    editingIndex = null
                },
                onDismiss = { editingIndex = null }
            )
        }

        // 底部 FAB → Create 页面 QuickImport 模式
        FloatingActionButton(
            onClick = { CreateArgument.navigateForQuickImport() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.new_task_cd))
        }
    }
}
