package ai.january.partner.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * The history part of a Tracking card: a title with a headline figure, the Week / Month / Year
 * switch, and the chart, a loading spinner, or the empty message. Failures show below the card.
 */
@Composable
internal fun TrackingChartSection(
    idPrefix: String,
    range: ChartRange,
    onRange: (ChartRange) -> Unit,
    loading: Boolean,
    failed: Boolean,
    isEmpty: Boolean,
    emptyText: String,
    headline: String?,
    chart: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(JanuaryColors.Divider))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (!loading && !isEmpty && headline != null) {
                Text(headline, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Muted, fontSize = 14.sp)
            }
        }
        // Buttons that report which range is selected, so screen readers announce "Week, selected".
        SegmentedControl(ChartRange.entries, range, { it.label }, onRange, Modifier.fillMaxWidth(), role = Role.Button) { "$idPrefix-range-${it.testTagSuffix}" }
        when {
            loading -> Box(Modifier.fillMaxWidth().height(CHART_HEIGHT), contentAlignment = Alignment.Center) {
                LoadingSpinner(Modifier.testTag("$idPrefix-loading"), JanuaryColors.Green)
            }
            failed -> Unit
            isEmpty -> Box(
                Modifier.fillMaxWidth().height(96.dp).background(JanuaryColors.Control, RoundedCornerShape(16.dp)).testTag("$idPrefix-empty"),
                contentAlignment = Alignment.Center,
            ) { Text(emptyText, color = JanuaryColors.Muted, fontWeight = FontWeight.Medium) }
            else -> chart()
        }
    }
}

private val CHART_HEIGHT = 190.dp

/** Daily (or, for Year, monthly) water totals as bars; empty days keep a faint slot. */
@Composable
internal fun WaterBarChart(bars: List<ChartValue>, range: ChartRange, unit: String, modifier: Modifier = Modifier) {
    val summary = waterChartSummary(range, bars, unit)
    val peak = bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    // A Year chart names every month by its initial; daily charts label a readable subset of days.
    val labels = if (range == ChartRange.YEAR) bars.indices.toList() else axisLabelIndices(bars.size, if (range == ChartRange.MONTH) 5 else 7)
    Canvas(modifier.fillMaxWidth().height(CHART_HEIGHT).testTag("water-chart").semantics { contentDescription = summary }) {
        val top = 20.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val paint = chartPaint(JanuaryColors.Muted.toArgb(), 11.sp.toPx())
        drawDashedLine(top)
        drawContext.canvas.nativeCanvas.drawText("${formatChartNumber(peak)} $unit", 0f, top - 6.dp.toPx(), paint)
        drawLine(JanuaryColors.Border, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())
        val slot = size.width / bars.size
        val barWidth = slot * if (bars.size > 12) 0.62f else 0.56f
        bars.forEachIndexed { index, bar ->
            val left = slot * index + (slot - barWidth) / 2
            if (bar.value > 0) {
                val height = max(((bar.value / peak) * (bottom - top)).toFloat(), 2.dp.toPx())
                drawRoundRect(
                    if (index == bars.lastIndex) JanuaryColors.Green else JanuaryColors.Green.copy(alpha = 0.72f),
                    Offset(left, bottom - height), Size(barWidth, height), CornerRadius(minOf(barWidth / 2, 4.dp.toPx())),
                )
            } else {
                drawRoundRect(JanuaryColors.ControlStrong, Offset(left, bottom - 3.dp.toPx()), Size(barWidth, 3.dp.toPx()), CornerRadius(1.5.dp.toPx()))
            }
        }
        labels.forEach { index ->
            val text = axisLabel(range, bars[index].date).let { if (range == ChartRange.YEAR) it.take(1) else it }
            val center = slot * index + slot / 2
            val x = (center - paint.measureText(text) / 2).coerceIn(0f, size.width - paint.measureText(text))
            drawContext.canvas.nativeCanvas.drawText(text, x, size.height - 6.dp.toPx(), paint)
        }
    }
}

/** Each day's weight as a line across the whole span, with its lowest and highest values marked. */
@Composable
internal fun WeightLineChart(points: List<ChartValue>, span: DateSpan, range: ChartRange, unit: String, modifier: Modifier = Modifier) {
    val summary = weightChartSummary(range, points, unit)
    val low = points.minOf { it.value }
    val high = points.maxOf { it.value }
    val spread = max(high - low, 1.0)
    val lower = low - spread * 0.25
    val upper = high + spread * 0.25
    val lastDay = max(span.days - 1, 1).toFloat()
    val labels = axisLabelIndices(span.days.toInt(), if (range == ChartRange.WEEK) 7 else 4)
    Canvas(modifier.fillMaxWidth().height(CHART_HEIGHT).testTag("weight-chart").semantics { contentDescription = summary }) {
        val top = 18.dp.toPx()
        val bottom = size.height - 24.dp.toPx()
        val paint = chartPaint(JanuaryColors.Muted.toArgb(), 11.sp.toPx())
        fun x(point: ChartValue) = ChronoUnit.DAYS.between(span.start, point.date) / lastDay * size.width
        fun y(value: Double) = (top + (upper - value) / (upper - lower) * (bottom - top)).toFloat()
        drawLine(JanuaryColors.Border, Offset(0f, bottom), Offset(size.width, bottom), 1.dp.toPx())
        drawDashedLine(y(high))
        drawContext.canvas.nativeCanvas.drawText("High ${formatChartNumber(high)} $unit", 0f, y(high) - 6.dp.toPx(), paint)
        if (points.size > 1 && high != low) {
            drawDashedLine(y(low))
            drawContext.canvas.nativeCanvas.drawText("Low ${formatChartNumber(low)} $unit", 0f, y(low) + 14.dp.toPx(), paint)
        }
        if (points.size > 1) {
            val line = Path().apply {
                moveTo(x(points[0]), y(points[0].value))
                points.drop(1).forEach { lineTo(x(it), y(it.value)) }
            }
            val fill = Path().apply { addPath(line); lineTo(x(points.last()), bottom); lineTo(x(points.first()), bottom); close() }
            drawPath(fill, Brush.verticalGradient(listOf(JanuaryColors.Rust.copy(alpha = 0.18f), JanuaryColors.Rust.copy(alpha = 0.02f)), startY = top, endY = bottom))
            drawPath(line, JanuaryColors.Rust, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (points.size <= 31) points.forEach { drawCircle(JanuaryColors.Rust, 3.dp.toPx(), Offset(x(it), y(it.value))) }
        val latest = points.last()
        val latestPosition = Offset(x(latest), y(latest.value))
        drawCircle(JanuaryColors.Surface, 6.dp.toPx(), latestPosition)
        drawCircle(JanuaryColors.Ink, 6.dp.toPx(), latestPosition, style = Stroke(2.dp.toPx()))
        labels.forEach { index ->
            val date = span.start.plusDays(index.toLong())
            val text = axisLabel(range, date)
            val center = index / lastDay * size.width
            val textX = (center - paint.measureText(text) / 2).coerceIn(0f, size.width - paint.measureText(text))
            drawContext.canvas.nativeCanvas.drawText(text, textX, size.height - 6.dp.toPx(), paint)
        }
    }
}

private fun DrawScope.drawDashedLine(y: Float) = drawLine(
    JanuaryColors.Border, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
)

private fun chartPaint(color: Int, textSize: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    this.color = color
    this.textSize = textSize
    typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
}
