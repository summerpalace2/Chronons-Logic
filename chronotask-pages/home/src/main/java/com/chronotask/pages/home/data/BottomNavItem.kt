package com.chronotask.pages.home.data

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * description ：底部导航栏参数
 * author : summer_palace2
 * email : qq2992203079@outlook.com
 * date : 2026/7/17 15:07
 */

/**
 * 底部导航条目数据
 *
 * @param titleResId 标题资源 ID
 * @param selectedIcon 选中态图标
 * @param unselectedIcon 未选中态图标
 */
data class BottomNavItem(
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
