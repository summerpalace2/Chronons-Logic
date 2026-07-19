package com.chronotask.components.ui.picker

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chronotask.components.ui.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * VerticalTimePickerDialog — 垂直滚轮时间选择弹窗
 *
 * @param initialHours   初始小时值 (0-23)，由外部传入上次设置值
 * @param initialMinutes 初始分钟值 (0-59)
 * @param onConfirm      确认回调，参数为 (选择的小时, 选择的分钟)
 * @param onDismiss      取消/点击外部区域回调
 */
/**
 * VerticalTimePickerDialog — 公共垂直滚轮时间选择弹窗
 *
 * 通用组件，可在任意模块使用。
 * 用法示例：
 *   VerticalTimePickerDialog(
 *       initialHours = 2,
 *       initialMinutes = 30,
 *       onConfirm = { h, m -> ... },
 *       onDismiss = { }
 *   )
 */

@Composable
fun VerticalTimePickerDialog(
    initialHours: Int = 0,
    initialMinutes: Int = 0,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    //当前选中的小时/分钟 — 由各自的 VerticalWheel 内部状态同步上来
    var selectedHours by remember { mutableIntStateOf(initialHours) }
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //── 标题 ──
                Text(
                    text = stringResource(R.string.set_target_duration),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(32.dp))

                //── 双滚轮行：小时 : 分钟 ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    VerticalWheel(
                        itemCount = 24,
                        initialValue = initialHours,
                        itemHeight = 60.dp,
                        visibleItems = 3,
                        onValueSelected = { selectedHours = it },
                        textStrings = (0..23).map { it.toString().padStart(2, '0') },
                        primaryColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    VerticalWheel(
                        itemCount = 60,
                        initialValue = initialMinutes,
                        itemHeight = 60.dp,
                        visibleItems = 3,
                        onValueSelected = { selectedMinutes = it },
                        textStrings = (0..59).map { it.toString().padStart(2, '0') },
                        primaryColor = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                //── 按钮行 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedHours, selectedMinutes) }) {
                        Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


/**
 * VerticalWheel — 单个垂直无限滚轮
 *
 * @param itemCount      滚轮项目总数（小时=24，分钟=60）
 * @param initialValue   初始选中的值 (0 until itemCount)
 * @param itemHeight     每个项目的高度 (dp)
 * @param visibleItems   同时可见的项目数（用于确定滚轮高度 = itemHeight * visibleItems）
 * @param onValueSelected 选择值变化回调，参数为选中项的索引
 * @param textStrings     每个索引对应的显示文本（如 "00", "01" ... "23"）
 * @param primaryColor    中心高亮项的颜色
 * @param textColor       非中心项的默认颜色
 */
@Composable
private fun VerticalWheel(
    itemCount: Int,
    initialValue: Int,
    itemHeight: androidx.compose.ui.unit.Dp,
    visibleItems: Int = 5,
    onValueSelected: (Int) -> Unit,
    textStrings: List<String>,
    primaryColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // ── 布局常量 ──
    val itemHeightPx = with(density) { itemHeight.toPx() }       // 单项高度（像素）
    val wheelHeight = itemHeight * visibleItems                  // 滚轮总高度

    // ── 核心状态 ──
    //currentOffset: 当前偏移量。值 = index * itemHeightPx 时表示第 index 项在滚轮中心。
    //0 = 第 0 项居中，itemHeightPx = 第 1 项居中，以此类推。
    var currentOffset by remember { mutableFloatStateOf(initialValue * itemHeightPx) }

    //动画驱动器 — 用于 snapTo（无动画过渡）和 animateTo（带动画过渡）
    val animatable = remember { Animatable(currentOffset) }

    //物理衰减器 — 用于高速滑动时预测目标位置
    val decay = remember { exponentialDecay<Float>() }

    //速度追踪器 — 记录手指移动历史，松手时计算瞬时速度
    val tracker = remember { VelocityTracker() }

    //flingJob: 跟踪正在运行的 animateTo 协程引用。
    //新手势开始时取消旧动画，确保没有并发动画冲突。
    var flingJob: kotlinx.coroutines.Job? = null

    //textPaint: Canvas 绘制文字用的 Paint，remember 避免每帧重建
    val textPaint = remember { Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER } }

    //isFlinging: 标记当前是否处于"滑动中"。
    //用于点按判断：如果滑动中抬起，按当前位置吸附；否则按点击位置计算。
    var isFlinging by remember { mutableStateOf(false) }

    //── 初始值同步 ──
    //当外部传入的 initialValue 变化时（如弹窗重新打开），重置滚轮位置
    LaunchedEffect(initialValue) {
        currentOffset = initialValue * itemHeightPx
        animatable.snapTo(currentOffset)
    }

    //手势处理 ──
    //pointerInput 绑定到 itemCount，确保 itemCount 变化时重新注册手势
    Box(
        modifier = Modifier
            .height(wheelHeight)
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(itemCount) {
                    //取消正在运行的 fling 动画，确保新手势立即响应
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    //取消正在运行的 fling 动画，确保新手势立即响应
                    flingJob?.cancel()
                    var isTap = true                  // 初始假定为点按，后续根据拖动距离修正
                    var lastY = down.position.y       // 上一次手指 Y 坐标，用于计算帧间位移 dy
                    var lastScrollIdx = (currentOffset / itemHeightPx).roundToInt()  // 上一次滚动的 index，用于震动反馈
                    val wasFlinging = isFlinging      // 捕获上一个手势的滑动状态（用于点按判断）
                    isFlinging = false                // 重置滑动标记

                    //拖拽循环：持续监听指针事件直到手指抬起
                    while (true) {
                        val event = awaitPointerEvent()
                        val c = event.changes.firstOrNull { it.id == down.id } ?: break

                        //手指抬起 → 跳出循环，进入吸附逻辑
                        if (!c.pressed) {
                            c.consume()
                            break
                        }

                        val y = c.position.y
                        val dy = lastY - y       // 正值 = 手指上滑（滚轮上移，index 减小）；负值 = 下滑
                        lastY = y

                        //更新偏移量：手指上滑 → currentOffset 增大 → 上方条目移入中心
                        currentOffset += dy

                        //滚动震动反馈：滚过每一格时触发轻微震动
                        val currentIdx = (currentOffset / itemHeightPx).roundToInt()
                        if (currentIdx != lastScrollIdx) {
                            lastScrollIdx = currentIdx
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        //记录位置历史，松手时用于计算速度
                        tracker.addPosition(c.uptimeMillis, c.position)


                        //判断是否为点按：拖动距离 > 15px 或 按住时间 > 300ms → 不是点按
                        if (abs(y - down.position.y) > 15f || (c.uptimeMillis - down.uptimeMillis) > 300) {
                            isTap = false
                        }

                        c.consume()
                    }

                    //手指抬起：计算瞬时速度
                    // 取负值是因为：手指上滑（y 减小）时 vy 应为正，表示 index 减小方向
                    val vy = try { -tracker.calculateVelocity().y } catch (_: Exception) { 0f }

                    //吸附逻辑：根据点按/拖拽/滑动的不同，计算目标 index
                    flingJob = scope.launch {
                        if (isTap) {
                            // ══════ 点按场景 ══════
                            val targetIdx = if (wasFlinging) {
                                //如果之前处于滑动中，按当前位置就近吸附
                                (currentOffset / itemHeightPx).roundToInt()
                            } else {
                                //否则根据点击位置计算：点击在中心上方 → index 减小，下方 → index 增大
                                val centerY = size.height / 2f
                                ((down.position.y - centerY + currentOffset) / itemHeightPx).roundToInt()
                            }

                            val targetOffset = targetIdx * itemHeightPx
                            isFlinging = false

                            //先同步 animatable 到当前 offset，确保动画从正确位置开始
                            animatable.snapTo(currentOffset)

                            animatable.animateTo(
                                targetOffset,
                                tween(150, easing = FastOutSlowInEasing)
                            ) { currentOffset = value }

                            currentOffset = targetOffset

                            onValueSelected(floorMod(targetIdx, itemCount))
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                        } else {
                            // ══════ 拖拽/滑动场景 ══════
                            isFlinging = true

                            //物理衰减预测：基于当前速度计算"如果没有阻力会停在哪里"
                            val projectedTarget = decay.calculateTargetValue(currentOffset, vy)

                            val targetIdx = if (abs(vy) > 300f) {
                                //高速滑动：使用物理衰减预测目标 index
                                //速度越大，滑动距离越远
                                (projectedTarget / itemHeightPx).roundToInt()
                            } else {
                                //低速拖拽：从当前位置就近吸附（防止回弹）
                                //不再基于起始位移判断，避免"拖了不到半格就弹回"的问题
                                (currentOffset / itemHeightPx).roundToInt()
                            }

                            val targetOffset = targetIdx * itemHeightPx

                            //动态计算动画时长：距离越长越长，范围 [300, 1000]ms
                            val distance = abs(targetOffset - currentOffset)
                            val itemsToScroll = distance / itemHeightPx
                            val duration = if (itemsToScroll <= 0f) 0
                            else (200 + itemsToScroll * 60).toInt().coerceIn(300, 1000)

                            //先同步 animatable 到当前 offset，确保动画从正确位置开始
                            animatable.snapTo(currentOffset)

                            animatable.animateTo(
                                targetValue = targetOffset,
                                animationSpec = tween(
                                    durationMillis = duration,
                                    easing = LinearOutSlowInEasing
                                )
                            ) { currentOffset = value }

                            currentOffset = targetOffset
                            isFlinging = false

                            onValueSelected(floorMod(targetIdx, itemCount))
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        //选中项高亮框：半透明主题色背景，标记当前中心项
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        )

        //Canvas 绘制：根据 currentOffset 绘制可见的条目
        Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
            val cx = size.width / 2
            val cy = size.height / 2

            //当前偏移对应的"虚拟 index"（含小数，表示在两格之间）
            val ci = currentOffset / itemHeightPx

            //只绘制中心 ± 1.5 格范围内的条目（性能优化：不绘制不可见的条目）
            val from = (ci - 1.5f).toInt()
            val to = (ci + 1.5f).toInt()

            val primaryColorArgb = primaryColor.toArgb()

            for (raw in from..to) {
                // floorMod 保证负数 index 也能正确回绕（如 -1 → itemCount-1）
                val idx = floorMod(raw, itemCount)
                val text = textStrings[idx]

                // dy: 该条目相对于中心位置的像素偏移
                //正值 = 在中心上方，负值 = 在中心下方
                val dy = raw * itemHeightPx - currentOffset
                val dist = dy / itemHeightPx  // 以"格"为单位的距离

                //超出可见范围的跳过
                if (abs(dist) > 1.5f) continue

                //视觉效果：
                //alpha: 距离中心越远越透明（1.5 格处完全透明）
                // scale: 距离中心越远越小（最小 0.7）
                //center: 距离 < 0.1 格时判定为"居中项"，使用主题色 + 加粗
                val alpha = (1f - abs(dist) / 1.5f).coerceIn(0f, 1f)
                val scale = (1f - abs(dist) / 3f).coerceIn(0.7f, 1f)
                val center = abs(dist) < 0.1f

                val colorArgb = if (center) primaryColorArgb else textColor.copy(alpha = alpha).toArgb()
                val fs = if (center) 32.sp else (24.sp * scale)
                val fsPx = fs.toPx()

                textPaint.textSize = fsPx
                textPaint.isFakeBoldText = center
                textPaint.color = colorArgb

                // 基线位置 = 中心 + dy - 文字高度修正（使文字垂直居中）
                val baselineY = cy + dy - (textPaint.ascent() + textPaint.descent()) / 2

                drawContext.canvas.nativeCanvas.drawText(text, cx, baselineY, textPaint)
            }
        }
    }
}

/**
 * floorMod — 正模运算（结果始终 ≥ 0）
 *
 * @param a 被除数（可能为负数）
 * @param b 除数（正数）
 * @return 正模结果，范围 [0, b)
 */
private fun floorMod(a: Int, b: Int): Int = ((a % b) + b) % b
