package com.chronotask.pages.notes.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.ui.R
import com.chronotask.components.ui.calendar.CalendarPickerDialog
import com.chronotask.pages.notes.api.NotesCreateArgument
import com.chronotask.pages.notes.api.NotesReadArgument
import com.chronotask.pages.notes.viewmodel.NotesViewModel
import com.chronotask.components.ui.theme.LocaleManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * NotesScreen - 笔记列表页
 *
 * 核心职责：展示笔记列表，支持标题搜索 + 日历日期筛选。
 * 布局结构（从上到下）：
 *   1. 日历筛选行（NotesCalendarRow）
 *   2. 搜索框（NotesSearchBar）
 *   3. 笔记列表（LazyColumn）/ 空状态视图
 *   4. 悬浮按钮（FAB 新建笔记）
 *
 * 状态管理：所有状态上移至 NotesViewModel，本函数仅负责 UI 渲染。
 * 弹窗状态（删除确认、日历选择）为纯 UI 状态，使用本地 mutableStateOf。
 */
@Composable
fun NotesScreen() {
    val viewModel: NotesViewModel = viewModel { NotesViewModel() }

    // ── 从 ViewModel 收集状态 ─────────────────────────────────
    val groupedNotes by viewModel.groupedNotes.collectAsState()
    val searchTitle by viewModel.searchTitle.collectAsState()
    val searchDate by viewModel.searchDate.collectAsState()
    val hasActiveFilter by viewModel.hasActiveFilter.collectAsState()

    // ── 本地 UI 状态（弹窗控制） ──────────────────────────────
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))

            // 日历筛选行
            NotesCalendarRow(
                searchDate = searchDate,
                searchTitle = searchTitle,
                onCalendarClick = { showCalendarDialog = true },
                onClearFilter = viewModel::clearAllFilters
            )

            // 搜索框（标题搜索，点击搜索按钮才触发筛选）
            NotesSearchBar(
                query = searchTitle,
                onQueryChange = viewModel::updateSearchTitle,
                onSearchCommit = viewModel::commitSearch,
                onClear = {
                    viewModel.updateSearchTitle("")
                    viewModel.clearAllFilters()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 笔记列表 / 空状态
            NotesListContent(
                groupedNotes = groupedNotes,
                hasActiveFilter = hasActiveFilter,
                onDelete = { entity ->
                    pendingDeleteId = entity.id
                    showDeleteDialog = true
                },
                onEdit = { entity ->
                    NotesReadArgument(entity.id).navigate()
                }
            )
        }

        // FAB: 新建笔记
        FloatingActionButton(
            onClick = { NotesCreateArgument.navigate() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
        }
    }

    // 日历选择弹窗
    if (showCalendarDialog) {
        CalendarPickerDialog(
            initialDate = searchDate ?: System.currentTimeMillis(),
            onDateSelected = { selectedDate: Long ->
                viewModel.updateSearchDate(selectedDate)
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    // 删除确认弹窗
    NotesDeleteDialog(
        showDialog = showDeleteDialog,
        onConfirm = {
            pendingDeleteId?.let { viewModel.deleteNote(it) }
            showDeleteDialog = false
            pendingDeleteId = null
        },
        onDismiss = {
            showDeleteDialog = false
            pendingDeleteId = null
        }
    )
}

/**
 * NotesListContent - 笔记列表内容区域
 *
 * 根据数据状态展示：
 * - 无数据且无筛选 → 空状态视图
 * - 无数据且有筛选 → 搜索无结果视图
 * - 有数据 → LazyColumn 列表
 *
 * @param groupedNotes   按日期分组的笔记列表
 * @param hasActiveFilter 是否处于筛选状态
 * @param onDelete       删除回调，参数为对应笔记实体
 * @param onEdit         编辑/阅读回调，参数为对应笔记实体
 */
@Composable
private fun NotesListContent(
    groupedNotes: List<com.chronotask.pages.notes.data.NotesSection>,
    hasActiveFilter: Boolean,
    onDelete: (NoteHistoryEntity) -> Unit,
    onEdit: (NoteHistoryEntity) -> Unit
) {
    if (groupedNotes.isEmpty() && !hasActiveFilter) {
        EmptyNotesView()
    } else if (groupedNotes.isEmpty() && hasActiveFilter) {
        EmptySearchResult()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedNotes.forEach { section ->
                item(key = "header_${section.dateLabel}") {
                    NotesDateHeader(label = section.dateLabel)
                }
                items(section.items, key = { it.id }) { entity ->
                    NotesListItem(
                        entity = entity,
                        onDelete = { onDelete(entity) },
                        onEdit = { onEdit(entity) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

/**
 * NotesDeleteDialog - 删除确认弹窗
 *
 * @param showDialog 是否显示弹窗
 * @param onConfirm  确认删除回调
 * @param onDismiss  取消/关闭回调
 */
@Composable
private fun NotesDeleteDialog(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_note_msg)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * NotesCalendarRow - 日历筛选行
 *
 * 布局：[日历图标] (日期文字/搜索状态) ... [返回键]
 * 返回键仅在 hasActiveFilter 时显示。
 *
 * @param searchDate  选中的日期毫秒时间戳（null 表示未选）
 * @param searchTitle 标题搜索关键词
 * @param onCalendarClick 点击日历图标回调（打开日历弹窗）
 * @param onClearFilter   清除所有筛选条件回调
 */
@Composable
private fun NotesCalendarRow(
    searchDate: Long?,
    searchTitle: String,
    onCalendarClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    val currentLocale by LocaleManager.currentLocale.collectAsState()
    val dateFormatPattern = stringResource(R.string.date_format_full)
    val dateFormat = remember(currentLocale, dateFormatPattern) {
        SimpleDateFormat(dateFormatPattern, currentLocale)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：日历图标
        IconButton(onClick = onCalendarClick, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = stringResource(R.string.select_date),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        // 中间：筛选状态文字
        if (searchDate != null) {
            Text(
                text = dateFormat.format(Date(searchDate)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (searchTitle.isNotBlank()) {
            Text(
                text = stringResource(R.string.title_colon, searchTitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧：返回键（仅在有筛选时显示）
        if (searchDate != null || searchTitle.isNotBlank()) {
            IconButton(onClick = onClearFilter, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.return_all),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * NotesSearchBar - 标题搜索框
 *
 * 支持输入中实时更新 ViewModel 关键词，点击搜索按钮提交筛选。
 * 输入框聚焦时显示 tonalElevation 提升效果。
 *
 * @param query         当前搜索关键词
 * @param onQueryChange 输入变化回调
 * @param onSearchCommit 点击搜索按钮回调
 * @param onClear       清除搜索回调
 */
@Composable
private fun NotesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchCommit: () -> Unit,
    onClear: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isFocused) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).onFocusChanged { isFocused = it.isFocused },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchCommit() }),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.search_title_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
            // 搜索按钮：点击触发搜索
            IconButton(onClick = onSearchCommit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * NotesListItem - 笔记列表项
 *
 * 布局：标题(空则无) | 正文首行(10字截断) | 任务来源(空则无) | 时间 | 删除
 * 点击卡片进入阅读页，点击删除图标触发删除确认。
 *
 * @param entity  笔记数据实体
 * @param onDelete 删除回调
 * @param onEdit   编辑/阅读回调
 */
@Composable
private fun NotesListItem(
    entity: NoteHistoryEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val timeStr = remember(entity.sessionStartTime) {
        String.format("%02d:%02d", Date(entity.sessionStartTime).hours, Date(entity.sessionStartTime).minutes)
    }
    val firstLine = entity.note.lineSequence().firstOrNull()?.take(10) ?: ""

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 标题（无则空着）
                if (entity.title.isNotBlank()) {
                    Text(
                        text = entity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // 正文首行（超 10 字截断）
                if (firstLine.isNotEmpty()) {
                    Text(
                        text = if (entity.note.lineSequence().firstOrNull()?.length ?: 0 > 10) "$firstLine..." else firstLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // 来源任务标签（无则空着）
                if (entity.sourceTaskTitle.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(start = 6.dp, top = 3.dp, end = 8.dp, bottom = 3.dp)
                    ) {
                        Text(
                            text = entity.sourceTaskTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * NotesDateHeader - 日期分组标题
 *
 * @param label 分组标签文本（如 "今天"、"昨天"、"7月15日"）
 */
@Composable
private fun NotesDateHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/**
 * EmptyNotesView - 空状态（无任何笔记时展示）
 */
@Composable
private fun EmptyNotesView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDCDD", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.no_notes_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * EmptySearchResult - 搜索无结果状态（有筛选条件但无匹配数据时展示）
 */
@Composable
private fun EmptySearchResult() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("\uD83D\uDD0D", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}