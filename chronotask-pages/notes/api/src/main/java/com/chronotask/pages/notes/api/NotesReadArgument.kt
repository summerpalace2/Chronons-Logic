package com.chronotask.pages.notes.api

import com.chronotask.components.navigation.core.nav3.AppNavArgument
import kotlinx.serialization.Serializable

/**
 * NotesReadArgument - 笔记阅读页导航参数
 *
 * @param noteId 笔记 ID（-1 表示新建）
 */
@Serializable
data class NotesReadArgument(val noteId: Long) : AppNavArgument