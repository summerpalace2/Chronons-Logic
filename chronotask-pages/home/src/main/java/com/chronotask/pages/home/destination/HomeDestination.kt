package com.chronotask.pages.home.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.home.api.HomeArgument
import com.chronotask.pages.home.ui.HomeScreen

/**
 * 首页导航目标
 *
 * 架构层：注册为导航表中的首页入口，解析导航参数后进入 HomeScreen。
 */
@AppNavDestination(NavigationTable.NAV_HOME)
class HomeDestination : AppNavEntry<HomeArgument>() {
    @Composable
    override fun Content(argument: HomeArgument) {
        HomeScreen()
    }
}
