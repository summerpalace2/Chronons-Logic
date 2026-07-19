package com.chronotask.components.navigation.core.nav3

import kotlinx.serialization.Serializable

/**
 * 导航参数基类
 *
 * navigate() / popBackStack() 通过 NavBackStackHolder 全局操作 backStack
 * 无需 CompositionLocal，任何地方均可调用
 */
interface AppNavArgument {
    fun navigate() {
        NavBackStackHolder.backStack?.push(this)
    }

    fun popBackStack() {
        val backStack = NavBackStackHolder.backStack ?: return
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    companion object {
        fun decodeFromRoute(route: String): AppNavArgument? {
            return RouteParser.parse(route)
        }
    }
}

fun String.navigateTo() {
    val navArgument = RouteParser.parse(this) ?: error("route not found")
    navArgument.navigate()
}

/**
 * 全局 BackStack Holder
 *
 * 替代 lateinit var appNavBackStack
 * 由 MainActivity.LaunchedEffect 赋值一次
 * navigate() / popBackStack() 在任何地方通过此 Holder 访问 backStack
 */
object NavBackStackHolder {
    @Volatile
    var backStack: AppNavBackStack? = null
        internal set

    /** 供外部模块（MainActivity）初始化 backStack */
    fun initialize(backStack: AppNavBackStack) {
        this.backStack = backStack
    }
}
