package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.AboutAuthorArgument
import com.chronotask.pages.settings.ui.AboutAuthorScreen

@AppNavDestination(NavigationTable.NAV_ABOUT_AUTHOR)
class AboutAuthorDestination : AppNavEntry<AboutAuthorArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: AboutAuthorArgument) {
        AboutAuthorScreen()
    }
}
