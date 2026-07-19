package com.chronotask.pages.taskdetail.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.taskdetail.api.TaskDetailArgument
import com.chronotask.pages.taskdetail.ui.TaskDetailScreen

/**
 * TaskDetailDestination - 任务详情页导航入口
 *
 * 核心职责：注册导航路由，接收 TaskDetailArgument 参数并传递给 TaskDetailScreen。
 */
@AppNavDestination(NavigationTable.NAV_TASK_DETAIL)
class TaskDetailDestination : AppNavEntry<TaskDetailArgument>() {
    @Composable
    override fun Content(argument: TaskDetailArgument) {
        TaskDetailScreen(taskId = argument.taskId, date = argument.date)
    }
}
