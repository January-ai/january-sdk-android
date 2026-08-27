package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class MacroValue(val label: String, val value: String, val unit: String)

@Composable
fun MacroGrid(values: List<MacroValue>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.take(4).chunked(2).forEach { rowValues ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { metric ->
                    Surface(
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = JanuaryColors.Control,
                    ) {
                        Column(
                            Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("${metric.value} ${metric.unit}".trim(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(metric.label, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
                        }
                    }
                }
                if (rowValues.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroGridPreview() {
    JanuaryDemoTheme {
        MacroGrid(listOf(MacroValue("Calories", "420", "cal"), MacroValue("Protein", "28", "g"), MacroValue("Carbs", "42", "g"), MacroValue("Fat", "16", "g")))
    }
}
