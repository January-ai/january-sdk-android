package ai.january.partner.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class PredictionPoint(val minutes: Double, val value: Double)

@Composable
fun PredictionChart(
    points: List<PredictionPoint>,
    minimum: Double,
    maximum: Double,
    modifier: Modifier = Modifier,
    lineColor: Color = JanuaryColors.Rust,
    showTargetBand: Boolean = true,
) {
    val targetBand = MaterialTheme.colorScheme.secondaryContainer
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(230.dp)) {
            if (points.isEmpty()) return@Canvas
            val minX = points.minOf { it.minutes }
            val maxX = points.maxOf { it.minutes }.coerceAtLeast(minX + 1)
            val minY = minimum
            val maxY = maximum.coerceAtLeast(minY + 1)
            if (showTargetBand) {
                drawRect(targetBand, topLeft = Offset(0f, size.height * .35f), size = Size(size.width, size.height * .35f))
            }
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = ((point.minutes - minX) / (maxX - minX) * size.width).toFloat()
                val y = (size.height - ((point.value - minY) / (maxY - minY) * size.height)).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "40", "80", "120 min").forEach { Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PredictionChartPreview() {
    JanuaryDemoTheme { PredictionChart(listOf(PredictionPoint(0.0, 95.0), PredictionPoint(60.0, 130.0), PredictionPoint(120.0, 105.0)), 80.0, 150.0) }
}
