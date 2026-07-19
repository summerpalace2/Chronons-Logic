package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.FontArgument
import com.chronotask.pages.settings.ui.FontSettingsScreen

@AppNavDestination(NavigationTable.NAV_FONT)
class FontDestination : AppNavEntry<FontArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: FontArgument) {
        FontSettingsScreen()
    }
}
