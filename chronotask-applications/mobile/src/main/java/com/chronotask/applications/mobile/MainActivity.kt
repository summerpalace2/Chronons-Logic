package com.chronotask.applications.mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.chronotask.applications.mobile.service.TimerService
import com.chronotask.components.common.DateUtils
import com.chronotask.components.common.QuickImportManager
import com.chronotask.components.common.TimerManager
import com.chronotask.components.common.appDataStore
import com.chronotask.components.common.appIoScope
import com.chronotask.components.database.entity.TaskEntity
import com.chronotask.components.database.repository.TaskRepository
import com.chronotask.components.navigation.core.nav3.AppNavDisplay
import com.chronotask.components.navigation.core.nav3.LocalAppNavBackStack
import com.chronotask.components.navigation.core.nav3.NavBackStackHolder
import com.chronotask.components.navigation.core.nav3.rememberAppNavBackStack
import com.chronotask.components.ui.theme.ChronoTaskTheme
import com.chronotask.components.ui.theme.LocaleManager
import com.chronotask.pages.taskdetail.api.TaskDetailArgument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MainActivity.kt
 *
 * 核心职责：提供 Compose 应用入口、导航容器、通知点击路由和通知权限申请。
 * 主要导出：MainActivity。
 */
class MainActivity : ComponentActivity() {

    /** 通知点击后待打开的任务 ID，由 onCreate/onNewIntent 写入。 */
    private val pendingTaskId = MutableStateFlow<Long?>(null)

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.wrapContextWithLocale(newBase ?: this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updatePendingTaskId(intent)

        setContent {
            val currentLocale by LocaleManager.currentLocale.collectAsState()
            val localizedContext = remember(currentLocale) {
                val config = Configuration(resources.configuration)
                config.setLocale(currentLocale)
                createConfigurationContext(config)
            }
            val backStack = rememberAppNavBackStack()
            val openTaskId by pendingTaskId.collectAsState()
            val runningTaskId by TimerManager.runningTaskId.collectAsState()
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )
            var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalAppNavBackStack provides backStack
            ) {
                ChronoTaskTheme {
                    // 初始化导航 Holder，并处理通知点击传入的任务详情路由。
                    LaunchedEffect(backStack, openTaskId) {
                        NavBackStackHolder.initialize(backStack)
                        openTaskId?.let { taskId ->
                            TaskDetailArgument(taskId, DateUtils.getTodayStart()).navigate()
                            pendingTaskId.value = null
                        }
                    }

                    // Android 13+ 的通知权限必须由 Activity 在用户操作期间申请。
                    LaunchedEffect(runningTaskId) {
                        if (
                            runningTaskId != null &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !notificationPermissionRequested &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionRequested = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // 一键导入自动同步：每天首次启动时自动导入任务。
                    LaunchedEffect(Unit) {
                        appIoScope.launch {
                            val enabled = appDataStore.quickImportEnabled.first()
                            if (enabled) {
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val lastImport = appDataStore.lastImportDate.first()
                                if (lastImport != todayStr) {
                                    val quickTasks = QuickImportManager.tasks.first()
                                    val offsetMinutes = appDataStore.dayStartOffsetMinutes.first()
                                    val activeDayMidnight = DateUtils.getActiveDayMidnight(
                                        System.currentTimeMillis(),
                                        offsetMinutes
                                    )
                                    val existingTitles = try {
                                        TaskRepository.getTasksByDate(activeDayMidnight).first()
                                            .map { it.title }
                                            .toSet()
                                    } catch (_: Exception) {
                                        emptySet<String>()
                                    }
                                    var imported = false
                                    quickTasks.forEach { quickTask ->
                                        if (quickTask.title !in existingTitles) {
                                            TaskRepository.insert(
                                                TaskEntity(
                                                    title = quickTask.title,
                                                    tagId = quickTask.tagId,
                                                    targetDurationMinutes = quickTask.targetMinutes,
                                                    scheduledDate = activeDayMidnight
                                                )
                                            )
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

    /** 接收通知 PendingIntent 传入的任务 ID。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        updatePendingTaskId(intent)
    }

    /** 将无效的通知参数过滤掉，避免打开不存在的任务详情页。 */
    private fun updatePendingTaskId(intent: Intent?) {
        pendingTaskId.value = intent
            ?.getLongExtra(TimerService.EXTRA_TASK_ID, -1L)
            ?.takeIf { it > 0L }
    }
}
