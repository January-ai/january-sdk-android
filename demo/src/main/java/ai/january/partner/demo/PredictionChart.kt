package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

data class PredictionPoint(val minutes: Double, val value: Double)

internal data class PredictionChartDomain(val lower: Double, val upper: Double)
internal fun predictionChartDomain(points: List<PredictionPoint>, minimum: Double?, maximum: Double?): PredictionChartDomain {
    val values = points.filter { it.minutes in 0.0..120.0 }.map { it.value } + listOfNotNull(minimum, maximum)
    if (values.isEmpty()) return PredictionChartDomain(50.0, 180.0)
    val dataMin = values.min(); val dataMax = values.max(); val span = max(dataMax - dataMin, 20.0)
    return PredictionChartDomain(floor((dataMin - max(12.0, span * 0.16)) / 10) * 10, ceil((dataMax + max(18.0, span * 0.24)) / 10) * 10)
}

@Composable
fun PredictionChart(points: List<PredictionPoint>, minimum: Double?, maximum: Double?, modifier: Modifier = Modifier, lineColor: Color = JanuaryColors.Rust, showTargetBand: Boolean = true, summaryValue: Double? = null, summaryDetail: String? = null, summaryDelta: String? = null) {
    val display = points.filter { it.minutes in 0.0..120.0 }.sortedBy { it.minutes }
    val lowerBound = minimum.takeIf { showTargetBand }; val upperBound = maximum.takeIf { showTargetBand }
    val domain = predictionChartDomain(display, lowerBound, upperBound)
    val peak = display.maxByOrNull { it.value }
    Surface(modifier.fillMaxWidth().semantics { contentDescription = "Predicted glucose response chart. " + (peak?.let { "Likely peak ${formatMetricNumber(it.value)} milligrams per deciliter at ${formatMetricNumber(it.minutes)} minutes after the meal" } ?: "No prediction points") }, shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, JanuaryColors.Border.copy(alpha = 0.75f)), shadowElevation = 4.dp) {
        Column {
            Column(Modifier.padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 8.dp)) {
                if (summaryValue != null) {
                    Text("LIKELY PEAK", fontSize = 13.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold, color = JanuaryColors.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(summaryValue.roundToInt().toString(), fontFamily = FontFamily.Monospace, fontSize = 58.sp, lineHeight = 66.sp, fontWeight = FontWeight.Bold, color = lineColor)
                        summaryDetail?.let { Text(it, fontSize = 14.sp, lineHeight = 20.sp, color = JanuaryColors.Body) }
                    }
                    summaryDelta?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = lineColor) }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PREDICTED RESPONSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp, color = JanuaryColors.Muted)
                        Text("mg/dL", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Muted)
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(205.dp)) {
                if (display.isEmpty()) return@Canvas
                val left = 18.dp.toPx()
                fun x(minutes: Double) = left + (minutes / 120 * (size.width - left)).toFloat()
                fun y(value: Double) = ((domain.upper - value) / (domain.upper - domain.lower) * size.height).toFloat()
                if (lowerBound != null && upperBound != null) drawRect(JanuaryColors.TargetBand, Offset(0f, y(upperBound)), Size(size.width, y(lowerBound) - y(upperBound)))
                val path = Path().apply {
                    moveTo(x(display[0].minutes), y(display[0].value))
                    for (i in 0 until display.lastIndex) {
                        val p0 = display[max(0, i - 1)]; val p1 = display[i]; val p2 = display[i + 1]; val p3 = display[min(display.lastIndex, i + 2)]
                        cubicTo(x(p1.minutes) + (x(p2.minutes) - x(p0.minutes)) / 6, y(p1.value) + (y(p2.value) - y(p0.value)) / 6,
                            x(p2.minutes) - (x(p3.minutes) - x(p1.minutes)) / 6, y(p2.value) - (y(p3.value) - y(p1.value)) / 6, x(p2.minutes), y(p2.value))
                    }
                }
                val fill = Path().apply { addPath(path); lineTo(x(display.last().minutes), y(lowerBound ?: domain.lower)); lineTo(x(display.first().minutes), y(lowerBound ?: domain.lower)); close() }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.24f), lineColor.copy(alpha = 0.02f))))
                drawPath(path, lineColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
                val meal = Offset(x(display.first().minutes), y(display.first().value))
                drawCircle(JanuaryColors.Gold, 9.dp.toPx(), meal); drawCircle(JanuaryColors.Ink, 9.dp.toPx(), meal, style = Stroke(2.5.dp.toPx()))
                peak?.let {
                    val position = Offset(x(min(it.minutes, 116.0)), y(it.value))
                    drawCircle(Color.White, 8.dp.toPx(), position); drawCircle(JanuaryColors.Ink, 8.dp.toPx(), position, style = Stroke(2.5.dp.toPx()))
                    if (summaryValue == null) {
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = JanuaryColors.Ink.toArgb(); textSize = 12.sp.toPx(); typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL) }
                        val label = "Likely peak ${it.value.roundToInt()} · +${it.minutes.roundToInt()} min"
                        val labelX = (position.x - paint.measureText(label) / 2).coerceIn(left, max(left, size.width - paint.measureText(label) - 8.dp.toPx()))
                        drawContext.canvas.nativeCanvas.drawText(label, labelX, max(16.sp.toPx(), position.y - 16.dp.toPx()), paint)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(0, 40, 80, 120).forEach { Text(it.toString(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Muted) }
            }
            Row(Modifier.padding(horizontal = 18.dp).padding(top = 10.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartLegend("Prediction", lineColor, 0)
                ChartLegend("Meal", JanuaryColors.Gold, 1)
                if (lowerBound != null && upperBound != null) ChartLegend("Target", JanuaryColors.TargetBand, 2)
                ChartLegend("Peak", Color.White, 1)
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color, kind: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(if (kind == 1) 12.dp else 22.dp, 12.dp)) {
            when (kind) {
                0 -> drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
                1 -> { drawCircle(color); drawCircle(JanuaryColors.Ink, style = Stroke(2.dp.toPx())) }
                else -> drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
        }
        Text(label, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Body)
    }
}
