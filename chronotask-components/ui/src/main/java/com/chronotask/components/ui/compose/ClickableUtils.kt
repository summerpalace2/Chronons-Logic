/*
 * ClickableUtils.kt
 *
 * clickable 扩展工具 — 为 Jetpack Compose 的 Modifier 提供去波纹、防重复点击等点击扩展。
 * 属于 chronotask-components UI 模块中的 compose 子模块。
 *
 * 核心导出：
 *   - Modifier.clickableNoIndicator — 无水波纹效果的点击扩展
 *   - Modifier.clickableSingle     — 防重复点击（节流）的点击扩展
 *
 * 使用示例：
 * ```kotlin
 * Box(modifier = Modifier.clickableNoIndicator { onClick() })
 * Text(modifier = Modifier.clickableSingle(interval = 800L) { onConfirm() })
 * ```
 */

package com.chronotask.components.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * 无水波纹效果的 clickable 扩展。
 *
 * 适用于希望点击无视觉反馈（如水波纹）但仍保留点击语义的场景。
 *
 * @param enabled 是否启用点击，默认 true
 * @param onClickLabel 无障碍语义标签，可为 null
 * @param role 语义角色，可为 null
 * @param onClick 点击时执行的动作
 */
fun Modifier.clickableNoIndicator(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    onClick = onClick
)

private var lastClickTime = 0L

/**
 * 防重复点击的 clickable 扩展（节流）。
 *
 * 在指定时间间隔内只允许触发一次点击，避免用户快速连续点击导致的重复操作。
 *
 * @param enabled 是否启用点击，默认 true
 * @param onClickLabel 无障碍语义标签，可为 null
 * @param role 语义角色，可为 null
 * @param interval 两次点击之间的最小间隔（毫秒），默认 500ms
 * @param onClick 点击时执行的动作
 */
fun Modifier.clickableSingle(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    interval: Long = 500L,
    onClick: () -> Unit,
) = clickable(
    enabled = enabled,
    onClickLabel = onClickLabel,
    role = role,
    interactionSource = null,
    indication = null
) {
    val nowClickTime = System.currentTimeMillis()
    if (nowClickTime - lastClickTime >= interval) {
        lastClickTime = nowClickTime
        onClick()
    }
}