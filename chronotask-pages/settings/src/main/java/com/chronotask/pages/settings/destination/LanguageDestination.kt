package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.LanguageArgument
import com.chronotask.pages.settings.ui.LanguageSettingsScreen

@AppNavDestination(NavigationTable.NAV_LANGUAGE)
class LanguageDestination : AppNavEntry<LanguageArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: LanguageArgument) {
        LanguageSettingsScreen()
    }
}
