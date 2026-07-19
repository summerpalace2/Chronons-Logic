package com.chronotask.pages.home.destination

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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronotask.components.navigation.core.nav3.AppNavDestination
import com.chronotask.components.navigation.core.nav3.AppNavEntry
import com.chronotask.components.navigation.core.nav3.NavigationTable
import com.chronotask.components.ui.theme.LocalAppDark
import com.chronotask.components.ui.R
import com.chronotask.pages.home.api.TabsArgument
import com.chronotask.pages.home.data.BottomNavItem
import com.chronotask.pages.home.ui.HomeScreen
import com.chronotask.pages.stats.ui.StatsScreen
import com.chronotask.pages.notes.ui.NotesScreen
import com.chronotask.pages.settings.ui.SettingsScreen


/** 底部导航条目列表 */
private val BOTTOM_NAV_ITEMS = listOf(
    BottomNavItem(R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(R.string.stats_title, Icons.Filled.Insights, Icons.Outlined.Insights),
    BottomNavItem(R.string.notes_title, Icons.AutoMirrored.Filled.StickyNote2, Icons.AutoMirrored.Outlined.StickyNote2),
    BottomNavItem(R.string.settings_title, Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * 标签页导航目标
 * 架构层：注册为导航表中的 Tabs 目的地
 */
@AppNavDestination(NavigationTable.NAV_TABS)
class TabsDestination : AppNavEntry<TabsArgument>() {
    @Composable
    override fun Content(argument: TabsArgument) {
        TabsScreen()
    }
}

/**
 * 标签页容器 — Scaffold + 底部导航 + 页面切换动画
 */
@Composable
private fun TabsScreen() {
    val isDark = LocalAppDark.current
    var currentPage by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            FloatingBottomNav(
                items = BOTTOM_NAV_ITEMS,
                currentIndex = currentPage,
                onItemClick = { currentPage = it },
                isDark = isDark
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

/**
 * 悬浮底部导航条
 *
 * 重组优化：interactionSource 通过 remember 在组件级别缓存，
 * 每次复用同一个实例，避免重复创建。
 *
 * @param items 导航条目列表
 * @param currentIndex 当前选中索引
 * @param onItemClick 点击回调
 * @param isDark 是否为暗色模式
 */
@Composable
private fun FloatingBottomNav(
    items: List<BottomNavItem>,
    currentIndex: Int,
    onItemClick: (Int) -> Unit,
    isDark: Boolean
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 重组优化：remember 在组件级缓存所有 interaction source，
    // 而不是在 forEach 内部 remember（每次索引变化会重建）
    val interactionSources = remember(items) {
        items.map { MutableInteractionSource() }
    }

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
