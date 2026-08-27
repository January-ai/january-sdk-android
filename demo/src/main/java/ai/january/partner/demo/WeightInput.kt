package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal enum class WeightDisplayUnit(val label: String) {
    POUNDS("lb"),
    KILOGRAMS("kg"),
}

internal fun poundsToKilograms(pounds: Double): Double = pounds * 0.45359237

internal fun kilogramsToPounds(kilograms: Double): Double = kilograms / 0.45359237

@Composable
internal fun WeightInput(
    weightPounds: Double,
    onWeightPoundsChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayUnit by rememberSaveable { mutableStateOf(WeightDisplayUnit.POUNDS) }
    val displayedWeight = if (displayUnit == WeightDisplayUnit.POUNDS) {
        weightPounds
    } else {
        poundsToKilograms(weightPounds)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weight", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            SegmentedControl(
                options = WeightDisplayUnit.entries,
                selected = displayUnit,
                label = { it.label },
                onSelect = { displayUnit = it },
                modifier = Modifier.width(180.dp),
            )
        }

        WeightNumberField(
            label = if (displayUnit == WeightDisplayUnit.POUNDS) "Pounds" else "Kilograms",
            value = formatWeight(displayedWeight),
            onValueChange = { value ->
                val pounds = if (displayUnit == WeightDisplayUnit.POUNDS) value else kilogramsToPounds(value)
                onWeightPoundsChange(pounds.coerceIn(60.0, 700.0))
            },
        )
    }
}

private fun formatWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)

@Composable
private fun WeightNumberField(
    label: String,
    value: String,
    onValueChange: (Double) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(value) }
    var isFocused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = value
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
        BasicTextField(
            value = text,
            onValueChange = { candidate ->
                val isNumeric = candidate.length <= 6 &&
                    candidate.all { it.isDigit() || it == '.' } &&
                    candidate.count { it == '.' } <= 1
                if (isNumeric) {
                    text = candidate
                    candidate.toDoubleOrNull()?.let(onValueChange)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(JanuaryColors.Control, RoundedCornerShape(14.dp))
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) text = value
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(
                color = JanuaryColors.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
    }
}
