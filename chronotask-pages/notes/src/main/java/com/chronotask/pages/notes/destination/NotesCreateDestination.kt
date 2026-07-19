package com.chronotask.pages.notes.destination

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.notes.api.NotesCreateArgument
import com.chronotask.pages.notes.viewmodel.NotesViewModel
import com.chronotask.pages.notes.ui.NoteEditorOverlay
import com.chronotask.pages.notes.data.NoteEditorMode

/**
 * NotesCreateDestination - 新建笔记导航入口
 *
 * 核心职责：以 nav3 导航方式展示新建笔记编辑器覆盖层。
 * 用户填写标题和正文后点击保存，调用 ViewModel.insertNote 写入数据库。
 */
@AppNavDestination(NavigationTable.NAV_NOTES_CREATE)
class NotesCreateDestination : AppNavEntry<NotesCreateArgument>() {
    @Composable
    override fun Content(argument: NotesCreateArgument) {
        val viewModel: NotesViewModel = viewModel { NotesViewModel() }

        NoteEditorOverlay(
            editingEntity = null,
            mode = NoteEditorMode.CREATE,
            onDismiss = { argument.popBackStack() },
            onSave = { text, title ->
                viewModel.insertNote(text, title)
                argument.popBackStack()
            },
            onAutoSave = { _, _ -> }
        )
    }
}