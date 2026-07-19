package com.chronotask.pages.stats.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.pages.stats.data.TagDistribution

/**
 * PieChart.kt
 *
 * 核心职责：饼图组件，展示标签时间分布。
 * 主要导出：PieChart。
 */

/**
 * 饼图组件
 *
 * 绘制环形饼图展示各标签的时间占比，右侧显示图例。
 *
 * @param data 标签分布数据列表（标签名 + 总秒数 + 百分比）
 * @param modifier 外部修饰符
 */

@Composable
fun PieChart(
    data: List<TagDistribution>,
    modifier: Modifier = Modifier
) {
    // 饼图配色方案（循环使用）- 12 种颜色，支持更多分类
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.inversePrimary,
        MaterialTheme.colorScheme.surfaceTint,
        Color(0xFF6750A4),  // Purple
        Color(0xFF03DAC5)   // Teal
    )

    // 动画进度（0f → 1f），驱动饼图展开动画
    var animProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(1000),
        label = "pie"
    )

    LaunchedEffect(data) { animProgress = 1f }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val stroke = 24.dp.toPx()
            val r = (size.minDimension - stroke) / 2
            val c = Offset(size.width / 2, size.height / 2)
            var accumulatedSweep = 0f

            data.forEachIndexed { i, d ->
                val sweep = d.percentage * 360f * animatedProgress
                val startAngle = -90f + accumulatedSweep
                drawArc(
                    colors[i % colors.size],
                    startAngle, sweep, false,
                    Offset(c.x - r, c.y - r),
                    Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Butt)
                )
                accumulatedSweep += sweep
            }
        }

        Spacer(modifier = Modifier.width(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEachIndexed { i, d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colors[i % colors.size]))
                    Spacer(Modifier.width(8.dp))
                    Text(d.tagName, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text("${String.format("%.1f", d.percentage * 100)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
