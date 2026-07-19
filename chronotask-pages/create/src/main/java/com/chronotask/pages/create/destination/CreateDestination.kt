package com.chronotask.pages.create.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.create.api.CreateArgument
import com.chronotask.pages.create.ui.CreateScreen

/**
 * 创建页面导航目标
 *
 * 职责：注册为导航表中的创建页目的地，解析导航参数后进入 CreateScreen。
 * 在架构中属于导航入口层（Destination），不包含业务逻辑。
 */
@AppNavDestination(NavigationTable.NAV_CREATE)
class CreateDestination : AppNavEntry<CreateArgument>() {
    @Composable
    override fun Content(argument: CreateArgument) {
        CreateScreen(taskId = argument.taskId, mode = argument.mode)
    }
}