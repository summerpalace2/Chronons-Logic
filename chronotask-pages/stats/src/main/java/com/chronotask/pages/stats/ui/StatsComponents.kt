package com.chronotask.pages.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.FormatUtils


/**
 * 统计卡片容器
 *
 * 统一的卡片样式：最低表面色背景 + 12dp 圆角 + 16dp 内边距。
 * 用于包裹统计块（总览、分类统计等），提供一致的视觉层级。
 *
 * @param modifier 外部修饰符
 * @param content 卡片内部内容
 */
@Composable
internal fun StatsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * 格式化时长
 *
 * 将秒数转换为可读字符串（如 "2h 30m"），委托 FormatUtils.formatDuration 实现。
 *
 * @param totalSeconds 总秒数
 * @return 格式化后的时间字符串
 */
internal fun formatDuration(totalSeconds: Long): String = FormatUtils.formatDuration(totalSeconds)
