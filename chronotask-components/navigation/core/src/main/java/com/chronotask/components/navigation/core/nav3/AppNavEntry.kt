package com.chronotask.components.navigation.core.nav3

import androidx.compose.runtime.Composable
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

abstract class AppNavEntry<T : AppNavArgument> {
    open fun isNeedLogin(argument: T): Boolean = false

    @Composable
    abstract fun Content(argument: T)

    open fun getContentKey(argument: T): String = argument.toString()

    open fun buildMetadata(): Map<String, Any> = emptyMap()
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AppNavDestination(val route: String)

interface AppNavCollector<T : AppNavArgument> {
    val route: String
    val navEntry: AppNavEntry<T>
    val argumentClazz: KClass<T>
    val argumentSerializer: KSerializer<T>
}
