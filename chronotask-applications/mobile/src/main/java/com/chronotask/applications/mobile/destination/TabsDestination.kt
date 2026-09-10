package com.chronotask.applications.mobile.destination

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.components.ui.R
import com.chronotask.pages.home.api.TabsArgument
import com.chronotask.pages.home.ui.HomeScreen
import com.chronotask.pages.notes.ui.NotesScreen
import com.chronotask.pages.settings.ui.SettingsScreen
import com.chronotask.pages.stats.ui.StatsScreen

/** 应用组合根拥有底部导航，因此 feature 之间不直接依赖彼此的 UI 实现。 */
@AppNavDestination(NavigationTable.NAV_TABS)
class TabsDestination : AppNavEntry<TabsArgument>() {
    @Composable
    override fun Content(argument: TabsArgument) {
        TabsScreen()
    }
}

private data class BottomNavItem(
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(R.string.stats_title, Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomNavItem(R.string.notes_title, Icons.AutoMirrored.Filled.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2),
    BottomNavItem(R.string.settings_title, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
private fun TabsScreen() {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            FloatingBottomNav(
                items = bottomNavItems,
                currentIndex = currentPage,
                onItemClick = { currentPage = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "tab_switch"
            ) { page ->
                when (page) {
                    0 -> HomeScreen()
                    1 -> StatsScreen()
                    2 -> NotesScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomNav(
    items: List<BottomNavItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSources = remember(items) { items.map { MutableInteractionSource() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = currentIndex == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSources[index],
                                indication = null,
                                onClick = { onItemClick(index) }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.titleResId),
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) selectedColor else unselectedColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.titleResId),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 9.sp
                            ),
                            color = if (selected) selectedColor else unselectedColor
                        )
                    }
                }
            }
        }
    }
}
