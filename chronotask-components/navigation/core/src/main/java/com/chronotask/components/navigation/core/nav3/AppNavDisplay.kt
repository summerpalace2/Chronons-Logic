package com.chronotask.components.navigation.core.nav3

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.chronotask.components.navigation.core.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay

@Suppress("UNCHECKED_CAST")
@Composable
fun AppNavDisplay(modifier: Modifier = Modifier, backStack: AppNavBackStack? = null) {
    val navBackStack = backStack ?: rememberAppNavBackStack()

    NavDisplay(
        modifier = modifier,
        backStack = navBackStack,
        onBack = { navBackStack.lastOrNull()?.popBackStack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        sceneStrategy = SinglePaneSceneStrategy(),
        transitionSpec = {
            ContentTransform(
                targetContentEnter = slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)),
                initialContentExit = fadeOut(tween(175))
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = fadeIn(tween(175)),
                initialContentExit = slideOutHorizontally(tween(350)) { -it / 3 } + fadeOut(tween(350))
            )
        },
        entryProvider = remember(appNavCollectors) {
            entryProvider(
                fallback = { argument ->
                    NavEntry(argument) { FallbackContent(argument) }
                }
            ) {
                appNavCollectors.forEach { route, collector ->
                    val navEntry = collector.navEntry as AppNavEntry<AppNavArgument>
                    addEntryProvider(
                        clazz = collector.argumentClazz,
                        clazzContentKey = { navEntry.getContentKey(it) },
                        metadata = navEntry.buildMetadata()
                    ) { AppNavEntryContent(navEntry = navEntry, argument = it) }
                }
            }
        }
    )
}

@Composable
private fun AppNavEntryContent(navEntry: AppNavEntry<AppNavArgument>, argument: AppNavArgument) {
    if (navEntry.isNeedLogin(argument)) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { Text(text = stringResource(R.string.navigation_need_login)) }
        return
    }
    navEntry.Content(argument)
}

@Composable
private fun FallbackContent(argument: AppNavArgument) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.navigation_page_not_found))
    }
}
