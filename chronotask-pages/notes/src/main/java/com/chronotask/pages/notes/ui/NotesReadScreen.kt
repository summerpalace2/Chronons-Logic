package com.chronotask.pages.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.appIoScope
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.database.repository.NoteHistoryRepository
import com.chronotask.components.ui.R
import kotlinx.coroutines.launch
import com.chronotask.pages.notes.data.NoteEditorMode

/**
 * NotesReadScreen - 笔记阅读页
 *
 * 核心职责：展示笔记详情，支持切换到编辑模式。
 * 使用 appIoScope 加载和保存数据，退出页面后仍可完成操作。
 *
 * @param noteId       笔记 ID
 * @param onNavigateBack 返回上一页回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesReadScreen(
    noteId: Long,
    onNavigateBack: () -> Unit
) {
    var entity by remember { mutableStateOf<NoteHistoryEntity?>(null) }
    var editMode by remember { mutableStateOf(false) }

    // 加载笔记数据（使用 appIoScope 保活）
    LaunchedEffect(noteId) {
        appIoScope.launch {
            entity = NoteHistoryRepository.getNoteById(noteId)
        }
    }

    // 切换到编辑模式
    if (editMode && entity != null) {
        NoteEditorOverlay(
            editingEntity = entity,
            mode = NoteEditorMode.EDIT,
            onDismiss = { editMode = false },
            onSave = { text, title ->
                appIoScope.launch {
                    entity?.let { NoteHistoryRepository.updateNoteAndTitle(it.id, text, title) }
                    // 乐观刷新：立即更新本地 entity，无需等待 Flow 回灌
                    entity = entity?.copy(note = text, title = title)
                    editMode = false
                }
            },
            onAutoSave = { _, _ -> }
        )
        return
    }

    // 只读展示
    entity?.let { note ->
        ReadingContent(
            entity = note,
            onEdit = { editMode = true },
            onBack = onNavigateBack
        )
    }
}

/**
 * ReadingContent - 只读笔记内容展示
 *
 * @param entity 笔记数据实体
 * @param onEdit 切换到编辑模式回调
 * @param onBack 返回上一页回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingContent(
    entity: NoteHistoryEntity,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val title = entity.title
    val body = entity.note

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = title.ifBlank {
                            entity.note.lineSequence().firstOrNull()?.trim()?.take(50) ?: ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            } else if (title.isBlank()) {
                Text(
                    text = stringResource(R.string.note_editor_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}