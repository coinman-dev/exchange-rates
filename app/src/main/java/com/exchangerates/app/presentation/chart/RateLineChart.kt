package com.exchangerates.app.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.exchangerates.app.domain.model.HistoryPoint

/**
 * Линейный график курса на Canvas: линия, градиентная заливка под ней и
 * «прицел» (scrub) — вертикальная линия с точкой, следующая за пальцем.
 *
 * Своя отрисовка вместо готовой библиотеки выбрана из-за полного контроля над
 * стилем (тонкая линия, градиент, точное поведение подсказки).
 */
@Composable
fun RateLineChart(
    points: List<HistoryPoint>,
    lineColor: Color,
    fillTop: Color,
    gridColor: Color,
    onScrub: (Int?) -> Unit,
    scrubIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var width by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(points.size) {
                detectTapGestures(
                    onPress = { offset ->
                        onScrub(indexAt(offset.x, size.width.toFloat(), points.size))
                        tryAwaitRelease()
                        onScrub(null)
                    },
                )
            }
            .pointerInput(points.size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onScrub(indexAt(offset.x, size.width.toFloat(), points.size))
                    },
                    onDragEnd = { onScrub(null) },
                    onDragCancel = { onScrub(null) },
                    onDrag = { change, _ ->
                        onScrub(indexAt(change.position.x, size.width.toFloat(), points.size))
                    },
                )
            },
    ) {
        width = size.width
        if (points.size < 2) return@Canvas

        val values = points.map { it.rate.toDouble() }
        val minValue = values.min()
        val maxValue = values.max()
        val span = (maxValue - minValue).takeIf { it > 0.0 } ?: (maxValue.takeIf { it != 0.0 } ?: 1.0)
        val verticalPadding = size.height * 0.10f
        val usableHeight = size.height - verticalPadding * 2

        fun xOf(index: Int) = size.width * index / (points.size - 1).toFloat()
        fun yOf(value: Double): Float {
            val normalized = ((value - minValue) / span).toFloat()
            return verticalPadding + (1f - normalized) * usableHeight
        }

        // горизонтальная сетка
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = verticalPadding + usableHeight * i / gridLines
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = with(density) { 1.dp.toPx() } / 2,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)),
            )
        }

        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { index, point ->
            val x = xOf(index)
            val y = yOf(point.rate.toDouble())
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(xOf(points.size - 1), size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(fillTop.copy(alpha = 0.35f), fillTop.copy(alpha = 0f)),
            ),
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = with(density) { 2.dp.toPx() }, cap = StrokeCap.Round),
        )

        val index = scrubIndex?.coerceIn(0, points.size - 1)
        if (index != null) {
            val x = xOf(index)
            val y = yOf(points[index].rate.toDouble())
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = with(density) { 1.dp.toPx() },
            )
            drawCircle(
                color = lineColor,
                radius = with(density) { 5.dp.toPx() },
                center = Offset(x, y),
            )
            drawCircle(
                color = Color.White,
                radius = with(density) { 2.dp.toPx() },
                center = Offset(x, y),
            )
        }
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int? {
    if (count < 2 || width <= 0f) return null
    val ratio = (x / width).coerceIn(0f, 1f)
    return Math.round(ratio * (count - 1))
}
