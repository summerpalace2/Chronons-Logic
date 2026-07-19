package com.chronotask.pages.notes.api

import com.chronotask.components.navigation.core.nav3.AppNavArgument
import kotlinx.serialization.Serializable

/**
 * NotesArgument - 笔记页导航参数
 *
 * 核心职责：为笔记页提供导航参数。全局笔记页无需 taskId，使用默认值 -1。
 */
@Serializable
data class NotesArgument(val taskId: Long = -1) : AppNavArgument