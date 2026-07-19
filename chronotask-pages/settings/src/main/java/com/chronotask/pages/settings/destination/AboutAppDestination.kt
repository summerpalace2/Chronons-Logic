package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.AboutAppArgument
import com.chronotask.pages.settings.ui.AboutAppScreen

@AppNavDestination(NavigationTable.NAV_ABOUT_APP)
class AboutAppDestination : AppNavEntry<AboutAppArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: AboutAppArgument) {
        AboutAppScreen()
    }
}
