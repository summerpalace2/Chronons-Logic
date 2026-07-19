package com.chronotask.pages.taskdetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.chronotask.components.database.entity.NoteHistoryEntity
import com.chronotask.components.ui.R

/**
 * NotesContent - 任务详情页笔记区
 *
 * 核心职责：展示当前任务的笔记，支持 READ/EDIT 双模式切换。
 *          一个任务对应一个笔记，无历史列表。
 *          READ 模式：展示笔记标题+正文，点击编辑按钮切换到 EDIT。
 *          EDIT 模式：BasicTextField 编辑，支持保存/取消。
 *
 * @param entity 当前任务的笔记实体（null = 无笔记）
 * @param onSave 保存回调 (title, content)
 */
@Composable
internal fun NotesContent(
    entity: NoteHistoryEntity?,
    onSave: (title: String, content: String) -> Unit
) {
    // editorMode: false = READ, true = EDIT
    var editorMode by remember { mutableStateOf(false) }
    val readOnly = !editorMode

    var titleFieldValue by remember(entity?.id) {
        val t = entity?.title ?: ""
        mutableStateOf(TextFieldValue(t, TextRange(t.length)))
    }
    var bodyFieldValue by remember(entity?.id) {
        val b = entity?.note ?: ""
        mutableStateOf(TextFieldValue(b, TextRange(b.length)))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ── 顶栏：模式切换 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.note_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 20.dp).size(18.dp)
            )
            if (readOnly) {
                IconButton(onClick = { editorMode = true }, modifier = Modifier.padding(end = 10.dp).size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.padding(end = 10.dp).size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(onClick = {
                    onSave(titleFieldValue.text, bodyFieldValue.text)
                    editorMode = false
                }) {
                    Text(stringResource(R.string.note_editor_save))
                }
                TextButton(onClick = {
                    val t = entity?.title ?: ""
                    val b = entity?.note ?: ""
                    titleFieldValue = TextFieldValue(t, TextRange(t.length))
                    bodyFieldValue = TextFieldValue(b, TextRange(b.length))
                    editorMode = false
                }) {
                    Text(stringResource(R.string.note_editor_cancel))
                }
            }
        }

        // ── 内容区 ──
        Spacer(modifier = Modifier.height(8.dp))

        if (readOnly) {
            ReadOnlyNoteView(
                title = titleFieldValue.text,
                body = bodyFieldValue.text
            )
        } else {
            EditNoteView(
                titleFieldValue = titleFieldValue,
                onTitleChange = { titleFieldValue = it },
                bodyFieldValue = bodyFieldValue,
                onBodyChange = { bodyFieldValue = it }
            )
        }
    }
}

/**
 * ReadOnlyNoteView - 只读笔记展示
 *
 * 核心职责：展示笔记标题（大字居中）+ 正文。
 */
@Composable
private fun ReadOnlyNoteView(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (body.isNotEmpty()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            if (title.isEmpty() && body.isEmpty()) {
                Text(
                    text = stringResource(R.string.note_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/**
 * EditNoteView - 编辑笔记
 *
 * 核心职责：双 BasicTextField 编辑（标题+正文）。
 */
@Composable
private fun EditNoteView(
    titleFieldValue: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    bodyFieldValue: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题输入框
            BasicTextField(
                value = titleFieldValue,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
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
            Spacer(modifier = Modifier.height(12.dp))
            // 正文输入框
            BasicTextField(
                value = bodyFieldValue,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (bodyFieldValue.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.note_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
