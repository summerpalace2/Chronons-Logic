package com.chronotask.pages.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.appIoScope
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.ui.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.chronotask.pages.notes.data.NoteEditorMode

/**
 * NoteEditorOverlay - 笔记编辑器覆盖层
 *
 * 支持三种模式：
 * - CREATE：新建笔记，自动聚焦标题输入框
 * - EDIT：编辑已有笔记
 * - READ：只读模式（隐藏保存按钮）
 *
 * 自动保存：用户停止输入 3 秒后触发 onAutoSave，使用 appIoScope 保活。
 *
 * @param editingEntity 待编辑的笔记实体（CREATE 模式传 null）
 * @param mode          编辑器模式（CREATE / EDIT / READ）
 * @param onDismiss     关闭编辑器回调
 * @param onSave        保存回调（body, title）
 * @param onAutoSave    自动保存回调（body, title），默认空实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteEditorOverlay(
    editingEntity: NoteHistoryEntity?,
    mode: NoteEditorMode,
    onDismiss: () -> Unit,
    onSave: (text: String, title: String) -> Unit,
    onAutoSave: suspend (text: String, title: String) -> Unit = { _, _ -> }
) {
    val readOnly = mode == NoteEditorMode.READ
    val isCreate = mode == NoteEditorMode.CREATE

    //输入框状态
    var titleFieldValue by remember(editingEntity?.id) {
        val t = when (mode) {
            NoteEditorMode.CREATE -> ""
            else -> editingEntity?.title ?: ""
        }
        mutableStateOf(TextFieldValue(t, TextRange(t.length)))
    }

    var bodyFieldValue by remember(editingEntity?.id) {
        val b = when (mode) {
            NoteEditorMode.CREATE -> ""
            else -> editingEntity?.note ?: ""
        }
        mutableStateOf(TextFieldValue(b, TextRange(b.length)))
    }

    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    var autoSaveJob by remember { mutableStateOf<Job?>(null) }

    // ── 自动保存（使用 appIoScope 保活，退出页面后仍可完成） ──
    LaunchedEffect(titleFieldValue.text, bodyFieldValue.text) {
        if (readOnly) return@LaunchedEffect
        autoSaveJob?.cancel()
        autoSaveJob = appIoScope.launch {
            delay(3000)
            val title = titleFieldValue.text
            val body = bodyFieldValue.text
            if (title.isNotBlank() || body.isNotBlank()) {
                onAutoSave(body, title.trim())
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = when (mode) {
                            NoteEditorMode.CREATE -> stringResource(R.string.add_note)
                            NoteEditorMode.READ -> stringResource(R.string.view_note)
                            NoteEditorMode.EDIT -> stringResource(R.string.edit)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    if (!readOnly) {
                        TextButton(onClick = {
                            val title = titleFieldValue.text.trim()
                            val body = bodyFieldValue.text.trim()
                            onSave(body, title)
                        }) {
                            Text(text = stringResource(R.string.note_editor_save))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // 标题输入框：单行，decorationBox 仅展示 hint 或光标
            BasicTextField(
                value = titleFieldValue,
                onValueChange = { titleFieldValue = it },
                readOnly = readOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
                    .focusRequester(titleFocusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (titleFieldValue.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_title_hint),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )

            // 正文输入框：与标题保持间距
            BasicTextField(
                value = bodyFieldValue,
                onValueChange = { bodyFieldValue = it },
                readOnly = readOnly,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp)
                    .focusRequester(bodyFocusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (bodyFieldValue.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_editor_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )

            // CREATE 模式自动聚焦到标题
            LaunchedEffect(mode) {
                if (isCreate) {
                    delay(300)
                    try { titleFocusRequester.requestFocus() } catch (_: Exception) {}
                }
            }
        }
    }
}