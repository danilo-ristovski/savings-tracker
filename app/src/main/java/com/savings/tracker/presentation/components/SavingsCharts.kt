package com.savings.tracker.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.presentation.theme.feeOrange
import com.savings.tracker.presentation.theme.savingsGreen
import com.savings.tracker.presentation.theme.withdrawalRed

private val pieColors = listOf(savingsGreen, withdrawalRed, feeOrange, Color(0xFF1565C0), Color(0xFF6A1B9A))

@Composable
fun LineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        EmptyChartMessage(modifier)
        return
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Canvas(modifier = modifier.padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)) {
        val w = size.width
        val h = size.height
        val maxVal = data.maxOf { it.second }.coerceAtLeast(1.0)
        val minVal = data.minOf { it.second }
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        // Grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = h - (h * i / gridCount)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            val labelValue = minVal + range * i / gridCount
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f".format(labelValue),
                -with(density) { 44.dp.toPx() },
                y + with(density) { 4.sp.toPx() },
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.LEFT
                },
            )
        }

        // Line path
        val path = Path()
        val points = data.mapIndexed { index, (_, value) ->
            val x = if (data.size == 1) w / 2 else w * index / (data.size - 1)
            val y = h - ((value - minVal) / range * h).toFloat()
            Offset(x, y)
        }

        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y)
            else path.lineTo(point.x, point.y)
        }

        drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Points
        points.forEach { point ->
            drawCircle(lineColor, radius = 5f, center = point)
            drawCircle(Color.White, radius = 3f, center = point)
        }

        // X-axis labels (show a subset)
        val labelStep = (data.size / 5).coerceAtLeast(1)
        data.forEachIndexed { index, (label, _) ->
            if (index % labelStep == 0 || index == data.lastIndex) {
                val x = if (data.size == 1) w / 2 else w * index / (data.size - 1)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    h + with(density) { 14.sp.toPx() },
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = with(density) { 9.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                    },
                )
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<Triple<String, Double, Double>>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) {
        EmptyChartMessage(modifier)
        return
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    Canvas(modifier = modifier.padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)) {
        val w = size.width
        val h = size.height
        val maxVal = data.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
        val groupWidth = w / data.size
        val barWidth = groupWidth * 0.35f
        val gap = groupWidth * 0.05f

        // Grid
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = h - (h * i / gridCount)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f".format(maxVal * i / gridCount),
                -with(density) { 44.dp.toPx() },
                y + with(density) { 4.sp.toPx() },
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.LEFT
                },
            )
        }

        data.forEachIndexed { index, (label, deposits, withdrawals) ->
            val groupX = groupWidth * index + groupWidth * 0.1f

            // Deposit bar
            val depHeight = (deposits / maxVal * h).toFloat()
            drawRect(
                savingsGreen,
                topLeft = Offset(groupX, h - depHeight),
                size = Size(barWidth, depHeight),
            )

            // Withdrawal bar
            val wdHeight = (withdrawals / maxVal * h).toFloat()
            drawRect(
                withdrawalRed,
                topLeft = Offset(groupX + barWidth + gap, h - wdHeight),
                size = Size(barWidth, wdHeight),
            )

            // Label
            drawContext.canvas.nativeCanvas.drawText(
                label,
                groupX + barWidth,
                h + with(density) { 14.sp.toPx() },
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = with(density) { 8.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                },
            )
        }
    }
}

@Composable
fun PieChart(
    slices: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
) {
    if (slices.isEmpty() || slices.all { it.second == 0.0 }) {
        EmptyChartMessage(modifier)
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val total = slices.sumOf { it.second }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
        ) {
            val diameter = minOf(size.width, size.height)
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            var startAngle = -90f

            slices.forEachIndexed { index, (_, value) ->
                val sweep = (value / total * 360f).toFloat() * animationProgress.value
                drawArc(
                    color = pieColors[index % pieColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                )
                startAngle += sweep
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            slices.forEachIndexed { index, (label, value) ->
                if (index > 0) Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(pieColors[index % pieColors.size]),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$label (${"%.0f".format(value / total * 100)}%)",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalComparisonBar(
    label1: String,
    value1: Double,
    label2: String,
    value2: Double,
    modifier: Modifier = Modifier,
) {
    val total = (value1 + value2).coerceAtLeast(1.0)
    val fraction1 = (value1 / total).toFloat()
    val fraction2 = (value2 / total).toFloat()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("$label1: ${"%.0f".format(value1)}", style = MaterialTheme.typography.bodySmall)
            Text("$label2: ${"%.0f".format(value2)}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            if (fraction1 > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction1)
                        .height(24.dp)
                        .background(savingsGreen, MaterialTheme.shapes.extraSmall),
                )
            }
            if (fraction2 > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction2)
                        .height(24.dp)
                        .background(withdrawalRed, MaterialTheme.shapes.extraSmall),
                )
            }
        }
    }
}

@Composable
private fun EmptyChartMessage(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No data available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
