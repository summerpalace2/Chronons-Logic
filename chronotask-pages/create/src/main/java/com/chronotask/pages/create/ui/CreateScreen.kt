package com.chronotask.pages.create.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.ui.R
import com.chronotask.components.ui.picker.VerticalTimePickerDialog
import com.chronotask.components.ui.compose.clickableNoIndicator
import com.chronotask.pages.create.api.CreateArgument
import com.chronotask.pages.create.api.CreateMode
import com.chronotask.pages.create.viewmodel.CreateViewModel

/**
 * 新建/编辑任务页 — 顶层脚手架
 *
 * 采用森林静谧主题，卡片式布局，分三个区块：
 * 1. 任务标题 — 大字号输入，聚焦时高亮
 * 2. 分类标签 — FlowRow 自适应换行，独立添加按钮
 * 3. 目标时长 — 圆形进度环 + 数字显示，支持无限时切换
 *
 * 重组优化：
 * - 各子组件（标题/标签/时长）各自通过 collectAsState 订阅所需状态
 * - 单一状态变化仅触发对应子组件的重组，不会导致整个屏幕重绘
 *
 * @param taskId 任务 ID，-1 表示新建模式，>0 表示编辑模式
 * @param mode 创建模式（Normal 标准 / QuickImport 快速导入）
 * @param viewModel 页面 ViewModel，默认通过 viewModel() 注入
 */
@Composable
fun CreateScreen(
    taskId: Long = -1,
    mode: CreateMode = CreateMode.Normal,
    viewModel: CreateViewModel = viewModel()
) {
    LaunchedEffect(mode) { viewModel.setMode(mode) }

    val isEditMode = taskId > 0
    LaunchedEffect(taskId) {
        if (isEditMode) viewModel.loadTask(taskId)
    }

    var showAddTagDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 顶部操作栏 ──
        CreateTopBar(
            isEditMode = isEditMode,
            mode = mode,
            onCancel = { CreateArgument().popBackStack() },
            onSave = {
                if (viewModel.save()) {
                    CreateArgument().popBackStack()
                }
            }
        )

        // ── 内容区（可滚动）──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TaskTitleCard(viewModel = viewModel)

            TagSelectionCard(
                viewModel = viewModel,
                onAddTag = { showAddTagDialog = true }
            )

            TargetDurationCard(
                viewModel = viewModel,
                onShowTimePicker = { showTimePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── 弹窗 ──
    if (showTimePicker) {
        val hours by viewModel.hours.collectAsState()
        val minutes by viewModel.minutes.collectAsState()
        VerticalTimePickerDialog(
            initialHours = hours,
            initialMinutes = minutes,
            onConfirm = { h, m ->
                viewModel.setTargetDuration(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showAddTagDialog) {
        val tags by viewModel.tags.collectAsState()
        AddTagDialog(
            existingTagNames = tags.map { it.name },
            onConfirm = { tagName ->
                viewModel.addTag(tagName)
                showAddTagDialog = false
            },
            onDismiss = { showAddTagDialog = false }
        )
    }
}

// 子组件

/**
 * 顶部操作栏 — 返回 / 标题 / 保存
 */
@Composable
private fun CreateTopBar(
    isEditMode: Boolean,
    mode: CreateMode,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val saveText = stringResource(R.string.save)
    val titleText = when {
        isEditMode -> stringResource(R.string.edit_task)
        mode == CreateMode.QuickImport -> stringResource(R.string.new_import_task)
        else -> stringResource(R.string.new_task)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Button(
            onClick = onSave,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = saveText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 任务标题卡片 — 输入框稍小，标签文字稍大
 */
@Composable
private fun TaskTitleCard(viewModel: CreateViewModel) {
    val title by viewModel.title.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.task_title_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            BasicTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = stringResource(R.string.task_title_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * 分类标签卡片 — 编辑按钮 + 小标签 + 文字简洁
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelectionCard(
    viewModel: CreateViewModel,
    onAddTag: () -> Unit
) {
    val tags by viewModel.tags.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }

    val tagIcons = remember {
        mapOf(
            "工作" to Icons.Default.Work,
            "学习" to Icons.AutoMirrored.Default.MenuBook,
            "健身" to Icons.Default.FitnessCenter
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.category),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isEditMode) {
                    TextButton(onClick = { isEditMode = false }) {
                        Text(
                            text = stringResource(R.string.done),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(
                        onClick = { isEditMode = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val isSelected = tag.id == selectedTagId

                    val bgColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                        label = "tagBg"
                    )
                    val borderColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        label = "tagBorder"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "tagText"
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp))
                            .clickableNoIndicator {
                                if (isEditMode) {
                                    viewModel.deleteTag(tag.id)
                                } else {
                                    viewModel.selectTag(tag.id)
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = bgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            if (isEditMode) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                if (!isEditMode) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                RoundedCornerShape(16.dp)
                            )
                            .clickableNoIndicator { onAddTag() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_tag),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 目标时长卡片
 */
@Composable
private fun TargetDurationCard(
    viewModel: CreateViewModel,
    onShowTimePicker: () -> Unit
) {
    val hours by viewModel.hours.collectAsState()
    val minutes by viewModel.minutes.collectAsState()
    val isUnlimited by viewModel.isUnlimited.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.target_duration),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

            val progress = if (isUnlimited) 0f else (hours * 60 + minutes) / 1440f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                        .graphicsLayer {
                            rotationZ = -90f
                            clip = true
                            shape = CircleShape
                        }
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isUnlimited) {
                        Text(
                            text = stringResource(R.string.unlimited),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${hours}h ${minutes}m",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUnlimited) {
                TextButton(onClick = onShowTimePicker) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.set_target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onShowTimePicker) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.modify),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "|",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    TextButton(onClick = { viewModel.toggleUnlimited() }) {
                        Text(
                            text = stringResource(R.string.set_unlimited),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
