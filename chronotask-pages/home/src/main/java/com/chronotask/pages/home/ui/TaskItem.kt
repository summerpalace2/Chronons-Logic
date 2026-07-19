package com.chronotask.pages.home.ui
import com.chronotask.pages.home.data.TaskItemState

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chronotask.components.ui.R
import com.chronotask.components.ui.compose.clickableNoIndicator

// TaskItem — 单个任务卡片
// 角色：单条任务的完整视觉呈现
//   ┌────────┐────────────────────────────────────────────────┐────────┐
//   │ 左边框 │ 标题 / 标签 / 录制指示                         │ 计时/完成│
//   │ (运行) │                                               │   按钮   │
//   └────────┘────────────────────────────────────────────────┘────────┘
//   + 底部 LinearProgressIndicator（仅当设置目标时长时显示）
//
// 状态分支：
//   • 编辑模式 → 右侧显示「编辑」+「删除」按钮
//   • 运行中  → 右侧显示 Stop 按钮（主色圆）
//   • 已完成  → 右侧显示 ✓ 图标（无点击效果）
//   • 可开始  → 右侧显示 Play 按钮（今天启用，非今天禁用）

/**
 * 单个任务卡片
 *
 * @param item            任务条目展示状态（包含 task + 计算后的累计/运行状态）
 * @param isToday         是否选中今天（影响开始按钮的可点击性）
 * @param isEditMode      编辑模式（显示编辑/删除按钮，隐藏时长+计时按钮）
 * @param onStart         开始计时回调
 * @param onStop          停止计时回调
 * @param onComplete      切换完成状态回调
 * @param onDelete        删除任务回调
 * @param onClick         卡片主体点击回调（跳转到详情页）
 * @param onStartDenied   非今天日期点击开始时的兑底回调（弹 Toast）
 */
// ─── 公共 Composable ───

@Composable
fun TaskItem(
    item: TaskItemState,
    isToday: Boolean,
    isEditMode: Boolean = false,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onStartDenied: () -> Unit = {}
) {
    val task = item.task
    val isRunning = item.isRunning
    val isCompleted = task.isCompleted
    val isOverTarget = item.isOverTarget

    val targetMinutes = task.targetDurationMinutes
    val progress = targetMinutes?.let { tm ->
        if (tm > 0) (item.todayDurationSeconds.toFloat() / (tm * 60f)).coerceIn(0f, 1f) else null
    }

    // 左边框颜色 - 设计图中只有运行中的任务有左边框
    val leftBorderColor = when {
        isRunning && isOverTarget -> MaterialTheme.colorScheme.primary
        isRunning -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    val bgColor = when {
        isRunning -> MaterialTheme.colorScheme.surfaceContainerLowest
        isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val timeColor = when {
        isOverTarget -> MaterialTheme.colorScheme.primary
        isRunning -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .clickableNoIndicator { onClick() }
                .padding(
                    start = if (leftBorderColor != Color.Transparent) 4.dp else 12.dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左边框
            if (leftBorderColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .background(leftBorderColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 左侧内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 分类标签
                    if (item.tagName.isNotEmpty()) {
                        val tagBg = when {
                            isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val tagText = when {
                            isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(tagBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.tagName,
                                style = MaterialTheme.typography.labelMedium,
                                color = tagText
                            )
                        }
                    }

                    // 运行状态指示
                    if (!isCompleted && isRunning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(timeColor.copy(alpha = pulseAlpha))
                            )
                            Text(
                                text = stringResource(R.string.recording),
                                style = MaterialTheme.typography.bodyMedium,
                                color = timeColor
                            )
                        }
                    }
                }
            }

            // 右侧：编辑模式下的编辑/删除按钮，或正常模式下的时间+操作按钮
            if (isEditMode) {
                // 编辑模式：编辑和删除按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 编辑按钮
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickableNoIndicator {
                                com.chronotask.pages.create.api.CreateArgument.navigateForEdit(item.task.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    // 删除按钮
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickableNoIndicator { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                // 正常模式：时间 + 操作按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatDuration(item.todayDurationSeconds),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = timeColor,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                        )
                        if (targetMinutes != null) {
                            Text(
                                text = "/ ${formatDurationMinutes(targetMinutes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverTarget) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                            )
                        } else if (!isCompleted) {
                            Text(
                                text = stringResource(R.string.unlimited),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 操作按钮
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.completed),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else if (isRunning) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickableNoIndicator { onStop() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.stop),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        val isEnabled = isToday
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickableNoIndicator {
                                    if (isEnabled) onStart() else onStartDenied()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.start),
                                tint = if (isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // 底部进度条 - 位于卡片内部底部
        if (targetMinutes != null && targetMinutes > 0) {
            val progressColor = when {
                isOverTarget -> MaterialTheme.colorScheme.primary
                isRunning -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            }
            val trackColor = when {
                isOverTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                isRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
            LinearProgressIndicator(
                progress = { progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isRunning) 4.dp else 2.dp)
                    .align(Alignment.BottomCenter),
                color = progressColor,
                trackColor = trackColor,
            )
        }
    }
}
