package com.chronotask.components.navigation.core.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.reflect.KClass

/**
 *
 *
 */
val LocalAppNavBackStack = compositionLocalOf<AppNavBackStack> {
    error("No AppNavBackStack provided")
}

class AppNavBackStack(internal val base: SnapshotStateList<AppNavArgument>) :
    List<AppNavArgument> by base, StateObject by base, RandomAccess by base {

    constructor(elements: List<AppNavArgument>) : this(base = elements.toMutableStateList())

    internal fun push(argument: AppNavArgument): Boolean {
        if (lastOrNull() == argument) return false
        base.add(argument)
        return true
    }

    internal fun removeAt(index: Int) {
        base.removeAt(index)
    }

    fun clear() {
        base.clear()
    }

    override fun toString(): String = Snapshot.withoutReadObservation { base.toString() }
}

@Composable
fun rememberAppNavBackStack(): AppNavBackStack {
    return rememberSaveable(saver = appNavBackStackSaver()) {
        AppNavBackStack(listOf(getFirstAppNavArgument()))
    }
}

fun getFirstAppNavArgument(): AppNavArgument {
    return AppNavArgument.decodeFromRoute(NavigationTable.NAV_TABS)!!
}

private fun appNavBackStackSaver(): Saver<AppNavBackStack, String> {
    val listSerializer = ListSerializer(PolymorphicSerializer(AppNavArgument::class))
    return Saver(
        save = { backStack -> navJson.encodeToString(listSerializer, backStack.toList()) },
        restore = { savedJsonString ->
            val restoredList = navJson.decodeFromString(listSerializer, savedJsonString)
            AppNavBackStack(restoredList)
        }
    )
}

internal val navJson: Json by lazy {
    ensureCollectorsInitialized()
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        serializersModule = SerializersModule {
            polymorphic(AppNavArgument::class) {
                appNavCollectors.values.forEach { collector ->
                    @Suppress("UNCHECKED_CAST")
                    subclass(
                        collector.argumentClazz as KClass<AppNavArgument>,
                        collector.argumentSerializer as kotlinx.serialization.KSerializer<AppNavArgument>
                    )
                }
            }
        }
    }
}
