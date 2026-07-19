package com.chronotask.components.common

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

/** 全局 Application 实例，由 ChronoTaskApplication.onCreate() 注入 */
lateinit var appApplication: Application
    private set

/** 全局 Context（复用 ApplicationContext，避免 Activity 泄漏） */
val appContext: Context
    get() = appApplication.applicationContext

/** 应用进程生命周期 */
val appLifecycle: Lifecycle
    get() = ProcessLifecycleOwner.get().lifecycle

private val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    Log.e("ChronoTask", "appCoroutineScope 未捕获异常", throwable)
}

/**
 * 跟随应用进程生命周期的主线程作用域
 * 用于 UI 相关的全局操作
 */
val appCoroutineScope: CoroutineScope = CoroutineScope(
    SupervisorJob(appLifecycle.coroutineScope.coroutineContext[Job])
        + Dispatchers.Main.immediate
        + appExceptionHandler
)

/**
 * 跟随应用进程生命周期的 IO 作用域
 * 用于需要保活的数据操作——退出页面后协程仍可完成数据保存
 */
val appIoScope: CoroutineScope = CoroutineScope(
    SupervisorJob(appLifecycle.coroutineScope.coroutineContext[Job])
        + Dispatchers.IO
        + appExceptionHandler
)

fun setApplication(application: Application) {
    appApplication = application
}
