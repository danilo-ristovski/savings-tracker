package com.savings.tracker.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savings.tracker.presentation.main.formatAmountRsd
import com.savings.tracker.presentation.theme.feeOrange
import com.savings.tracker.presentation.theme.savingsGreen
import com.savings.tracker.presentation.theme.withdrawalRed
import kotlin.math.atan2
import kotlin.math.sqrt

private val pieColors = listOf(savingsGreen, withdrawalRed, feeOrange, Color(0xFF1565C0), Color(0xFF6A1B9A))

@Composable
private fun ChartTooltip(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.widthIn(max = 200.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp,
        tonalElevation = 3.dp,
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            content()
        }
    }
}

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

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Store computed point positions for tooltip placement
    var pointPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val paddingStart = with(density) { 48.dp.toPx() }
    val paddingEnd = with(density) { 16.dp.toPx() }
    val paddingTop = with(density) { 16.dp.toPx() }
    val paddingBottom = with(density) { 32.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        val maxVal = data.maxOf { it.second }.coerceAtLeast(1.0)
                        val minVal = data.minOf { it.second }
                        val range = (maxVal - minVal).coerceAtLeast(1.0)

                        val points = data.mapIndexed { index, (_, value) ->
                            val x = if (data.size == 1) w / 2 else w * index / (data.size - 1)
                            val y = h - ((value - minVal) / range * h).toFloat()
                            Offset(x, y)
                        }

                        val touchRadius = 48f
                        val tappedIndex = points.indexOfFirst { pt ->
                            val dx = pt.x - tapOffset.x
                            val dy = pt.y - tapOffset.y
                            sqrt(dx * dx + dy * dy) < touchRadius
                        }
                        selectedIndex = if (tappedIndex >= 0 && tappedIndex != selectedIndex) tappedIndex else null
                    }
                },
        ) {
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
            pointPositions = points

            points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.x, point.y)
                else path.lineTo(point.x, point.y)
            }

            drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Points
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                drawCircle(lineColor, radius = if (isSelected) 8f else 5f, center = point)
                drawCircle(Color.White, radius = if (isSelected) 5f else 3f, center = point)
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

        // Tooltip overlay
        selectedIndex?.let { idx ->
            if (idx in pointPositions.indices) {
                val point = pointPositions[idx]
                val (label, value) = data[idx]
                val tooltipX = (paddingStart + point.x - 60f).coerceAtLeast(0f)
                val tooltipY = (paddingTop + point.y - 60f).coerceAtLeast(0f)

                ChartTooltip(
                    modifier = Modifier.offset { IntOffset(tooltipX.toInt(), tooltipY.toInt()) },
                ) {
                    Column {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = formatAmountRsd(value),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
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

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val paddingStart = with(density) { 48.dp.toPx() }
    val paddingTop = with(density) { 16.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 32.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        val maxVal = data.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
                        val groupWidth = w / data.size

                        val tappedIndex = data.indices.firstOrNull { index ->
                            val groupX = groupWidth * index
                            tapOffset.x >= groupX && tapOffset.x < groupX + groupWidth &&
                                tapOffset.y >= 0f && tapOffset.y <= h
                        }
                        selectedIndex = if (tappedIndex != null && tappedIndex != selectedIndex) tappedIndex else null
                    }
                },
        ) {
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

        // Tooltip overlay
        selectedIndex?.let { idx ->
            if (idx in data.indices && canvasSize.width > 0) {
                val w = canvasSize.width.toFloat()
                val groupWidth = w / data.size
                val (label, deposits, withdrawals) = data[idx]
                val tooltipX = (paddingStart + groupWidth * idx + groupWidth * 0.1f).coerceAtLeast(0f)
                val tooltipY = paddingTop

                ChartTooltip(
                    modifier = Modifier.offset { IntOffset(tooltipX.toInt(), tooltipY.toInt()) },
                ) {
                    Column {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "Deposits: ${formatAmountRsd(deposits)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = savingsGreen,
                        )
                        Text(
                            text = "Withdrawals: ${formatAmountRsd(withdrawals)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = withdrawalRed,
                        )
                    }
                }
            }
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

    var selectedSlice by remember { mutableStateOf<Int?>(null) }
    var pieCenterAndRadius by remember { mutableStateOf<Triple<Float, Float, Float>?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(slices) {
                        detectTapGestures { tapOffset ->
                            val info = pieCenterAndRadius ?: return@detectTapGestures
                            val (cx, cy, radius) = info
                            val dx = tapOffset.x - cx
                            val dy = tapOffset.y - cy
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist > radius) {
                                selectedSlice = null
                                return@detectTapGestures
                            }

                            // Angle in degrees, starting from -90 (top), going clockwise
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Normalize to 0..360 starting from top (-90 degrees)
                            angle = (angle + 90f + 360f) % 360f

                            var cumulative = 0f
                            var found = -1
                            for (i in slices.indices) {
                                val sweep = (slices[i].second / total * 360f).toFloat()
                                if (angle >= cumulative && angle < cumulative + sweep) {
                                    found = i
                                    break
                                }
                                cumulative += sweep
                            }
                            tapPosition = tapOffset
                            selectedSlice = if (found >= 0 && found != selectedSlice) found else null
                        }
                    },
            ) {
                val diameter = minOf(size.width, size.height)
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                pieCenterAndRadius = Triple(
                    size.width / 2f,
                    size.height / 2f,
                    diameter / 2f,
                )
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

            // Tooltip overlay
            selectedSlice?.let { idx ->
                if (idx in slices.indices) {
                    val (label, value) = slices[idx]
                    val percentage = value / total * 100
                    val tp = tapPosition ?: return@let
                    // Position tooltip near the tap, offset by padding
                    val padPx = with(LocalDensity.current) { 16.dp.toPx() }
                    val tooltipX = (padPx + tp.x - 50f).coerceAtLeast(0f)
                    val tooltipY = (padPx + tp.y - 70f).coerceAtLeast(0f)

                    ChartTooltip(
                        modifier = Modifier.offset { IntOffset(tooltipX.toInt(), tooltipY.toInt()) },
                    ) {
                        Column {
                            Text(text = label, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = formatAmountRsd(value),
                                style = MaterialTheme.typography.bodyMedium,
                                color = pieColors[idx % pieColors.size],
                            )
                            Text(
                                text = "${"%.1f".format(percentage)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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
