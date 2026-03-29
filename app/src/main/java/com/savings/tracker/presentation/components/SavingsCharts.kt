package com.savings.tracker.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.mutableFloatStateOf
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
    val selectedDotColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var pointPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Zoom/pan state: scale ∈ [1, data.size/5], panFraction ∈ [0, 1]
    var zoomScale by remember(data) { mutableFloatStateOf(1f) }
    var panFraction by remember(data) { mutableFloatStateOf(0f) }

    val visibleCount = (data.size / zoomScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
    val maxStartIndex = (data.size - visibleCount).coerceAtLeast(0)
    val startIndex = (panFraction * maxStartIndex).toInt().coerceIn(0, maxStartIndex)
    val visibleData = if (data.size <= 1) data else data.subList(startIndex, startIndex + visibleCount)

    val paddingStart = with(density) { 48.dp.toPx() }
    val paddingTop = with(density) { 16.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 48.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(data) {
                    coroutineScope {
                        launch {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val newScale = (zoomScale * gestureZoom).coerceIn(1f, (data.size / 5f).coerceAtLeast(1f))
                                zoomScale = newScale
                                val currentVisibleCount = (data.size / newScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
                                val currentMaxStart = (data.size - currentVisibleCount).coerceAtLeast(0)
                                if (currentMaxStart > 0 && canvasSize.width > 0) {
                                    val panDelta = -pan.x / canvasSize.width.toFloat()
                                    panFraction = (panFraction + panDelta).coerceIn(0f, 1f)
                                }
                                if (gestureZoom != 1f) selectedIndex = null
                            }
                        }
                        launch {
                            detectTapGestures { tapOffset ->
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                // Recompute visible window at tap time using current state
                                val currentVisibleCount = (data.size / zoomScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
                                val currentMaxStart = (data.size - currentVisibleCount).coerceAtLeast(0)
                                val currentStart = (panFraction * currentMaxStart).toInt().coerceIn(0, currentMaxStart)
                                val currentVisible = if (data.size <= 1) data else data.subList(currentStart, currentStart + currentVisibleCount)

                                val maxVal = currentVisible.maxOf { it.second }.coerceAtLeast(1.0)
                                val minVal = currentVisible.minOf { it.second }
                                val range = (maxVal - minVal).coerceAtLeast(1.0)
                                val points = currentVisible.mapIndexed { index, (_, value) ->
                                    val x = if (currentVisible.size == 1) w / 2 else w * index / (currentVisible.size - 1)
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
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val maxVal = visibleData.maxOf { it.second }.coerceAtLeast(1.0)
            val minVal = visibleData.minOf { it.second }
            val range = (maxVal - minVal).coerceAtLeast(1.0)

            // Grid lines
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = h - (h * i / gridCount)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                val labelValue = minVal + range * i / gridCount
                drawContext.canvas.nativeCanvas.drawText(
                    formatAmountRsd(labelValue),
                    -with(density) { 44.dp.toPx() },
                    y + with(density) { 4.sp.toPx() },
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.LEFT
                    },
                )
            }

            // Year boundary separators
            val yearSeparatorColor = gridColor.copy(alpha = 0.6f)
            visibleData.forEachIndexed { index, (label, _) ->
                val currentYear = if (label.length >= 10) label.substring(6) else ""
                val prevYear = if (index > 0) {
                    val prev = visibleData[index - 1].first
                    if (prev.length >= 10) prev.substring(6) else ""
                } else ""
                if (index > 0 && currentYear != prevYear) {
                    val x = if (visibleData.size == 1) w / 2 else w * index / (visibleData.size - 1)
                    drawLine(
                        color = yearSeparatorColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 2f,
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        currentYear,
                        x,
                        h + with(density) { 28.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        },
                    )
                }
                if (index == 0 && currentYear.isNotEmpty()) {
                    val x = if (visibleData.size == 1) w / 2 else w * 0f / (visibleData.size - 1)
                    drawContext.canvas.nativeCanvas.drawText(
                        currentYear,
                        x + with(density) { 16.dp.toPx() },
                        h + with(density) { 28.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        },
                    )
                }
            }

            // Line path
            val path = Path()
            val points = visibleData.mapIndexed { index, (_, value) ->
                val x = if (visibleData.size == 1) w / 2 else w * index / (visibleData.size - 1)
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
                if (isSelected) {
                    drawCircle(selectedDotColor.copy(alpha = 0.25f), radius = 18f, center = point)
                    drawCircle(selectedDotColor, radius = 11f, center = point)
                    drawCircle(Color.White, radius = 6f, center = point)
                } else {
                    drawCircle(lineColor, radius = 5f, center = point)
                    drawCircle(Color.White, radius = 3f, center = point)
                }
            }

            // X-axis labels (show a subset)
            val labelStep = (visibleData.size / 5).coerceAtLeast(1)
            visibleData.forEachIndexed { index, (label, _) ->
                if (index % labelStep == 0 || index == visibleData.lastIndex) {
                    val x = if (visibleData.size == 1) w / 2 else w * index / (visibleData.size - 1)
                    val displayLabel = if (label.length >= 5) label.substring(0, 5) else label
                    drawContext.canvas.nativeCanvas.drawText(
                        displayLabel,
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

        // Tooltip overlay — positioned above dot if space allows, below otherwise
        selectedIndex?.let { idx ->
            if (idx in pointPositions.indices && idx in visibleData.indices) {
                val point = pointPositions[idx]
                val (label, value) = visibleData[idx]

                val tooltipWidthPx = with(density) { 140.dp.toPx() }
                val tooltipHeightPx = with(density) { 58.dp.toPx() }
                val dotGapPx = with(density) { 12.dp.toPx() }

                val dotAbsX = paddingStart + point.x
                val dotAbsY = paddingTop + point.y

                // Show below dot when near the top, above otherwise
                val tooltipY = if (point.y < tooltipHeightPx + dotGapPx) {
                    dotAbsY + dotGapPx + 8f  // below
                } else {
                    dotAbsY - tooltipHeightPx - dotGapPx  // above
                }

                // Center horizontally on the dot, clamped so it stays on screen
                val maxX = (paddingStart + canvasSize.width - tooltipWidthPx).coerceAtLeast(0f)
                val tooltipX = (dotAbsX - tooltipWidthPx / 2f).coerceIn(0f, maxX)

                ChartTooltip(
                    modifier = Modifier.offset { IntOffset(tooltipX.toInt(), tooltipY.toInt()) },
                ) {
                    Column {
                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "${formatAmountRsd(value)} RSD",
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var zoomScale by remember(data) { mutableFloatStateOf(1f) }
    var panFraction by remember(data) { mutableFloatStateOf(0f) }

    val visibleCount = (data.size / zoomScale).toInt().coerceIn(3.coerceAtMost(data.size), data.size)
    val maxStartIndex = (data.size - visibleCount).coerceAtLeast(0)
    val startIndex = (panFraction * maxStartIndex).toInt().coerceIn(0, maxStartIndex)
    val visibleData = if (data.size <= 1) data else data.subList(startIndex, startIndex + visibleCount)

    val paddingStart = with(density) { 48.dp.toPx() }
    val paddingTop = with(density) { 16.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 48.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(data) {
                    coroutineScope {
                        launch {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val newScale = (zoomScale * gestureZoom).coerceIn(1f, (data.size / 3f).coerceAtLeast(1f))
                                zoomScale = newScale
                                val curVisible = (data.size / newScale).toInt().coerceIn(3.coerceAtMost(data.size), data.size)
                                val curMaxStart = (data.size - curVisible).coerceAtLeast(0)
                                if (curMaxStart > 0 && canvasSize.width > 0) {
                                    panFraction = (panFraction - pan.x / canvasSize.width).coerceIn(0f, 1f)
                                }
                                selectedIndex = null
                            }
                        }
                        launch {
                            detectTapGestures { tapOffset ->
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                val curVisibleCount = (data.size / zoomScale).toInt().coerceIn(3.coerceAtMost(data.size), data.size)
                                val curMaxStart = (data.size - curVisibleCount).coerceAtLeast(0)
                                val curStart = (panFraction * curMaxStart).toInt().coerceIn(0, curMaxStart)
                                val curVisible = if (data.size <= 1) data else data.subList(curStart, curStart + curVisibleCount)
                                val groupWidth = w / curVisible.size
                                val tappedIndex = curVisible.indices.firstOrNull { i ->
                                    tapOffset.x >= groupWidth * i && tapOffset.x < groupWidth * (i + 1) && tapOffset.y >= 0f && tapOffset.y <= h
                                }
                                selectedIndex = if (tappedIndex != null && tappedIndex != selectedIndex) tappedIndex else null
                            }
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val maxVal = visibleData.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
            val groupWidth = w / visibleData.size
            val barWidth = groupWidth * 0.35f
            val gap = groupWidth * 0.05f

            // Grid
            for (i in 0..4) {
                val y = h - (h * i / 4)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    formatAmountRsd(maxVal * i / 4),
                    -with(density) { 44.dp.toPx() },
                    y + with(density) { 4.sp.toPx() },
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.LEFT
                    },
                )
            }

            // Year separators + year labels
            val yearSepColor = gridColor.copy(alpha = 0.6f)
            visibleData.forEachIndexed { index, (label, _, _) ->
                val currentYear = label.takeLast(4)
                val prevYear = if (index > 0) visibleData[index - 1].first.takeLast(4) else ""
                if (index > 0 && currentYear != prevYear) {
                    val x = groupWidth * index
                    drawLine(yearSepColor, Offset(x, 0f), Offset(x, h), strokeWidth = 2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        currentYear,
                        x + with(density) { 4.dp.toPx() },
                        h + with(density) { 30.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.LEFT
                            isFakeBoldText = true
                        },
                    )
                }
                if (index == 0 && currentYear.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        currentYear,
                        groupWidth * 0.5f,
                        h + with(density) { 30.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        },
                    )
                }
            }

            // Bars + month labels
            val labelStep = (visibleData.size / 5).coerceAtLeast(1)
            visibleData.forEachIndexed { index, (label, deposits, withdrawals) ->
                val groupX = groupWidth * index + groupWidth * 0.1f
                val depHeight = (deposits / maxVal * h).toFloat()
                drawRect(savingsGreen, topLeft = Offset(groupX, h - depHeight), size = Size(barWidth, depHeight))
                val wdHeight = (withdrawals / maxVal * h).toFloat()
                drawRect(withdrawalRed, topLeft = Offset(groupX + barWidth + gap, h - wdHeight), size = Size(barWidth, wdHeight))
                if (index % labelStep == 0 || index == visibleData.lastIndex) {
                    drawContext.canvas.nativeCanvas.drawText(
                        label.take(3),
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

        // Tooltip overlay — clamped to stay within chart bounds
        selectedIndex?.let { idx ->
            if (idx in visibleData.indices && canvasSize.width > 0) {
                val w = canvasSize.width.toFloat()
                val groupWidth = w / visibleData.size
                val (label, deposits, withdrawals) = visibleData[idx]
                val tooltipWidthPx = with(density) { 180.dp.toPx() }
                val rawX = paddingStart + groupWidth * idx + groupWidth * 0.1f
                val maxX = (paddingStart + canvasSize.width - tooltipWidthPx).coerceAtLeast(0f)
                val tooltipX = rawX.coerceIn(0f, maxX)
                ChartTooltip(modifier = Modifier.offset { IntOffset(tooltipX.toInt(), paddingTop.toInt()) }) {
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = "Deposits: ${formatAmountRsd(deposits)} RSD",
                            style = MaterialTheme.typography.bodySmall,
                            color = savingsGreen,
                        )
                        Text(
                            text = "Withdrawals: ${formatAmountRsd(withdrawals)} RSD",
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
                    val tooltipWidthPx = with(LocalDensity.current) { 160.dp.toPx() }
                    val rawX = padPx + tp.x - tooltipWidthPx / 2f
                    val maxX = (with(LocalDensity.current) { 16.dp.toPx() } + pieCenterAndRadius!!.first * 2 - tooltipWidthPx).coerceAtLeast(0f)
                    val tooltipX = rawX.coerceIn(0f, maxX)
                    val tooltipY = (padPx + tp.y - 70f).coerceAtLeast(0f)

                    ChartTooltip(
                        modifier = Modifier.offset { IntOffset(tooltipX.toInt(), tooltipY.toInt()) },
                    ) {
                        Column {
                            Text(text = label, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "${formatAmountRsd(value)} RSD",
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
fun StackedAreaChart(
    data: List<Triple<String, Double, Double>>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) { EmptyChartMessage(modifier); return }

    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var zoomScale by remember(data) { mutableFloatStateOf(1f) }
    var panFraction by remember(data) { mutableFloatStateOf(0f) }

    val visibleCount = (data.size / zoomScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
    val maxStartIndex = (data.size - visibleCount).coerceAtLeast(0)
    val startIndex = (panFraction * maxStartIndex).toInt().coerceIn(0, maxStartIndex)
    val visibleData = if (data.size <= 1) data else data.subList(startIndex, startIndex + visibleCount)

    val paddingStart = with(density) { 48.dp.toPx() }
    val paddingTop = with(density) { 16.dp.toPx() }
    val maxVal = visibleData.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .padding(start = 48.dp, end = 16.dp, top = 16.dp, bottom = 48.dp)
                .onSizeChanged { canvasSize = it }
                .pointerInput(data) {
                    coroutineScope {
                        launch {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val newScale = (zoomScale * gestureZoom).coerceIn(1f, (data.size / 5f).coerceAtLeast(1f))
                                zoomScale = newScale
                                val curVisible = (data.size / newScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
                                val curMaxStart = (data.size - curVisible).coerceAtLeast(0)
                                if (curMaxStart > 0 && canvasSize.width > 0) {
                                    panFraction = (panFraction - pan.x / canvasSize.width).coerceIn(0f, 1f)
                                }
                                selectedIndex = null
                            }
                        }
                        launch {
                            detectTapGestures { tap ->
                                val w = canvasSize.width.toFloat()
                                val curVisibleCount = (data.size / zoomScale).toInt().coerceIn(5.coerceAtMost(data.size), data.size)
                                val curMaxStart = (data.size - curVisibleCount).coerceAtLeast(0)
                                val curStart = (panFraction * curMaxStart).toInt().coerceIn(0, curMaxStart)
                                val curVisible = if (data.size <= 1) data else data.subList(curStart, curStart + curVisibleCount)
                                val step = if (curVisible.size > 1) w / (curVisible.size - 1) else w
                                val idx = (tap.x / step).toInt().coerceIn(curVisible.indices)
                                selectedIndex = if (idx != selectedIndex) idx else null
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            for (i in 0..4) {
                val y = h - (h * i / 4)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                drawContext.canvas.nativeCanvas.drawText(
                    formatAmountRsd(maxVal * i / 4),
                    -with(density) { 44.dp.toPx() },
                    y + with(density) { 4.sp.toPx() },
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = with(density) { 10.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }

            fun xOf(i: Int): Float = if (visibleData.size == 1) w / 2 else w * i / (visibleData.size - 1)
            fun yOf(v: Double): Float = h - (v / maxVal * h).toFloat()

            val depositPath = Path()
            depositPath.moveTo(xOf(0), h)
            visibleData.forEachIndexed { i, (_, dep, _) -> depositPath.lineTo(xOf(i), yOf(dep)) }
            depositPath.lineTo(xOf(visibleData.lastIndex), h)
            depositPath.close()
            drawPath(depositPath, savingsGreen.copy(alpha = 0.35f))
            val depositLine = Path()
            visibleData.forEachIndexed { i, (_, dep, _) ->
                if (i == 0) depositLine.moveTo(xOf(i), yOf(dep)) else depositLine.lineTo(xOf(i), yOf(dep))
            }
            drawPath(depositLine, savingsGreen, style = Stroke(width = 2f, cap = StrokeCap.Round))

            val wdPath = Path()
            wdPath.moveTo(xOf(0), h)
            visibleData.forEachIndexed { i, (_, _, wd) -> wdPath.lineTo(xOf(i), yOf(wd)) }
            wdPath.lineTo(xOf(visibleData.lastIndex), h)
            wdPath.close()
            drawPath(wdPath, withdrawalRed.copy(alpha = 0.35f))
            val wdLine = Path()
            visibleData.forEachIndexed { i, (_, _, wd) ->
                if (i == 0) wdLine.moveTo(xOf(i), yOf(wd)) else wdLine.lineTo(xOf(i), yOf(wd))
            }
            drawPath(wdLine, withdrawalRed, style = Stroke(width = 2f, cap = StrokeCap.Round))

            // Year separators + year labels (label format: "dd.MM.yy")
            val yearSepColor = gridColor.copy(alpha = 0.6f)
            visibleData.forEachIndexed { index, (label, _, _) ->
                val currentYear = label.takeLast(2)
                val prevYear = if (index > 0) visibleData[index - 1].first.takeLast(2) else ""
                if (index > 0 && currentYear != prevYear) {
                    val x = xOf(index)
                    drawLine(yearSepColor, Offset(x, 0f), Offset(x, h), strokeWidth = 2f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "'$currentYear",
                        x + with(density) { 4.dp.toPx() },
                        h + with(density) { 30.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.LEFT
                            isFakeBoldText = true
                        },
                    )
                }
                if (index == 0 && currentYear.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        "'$currentYear",
                        xOf(0) + with(density) { 4.dp.toPx() },
                        h + with(density) { 30.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = primaryColor.hashCode()
                            textSize = with(density) { 10.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.LEFT
                            isFakeBoldText = true
                        },
                    )
                }
            }

            // X-axis labels
            val labelStep = (visibleData.size / 5).coerceAtLeast(1)
            visibleData.forEachIndexed { i, (label, _, _) ->
                if (i % labelStep == 0 || i == visibleData.lastIndex) {
                    drawContext.canvas.nativeCanvas.drawText(
                        label.take(5),
                        xOf(i),
                        h + with(density) { 14.sp.toPx() },
                        android.graphics.Paint().apply {
                            color = textColor.hashCode()
                            textSize = with(density) { 9.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }

        selectedIndex?.let { idx ->
            if (idx in visibleData.indices && canvasSize.width > 0) {
                val (label, dep, wd) = visibleData[idx]
                val w = canvasSize.width.toFloat()
                val xPos = paddingStart + (if (visibleData.size == 1) w / 2 else w * idx / (visibleData.size - 1))
                val tooltipWidthPx = with(density) { 180.dp.toPx() }
                val maxX = (paddingStart + canvasSize.width - tooltipWidthPx).coerceAtLeast(0f)
                val tooltipX = xPos.coerceIn(0f, maxX)
                ChartTooltip(modifier = Modifier.offset { IntOffset(tooltipX.toInt(), paddingTop.toInt()) }) {
                    Column {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text("Deposits: ${formatAmountRsd(dep)} RSD", style = MaterialTheme.typography.bodySmall, color = savingsGreen)
                        Text("Withdrawals: ${formatAmountRsd(wd)} RSD", style = MaterialTheme.typography.bodySmall, color = withdrawalRed)
                    }
                }
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
