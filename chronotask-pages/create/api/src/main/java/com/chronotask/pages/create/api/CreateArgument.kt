package com.chronotask.pages.create.api

import com.chronotask.components.navigation.core.nav3.AppNavArgument
import kotlinx.serialization.Serializable

/**
 * CreateMode — 创建页面的工作模式
 *
 * @property Normal 标准模式：保存到 Room DB（主页任务）
 * @property QuickImport 一键导入模式：保存到 DataStore（导入模板）
 */
@Serializable
enum class CreateMode { Normal, QuickImport }

@Serializable
data class CreateArgument(
    val taskId: Long = -1,
    val mode: CreateMode = CreateMode.Normal
) : AppNavArgument {
    companion object {
        fun navigateForCreate() {
            CreateArgument(taskId = -1, mode = CreateMode.Normal).navigate()
        }
        fun navigateForEdit(taskId: Long) {
            CreateArgument(taskId = taskId, mode = CreateMode.Normal).navigate()
        }
        fun navigateForQuickImport() {
            CreateArgument(taskId = -1, mode = CreateMode.QuickImport).navigate()
        }
    }
}
