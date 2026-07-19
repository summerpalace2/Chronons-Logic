package com.chronotask.pages.stats.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.stats.api.StatsArgument
import com.chronotask.pages.stats.ui.StatsScreen

/**
 * StatsDestination - 统计页导航入口
 *
 * 核心职责：导航框架自动发现此入口，根据 NAV_STATS 路由展示统计页。
 */
@AppNavDestination(NavigationTable.NAV_STATS)
class StatsDestination : AppNavEntry<StatsArgument>() {
    @Composable
    override fun Content(argument: StatsArgument) {
        StatsScreen()
    }
}