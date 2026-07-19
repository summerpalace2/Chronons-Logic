package com.chronotask.pages.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator

// TodayRestToggle
// 角色：首页右上角的「休息日/工作日」切换开关
//
// 状态映射：
//   isRest = true  → 红色描边 + 红色标签 → 休息模式（当天不计入统计）
//   isRest = false → 紫色描边 + 紫罗兰标签 → 正常模式
//
// 视觉结构：
//   ┌─────────────────────────────┐
//   │  ●  休息模式 / 工作模式      │
//   └─────────────────────────────┘
//   （圆角胶囊 + 左侧小圆点 + 文案）

/**
 * 今日休息切换开关
 *
 * @param isRest   当前是否为休息日（控制颜色与文案）
 * @param onToggle 点击回调（由 ViewModel 处理状态切换与落库）
 */
@Composable
fun TodayRestToggle(isRest: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                color = if (isRest) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                if (isRest) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickableNoIndicator(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 状态指示圆点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRest) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
            )
            // 状态文案
            Text(
                text = stringResource(if (isRest) R.string.rest_mode else R.string.working_mode),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isRest) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}