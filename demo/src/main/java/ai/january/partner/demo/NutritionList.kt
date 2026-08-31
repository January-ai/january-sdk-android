package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class NutritionValue(val label: String, val value: String)

@Composable
fun NutritionList(values: List<NutritionValue>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(modifier.fillMaxWidth()) {
        values.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.label, modifier = Modifier.weight(1f))
                Text(item.value, fontFamily = FontFamily.Monospace)
            }
            if (index < values.lastIndex) HorizontalDivider(color = JanuaryColors.Divider)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionListPreview() {
    JanuaryDemoTheme { NutritionList(listOf(NutritionValue("Fiber", "8 g"), NutritionValue("Sodium", "420 mg"))) }
}
