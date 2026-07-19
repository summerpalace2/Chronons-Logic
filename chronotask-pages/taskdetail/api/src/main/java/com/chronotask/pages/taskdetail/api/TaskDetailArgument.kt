package com.chronotask.pages.taskdetail.api

import com.chronotask.components.navigation.core.nav3.AppNavArgument
import kotlinx.serialization.Serializable

@Serializable
data class TaskDetailArgument(val taskId: Long, val date: Long = 0) : AppNavArgument