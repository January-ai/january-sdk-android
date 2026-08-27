package ai.january.partner.demo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.january.partner.foods.ServingOption
import ai.january.partner.glucose.GlucosePrediction
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.Height
import ai.january.partner.glucose.HeightUnit
import ai.january.partner.glucose.MedicalCondition
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val GoldText = androidx.compose.ui.graphics.Color(0xFF6E5613)

internal fun numericText(value: String): String = value.filter { it.isDigit() || it == '.' }

@Composable
internal fun FormSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(title, Modifier.padding(horizontal = 6.dp))
        DemoCard { content() }
    }
}

@Composable
internal fun MeasurementRow(label: String, value: String, onChange: (String) -> Unit, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.width(72.dp),
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
        Text(unit, modifier = Modifier.padding(start = 10.dp), color = JanuaryColors.Muted)
    }
}

@Composable
internal fun StartTimeRow(startTime: OffsetDateTime, onChange: (OffsetDateTime) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Start time", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        TextButton(
            contentPadding = PaddingValues(0.dp),
            onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onChange(startTime.with(LocalDate.of(year, month + 1, day))) },
                startTime.year,
                startTime.monthValue - 1,
                startTime.dayOfMonth,
            ).show()
        }) {
            Text(
                startTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                modifier = Modifier.background(JanuaryColors.Control, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                color = JanuaryColors.Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        TextButton(
            contentPadding = PaddingValues(0.dp),
            onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onChange(startTime.withHour(hour).withMinute(minute)) },
                startTime.hour,
                startTime.minute,
                false,
            ).show()
        }) {
            Text(
                startTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                modifier = Modifier.background(JanuaryColors.Control, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
                color = JanuaryColors.Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun SelectedFoodRow(
    selected: DemoSelectedFood,
    onServingChange: (ServingOption) -> Unit,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selected.food.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove ${selected.food.name}")
                }
            }
            Box {
                TextButton(onClick = { menuOpen = true }) {
                    Text("${formatDemoNumber(selected.serving.quantity)} ${selected.serving.unit}", color = JanuaryColors.Muted)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    selected.food.servings.forEach { serving ->
                        DropdownMenuItem(
                            text = { Text("${formatDemoNumber(serving.quantity)} ${serving.unit}") },
                            onClick = { onServingChange(serving); menuOpen = false },
                        )
                    }
                }
            }
        }
        DemoQuantityButton("−", false) { onQuantityChange(selected.quantity - 0.25) }
        Text(
            formatDemoNumber(selected.quantity),
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
        )
        DemoQuantityButton("+", true) { onQuantityChange(selected.quantity + 0.25) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConditionsSheet(
    selected: Set<MedicalCondition>,
    onChange: (Set<MedicalCondition>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JanuaryColors.Paper) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppModalHeader(title = "Health conditions", onDismiss = onDismiss)
            Text("Select all that apply. Leave both unselected if neither condition applies.", color = JanuaryColors.Muted)
            DemoCard {
                ConditionRow("Type 2 diabetes", MedicalCondition.TYPE_2_DIABETES, selected, onChange)
                HorizontalDivider(color = JanuaryColors.Divider)
                ConditionRow("Prediabetes", MedicalCondition.PREDIABETES, selected, onChange)
            }
        }
    }
}

@Composable
internal fun ConditionRow(
    label: String,
    condition: MedicalCondition,
    selected: Set<MedicalCondition>,
    onChange: (Set<MedicalCondition>) -> Unit,
) {
    val isSelected = condition in selected
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            onChange(if (isSelected) selected - condition else selected + condition)
        }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Icon(
            if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = if (isSelected) JanuaryColors.Green else JanuaryColors.Border,
        )
    }
}

@Composable
internal fun GlucosePredictionResult(result: GlucosePrediction, foods: List<DemoSelectedFood>) {
    val peak = result.prediction.maxByOrNull { it.value }
    val mealStart = result.prediction.minByOrNull { it.minutes }
    val delta = ((peak?.value ?: 0.0) - (mealStart?.value ?: 0.0)).coerceAtLeast(0.0)
    val impactColor = if (result.impact.value == "low") JanuaryColors.Green else JanuaryColors.Rust
    Text("Estimated response", style = MaterialTheme.typography.displaySmall)
    Text(
        "${result.impact.value.replaceFirstChar(Char::uppercase)} impact",
        color = impactColor,
        fontWeight = FontWeight.Bold,
    )
    DemoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                SectionLabel("Likely peak")
                Text(
                    peak?.value?.toInt()?.toString() ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text("mg/dL", fontWeight = FontWeight.SemiBold)
        }
        GlucoseChart(result)
    }
    DemoCard {
        foods.forEachIndexed { index, food ->
            if (index > 0) HorizontalDivider(color = JanuaryColors.Divider)
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(food.food.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatDemoNumber(food.serving.quantity)} ${food.serving.unit} · quantity ${formatDemoNumber(food.quantity)}",
                        color = JanuaryColors.Muted,
                    )
                }
                if (index == 0) {
                    Text("+${delta.toInt()}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = impactColor)
                }
            }
        }
    }
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = JanuaryColors.GoldContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Worth knowing")
            Text("This estimate reflects the foods, servings, and profile entered above. Adjusting the meal will generate a new prediction.")
        }
    }
    Text(
        "This is a prediction, not a medical recommendation.",
        style = MaterialTheme.typography.bodySmall,
        color = JanuaryColors.Muted,
    )
}

@Composable
internal fun GlucoseChart(result: GlucosePrediction) {
    val lineColor = if (result.impact.value == "low") JanuaryColors.Green else JanuaryColors.Rust
    PredictionChart(
        points = result.prediction.map { PredictionPoint(it.minutes.toDouble(), it.value) },
        minimum = result.chart.min,
        maximum = result.chart.max,
        lineColor = lineColor,
    )
}
