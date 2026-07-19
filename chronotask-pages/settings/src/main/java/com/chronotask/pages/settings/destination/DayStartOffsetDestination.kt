package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.DayStartOffsetArgument
import com.chronotask.pages.settings.ui.DayStartOffsetScreen

@AppNavDestination(NavigationTable.NAV_DAY_START_OFFSET)
class DayStartOffsetDestination : AppNavEntry<DayStartOffsetArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: DayStartOffsetArgument) {
        DayStartOffsetScreen(onBack = { DayStartOffsetArgument.popBackStack() })
    }
}
