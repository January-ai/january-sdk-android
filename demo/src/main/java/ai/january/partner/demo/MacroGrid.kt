package ai.january.partner.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MacroValue(val label: String, val value: String, val unit: String)

@Composable
fun MacroGrid(values: List<MacroValue>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            values.take(4).chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    row.forEach { metric ->
                        Column(Modifier.weight(1f).heightIn(min = 64.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(metric.label.uppercase(), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = JanuaryColors.Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(metric.value, fontSize = 20.sp, lineHeight = 26.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Text(metric.unit, fontSize = 15.sp, lineHeight = 20.sp, color = JanuaryColors.Muted)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (values.size > 2) HorizontalDivider(Modifier.align(Alignment.Center), color = JanuaryColors.Divider)
    }
}
