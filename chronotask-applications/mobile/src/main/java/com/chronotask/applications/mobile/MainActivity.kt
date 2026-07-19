package com.chronotask.applications.mobile

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chronotask.components.navigation.core.nav3.AppNavDisplay
import com.chronotask.components.navigation.core.nav3.LocalAppNavBackStack
import com.chronotask.components.navigation.core.nav3.NavBackStackHolder
import com.chronotask.components.navigation.core.nav3.rememberAppNavBackStack
import com.chronotask.components.ui.theme.ChronoTaskTheme
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.appDataStore
import com.chronotask.components.common.appIoScope
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.repository.TaskRepository
import com.chronotask.components.ui.theme.LocaleManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 主 Activity — 应用入口
 *
 * 职责：
 * 1. attachBaseContext：通过 LocaleManager.wrapContextWithLocale 读取 SharedPreferences 配置 Locale
 * 2. CompositionLocalProvider：注入 localizedContext + LocalAppNavBackStack
 * 3. NavBackStackHolder：将 backStack 注入全局 Holder，供 AppNavArgument.navigate() 调用
 *
 * ProcessLifecycleOwner 的监听已移至 ChronoTaskApplication.onCreate()
 * attachBaseContext 逻辑已抽取至 LocaleManager.wrapContextWithLocale()
 */
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.wrapContextWithLocale(newBase ?: this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentLocale by LocaleManager.currentLocale.collectAsState()
            val localizedContext = remember(currentLocale) {
                val config = Configuration(resources.configuration)
                config.setLocale(currentLocale)
                createConfigurationContext(config)
            }

            val backStack = rememberAppNavBackStack()

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalAppNavBackStack provides backStack
            ) {
                ChronoTaskTheme {
                    // 将 backStack 注入全局 Holder，供 AppNavArgument.navigate() 调用
                    LaunchedEffect(backStack) {
                        NavBackStackHolder.initialize(backStack)
                    }

                    // 一键导入自动同步：每天首次启动时自动导入任务
                    LaunchedEffect(Unit) {
                        appIoScope.launch {
                            val enabled = appDataStore.quickImportEnabled.first()
                            if (enabled) {
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val lastImport = appDataStore.lastImportDate.first()
                                if (lastImport != todayStr) {
                                    val quickTasks = QuickImportManager.tasks.first()
                                    val offsetMinutes = appDataStore.dayStartOffsetMinutes.first()
                                    val activeDayMidnight = DateUtils.getActiveDayMidnight(System.currentTimeMillis(), offsetMinutes)
                                    // 检查重复：查询当天已有任务标题，避免与手动导入冲突
                                    val existingTitles = try {
                                        TaskRepository.getTasksByDate(activeDayMidnight).first()
                                            .map { it.title }
                                            .toSet()
                                    } catch (_: Exception) { emptySet<String>() }
                                    var imported = false
                                    quickTasks.forEach { qt ->
                                        if (qt.title !in existingTitles) {
                                            TaskRepository.insert(TaskEntity(
                                                title = qt.title,
                                                tagId = qt.tagId,
                                                targetDurationMinutes = qt.targetMinutes,
                                                scheduledDate = activeDayMidnight
                                            ))
                                            imported = true
                                        }
                                    }
                                    if (imported) appDataStore.setLastImportDate(todayStr)
                                }
                            }
                        }
                    }

                    Scaffold { paddingValues ->
                        AppNavDisplay(
                            backStack = backStack,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

