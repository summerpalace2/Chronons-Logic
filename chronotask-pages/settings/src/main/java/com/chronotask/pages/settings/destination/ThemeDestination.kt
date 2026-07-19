package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.ThemeArgument
import com.chronotask.pages.settings.ui.ThemeSettingsScreen

@AppNavDestination(NavigationTable.NAV_THEME)
class ThemeDestination : AppNavEntry<ThemeArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: ThemeArgument) {
        ThemeSettingsScreen()
    }
}
