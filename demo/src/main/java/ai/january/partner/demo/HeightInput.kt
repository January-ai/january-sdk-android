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
import kotlin.math.roundToInt

internal enum class HeightDisplayUnit(val label: String) {
    IMPERIAL("ft + in"),
    METRIC("cm"),
}

internal data class FeetAndInches(val feet: Int, val inches: Int)

internal fun feetAndInches(heightInches: Double): FeetAndInches {
    val rounded = heightInches.roundToInt()
    return FeetAndInches(feet = rounded / 12, inches = rounded % 12)
}

internal fun inchesToCentimeters(heightInches: Double): Double = heightInches * 2.54

internal fun centimetersToInches(centimeters: Double): Double = centimeters / 2.54

@Composable
internal fun HeightInput(
    heightInches: Double,
    onHeightInchesChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayUnit by rememberSaveable { mutableStateOf(HeightDisplayUnit.IMPERIAL) }
    val imperial = feetAndInches(heightInches)

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Height", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            SegmentedControl(
                options = HeightDisplayUnit.entries,
                selected = displayUnit,
                label = { it.label },
                onSelect = { displayUnit = it },
                modifier = Modifier.width(180.dp),
            )
        }

        if (displayUnit == HeightDisplayUnit.IMPERIAL) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HeightNumberField(
                    label = "Feet",
                    value = imperial.feet,
                    onValueChange = { feet ->
                        onHeightInchesChange((feet * 12 + imperial.inches).toDouble().coerceIn(36.0, 96.0))
                    },
                    modifier = Modifier.weight(1f),
                )
                HeightNumberField(
                    label = "Inches",
                    value = imperial.inches,
                    onValueChange = { inches ->
                        onHeightInchesChange((imperial.feet * 12 + inches).toDouble().coerceIn(36.0, 96.0))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            HeightNumberField(
                label = "Centimeters",
                value = inchesToCentimeters(heightInches).roundToInt(),
                onValueChange = { centimeters ->
                    onHeightInchesChange(centimetersToInches(centimeters.toDouble()).coerceIn(36.0, 96.0))
                },
            )
        }
    }
}

@Composable
private fun HeightNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf(value.toString()) }
    var isFocused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = value.toString()
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
        BasicTextField(
            value = text,
            onValueChange = { candidate ->
                if (candidate.length <= 3 && candidate.all(Char::isDigit)) {
                    text = candidate
                    candidate.toIntOrNull()?.let(onValueChange)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .background(JanuaryColors.Control, RoundedCornerShape(14.dp))
                .onFocusChanged {
                    isFocused = it.isFocused
                    if (!it.isFocused) text = value.toString()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            textStyle = TextStyle(
                color = JanuaryColors.Ink,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}
