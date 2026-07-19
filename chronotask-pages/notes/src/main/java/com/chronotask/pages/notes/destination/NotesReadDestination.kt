package com.chronotask.pages.notes.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.notes.api.NotesReadArgument
import com.chronotask.pages.notes.ui.NotesReadScreen

/**
 * NotesReadDestination - 笔记阅读页导航入口
 *
 * 核心职责：根据笔记 ID 展示只读笔记详情，支持切换到编辑模式。
 */
@AppNavDestination(NavigationTable.NAV_NOTES_READ)
class NotesReadDestination : AppNavEntry<NotesReadArgument>() {
    @Composable
    override fun Content(argument: NotesReadArgument) {
        NotesReadScreen(
            noteId = argument.noteId,
            onNavigateBack = { argument.popBackStack() }
        )
    }
}