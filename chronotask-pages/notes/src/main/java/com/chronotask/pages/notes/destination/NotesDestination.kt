package com.chronotask.pages.notes.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.notes.api.NotesArgument
import com.chronotask.pages.notes.ui.NotesScreen

/**
 * NotesDestination - 笔记列表页导航入口
 *
 * 核心职责：导航框架自动发现此入口，根据 NAV_NOTES 路由展示笔记列表页。
 */
@AppNavDestination(NavigationTable.NAV_NOTES)
class NotesDestination : AppNavEntry<NotesArgument>() {
    @Composable
    override fun Content(argument: NotesArgument) {
        NotesScreen()
    }
}