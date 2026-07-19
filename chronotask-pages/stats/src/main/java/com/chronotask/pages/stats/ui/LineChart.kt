package com.chronotask.pages.stats.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronotask.pages.stats.data.TagDistribution


/**
 * 折线图数据点
 *
 * @param label X 轴标签（日期/月份）
 * @param value Y 轴数值（秒）
 * @param tagDistributions 该数据点的标签分布
 * @param isRestDay 是否为休息日（休息日不可选中）
 */
data class LineChartDataPoint(
    val label: String,
    val value: Long,
    val tagDistributions: List<TagDistribution> = emptyList(),
    val isRestDay: Boolean = false
)

/**
 * LineChart - 折线图组件
 *
 * 核心职责：绘制折线图，支持选中高亮。
 * 主要导出：LineChart, handleSelection。
 */
@Composable
fun LineChart(
    data: List<LineChartDataPoint>,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    onPointSelected: (Int, LineChartDataPoint?) -> Unit = { _, _ -> }
) {
    if (data.isEmpty() || data.size < 2) return

    // 最大 Y 值（用于归一化），至少为 1 避免除零
    val maxValue = data.maxOf { it.value }.coerceAtLeast(1L)
    val yAxisSteps = remember(maxValue) { calculateYAxisSteps(maxValue) }
    val yAxisMax = yAxisSteps.lastOrNull() ?: 1L

    // 动画进度（0f → 1f），驱动折线动画
    var animationProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(800),
        label = "line"
    )
    LaunchedEffect(data) { animationProgress = 0f; animationProgress = 1f }

    // 当前选中的数据点索引（-1 表示未选中）
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val pointOffsetsState = remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val density = LocalDensity.current
    val yAxisWidth = 30.dp
    val chartHeight = 224.dp
    val pointSpacing = 60.dp
    val leftPadding = 20.dp
    val rightPadding = 20.dp
    val scrollState = rememberScrollState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(chartHeight).graphicsLayer { clip = false }) {
            // Y轴标签
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(chartHeight)
                    .padding(top = 30.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisSteps.reversed().forEach { value ->
                    Text(
                        text = formatTimeLabel(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.width(4.dp))

            // 外层滚动容器 - 横向滚动 + 点击选点（scroll 优先，tap 次之）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(chartHeight)
                    .horizontalScroll(scrollState)
                    .graphicsLayer { clip = false }
                    .pointerInput(data, scrollState) {
                        detectTapGestures(
                            onTap = { offset ->
                                val canvasX = offset.x
                                handleSelection(canvasX, pointOffsetsState, scrollState, density, pointSpacing, data, selectedIndex) { index ->
                                    selectedIndex = index
                                    onPointSelected(index, if (index >= 0) data[index] else null)
                                }
                            }
                        )
                    }
            ) {
                val chartWidthDp = leftPadding + pointSpacing * (data.size - 1) + rightPadding

                Box(
                    modifier = Modifier
                        .width(chartWidthDp)
                        .height(chartHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val topPad = 30.dp.toPx()
                        val bottomPad = 24.dp.toPx()
                        val leftPad = leftPadding.toPx()
                        val chartH = size.height - topPad - bottomPad
                        val stepX = if (data.size > 1) (size.width - leftPad * 2) / (data.size - 1) else 0f

                        val points = data.mapIndexed { i, d ->
                            Offset(
                                x = leftPad + i * stepX,
                                y = size.height - bottomPad - (d.value.toFloat() / yAxisMax) * chartH * animatedProgress
                            )
                        }

                        // 更新坐标
                        val newY = points.map { it.y }
                        val oldY = pointOffsetsState.value.map { it.y }
                        val yChanged = newY.size != oldY.size ||
                                newY.zip(oldY).any { kotlin.math.abs(it.first - it.second) > 0.5f }
                        if (yChanged || canvasSize != size) {
                            pointOffsetsState.value = points
                            canvasSize = size
                        }

                        // 1. 参考线
                        yAxisSteps.forEach { step ->
                            val y = size.height - bottomPad - (step.toFloat() / yAxisMax) * chartH
                            drawLine(Color.LightGray.copy(alpha = 0.4f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                        }

                        // 2. 填充区域（跳过休息日）
                        val activeIndices = data.indices.filter { !data[it].isRestDay }
                        if (activeIndices.size >= 2) {
                            val fillPath = Path().apply {
                                val firstP = points[activeIndices.first()]
                                moveTo(firstP.x, size.height - bottomPad)
                                activeIndices.forEach { i -> lineTo(points[i].x, points[i].y) }
                                val lastP = points[activeIndices.last()]
                                lineTo(lastP.x, size.height - bottomPad)
                                close()
                            }
                            drawPath(fillPath, fillColor)

                            // 3. 折线（跳过休息日）
                            val linePath = Path().apply {
                                moveTo(points[activeIndices.first()].x, points[activeIndices.first()].y)
                                for (j in 1 until activeIndices.size) {
                                    lineTo(points[activeIndices[j]].x, points[activeIndices[j]].y)
                                }
                            }
                            drawPath(linePath, primaryColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }

                        // 4. 普通数据点（跳过休息日）
                        points.forEachIndexed { index, point ->
                            if (index != selectedIndex && !data[index].isRestDay) {
                                drawCircle(Color.White, 6.dp.toPx(), point)
                                drawCircle(primaryColor.copy(alpha = 0.8f), 4.dp.toPx(), point)
                            }
                        }

                        // 5. 选中垂直虚线（在选中点之前画，避免贯穿气泡）
                        if (selectedIndex in points.indices) {
                            val sp = points[selectedIndex]
                            drawLine(
                                primaryColor.copy(alpha = 0.3f),
                                Offset(sp.x, 0f),
                                Offset(sp.x, sp.y - 8.dp.toPx()),  // 只画到点上方
                                1.dp.toPx()
                            )
                        }

                        // 6. 选中的数据点（休息日不显示）
                        if (selectedIndex in points.indices && !data[selectedIndex].isRestDay) {
                            val p = points[selectedIndex]
                            drawCircle(Color.White, 8.dp.toPx(), p)
                            drawCircle(primaryColor, 6.dp.toPx(), p)
                        }

                        // 7. 静态标签（跳过休息日和零值）
                        val textPaint = android.graphics.Paint().apply {
                            textSize = 10.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }

                        points.forEachIndexed { index, point ->
                            if (data[index].value <= 0 || data[index].isRestDay || index == selectedIndex) return@forEachIndexed
                            textPaint.color = android.graphics.Color.argb(180, 100, 100, 113)
                            val labelY = point.y - 14.dp.toPx()
                            drawContext.canvas.nativeCanvas.drawText(
                                formatTimeLabel(data[index].value), point.x, labelY, textPaint
                            )
                        }

                        // 8. X轴标签
                        val xPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(180, 100, 100, 113)
                            textSize = 11.sp.toPx()
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        data.forEachIndexed { index, d ->
                            val x = leftPad + index * stepX
                            drawContext.canvas.nativeCanvas.drawText(
                                d.label, x, size.height - 4.dp.toPx(), xPaint
                            )
                        }

                        // 9. 选中气泡（最后绘制，在最上层）
                        if (selectedIndex in points.indices) {
                            val p = points[selectedIndex]
                            val label = formatTimeLabel(data[selectedIndex].value)

                            val bubbleW = 50.dp.toPx()
                            val bubbleH = 22.dp.toPx()
                            val cornerRadius = 6.dp.toPx()

                            // 居中优先，溢出时 clamp 到画布边界
                            var bubbleX = p.x - bubbleW / 2
                            if (bubbleX < 0f) bubbleX = 0f
                            if (bubbleX + bubbleW > size.width) bubbleX = size.width - bubbleW
                            val bubbleY = p.y - bubbleH - 8.dp.toPx()

                            // 气泡背景
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(bubbleX, bubbleY),
                                size = Size(bubbleW, bubbleH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                            )

                            // 气泡文字
                            val bubblePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 11.sp.toPx()
                                isFakeBoldText = true
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                label,
                                bubbleX + bubbleW / 2,
                                bubbleY + bubbleH / 2 + 4.dp.toPx(),
                                bubblePaint
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun handleSelection(
    canvasX: Float,
    pointOffsetsState: androidx.compose.runtime.MutableState<List<Offset>>,
    scrollState: ScrollState,
    density: Density,
    pointSpacing: androidx.compose.ui.unit.Dp,
    data: List<LineChartDataPoint>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    val offsets = pointOffsetsState.value
    if (offsets.isEmpty()) return

    // canvasX 已为画布绝对坐标（localX + scrollOffset），直接使用
    val contentX = canvasX

    // 找最近的点
    val nearest = offsets.withIndex().minByOrNull { kotlin.math.abs(it.value.x - contentX) }
    if (nearest == null) {
        if (currentIndex != -1) onIndexChange(-1)
        return
    }

    // 容差：实际点间距的一半
    val stepX = if (offsets.size >= 2) offsets[1].x - offsets[0].x
                else with(density) { pointSpacing.toPx() }
    val tolerance = stepX / 2f

    if (kotlin.math.abs(nearest.value.x - contentX) <= tolerance) {
        if (data[nearest.index].isRestDay) {
            // 休息日不可选中
            if (currentIndex != -1) onIndexChange(-1)
        } else if (currentIndex != nearest.index) {
            onIndexChange(nearest.index)
        }
    } else {
        if (currentIndex != -1) onIndexChange(-1)
    }
}


