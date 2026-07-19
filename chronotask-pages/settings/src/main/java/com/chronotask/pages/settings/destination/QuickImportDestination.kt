/**
 * QuickImportDestination.kt
 *
 * 核心职责：一键导入编辑器页面的导航入口。
 *          注册路由为 settings/quickImport，由 KSP Processor 自动发现。
 */

package com.chronotask.pages.settings.destination

import androidx.compose.runtime.Composable
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.pages.settings.api.QuickImportArgument
import com.chronotask.pages.settings.ui.QuickImportEditor

@AppNavDestination(NavigationTable.NAV_QUICK_IMPORT)
class QuickImportDestination : AppNavEntry<QuickImportArgument>() {
    @Composable
    @Suppress("UNUSED_PARAMETER")
    override fun Content(argument: QuickImportArgument) {
        QuickImportEditor(
            onBack = { QuickImportArgument.popBackStack() }
        )
    }
}
