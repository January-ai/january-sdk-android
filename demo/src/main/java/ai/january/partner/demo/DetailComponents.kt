package ai.january.partner.demo

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.january.partner.FoodId
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId
import ai.january.partner.foods.ServingOption
import ai.january.partner.glucose.*
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import java.time.OffsetDateTime
import kotlinx.coroutines.launch

@Composable
internal fun DetailDisclosure(title: String = "Technical details", content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Icon(if (expanded) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight, if (expanded) "Collapse $title" else "Expand $title", Modifier.size(18.dp), tint = JanuaryColors.Muted)
        }
        if (expanded) content()
    }
}

@Composable
internal fun QuantityStepper(quantity: Double, onChange: (Double) -> Unit, minimum: Double = 0.25, maximum: Double = 100.0, step: Double = 0.25) {
    Row(Modifier.clip(RoundedCornerShape(9.dp)).background(JanuaryColors.Control), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((quantity - step).coerceAtLeast(minimum)) }, enabled = quantity > minimum) { Icon(Icons.Outlined.Remove, "Decrease quantity") }
        VerticalDivider(Modifier.height(20.dp), color = JanuaryColors.Border)
        IconButton(onClick = { onChange((quantity + step).coerceAtMost(maximum)) }, enabled = quantity < maximum) { Icon(Icons.Outlined.Add, "Increase quantity") }
    }
}

@Composable
internal fun ServingControls(servings: List<ServingOption>, serving: ServingOption?, quantity: Double, onServing: (ServingOption) -> Unit, onQuantity: (Double) -> Unit, menuItem: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    if (servings.isEmpty()) return
    DemoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (menuItem) SectionLabel("Serving")
            Box {
                Row(Modifier.fillMaxWidth().clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (!menuItem) Text("Serving unit", style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
                        Text(serving?.let(::servingLabel) ?: "Choose a serving", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Green)
                    }
                    Icon(Icons.Outlined.UnfoldMore, "Choose a serving", Modifier.size(18.dp), tint = JanuaryColors.Green)
                }
                DropdownMenu(expanded, { expanded = false }) {
                    servings.forEach { option ->
                        DropdownMenuItem(text = { Text(servingLabel(option)) }, trailingIcon = { if (serving?.id == option.id) Icon(Icons.Outlined.Check, null) }, onClick = { onServing(option); expanded = false })
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantity: ${formatDemoNumber(quantity)} ${serving?.unit.orEmpty()}", Modifier.weight(1f), fontSize = 15.sp, lineHeight = 20.sp)
                QuantityStepper(quantity, onQuantity)
            }
        }
    }
}

@Composable
internal fun DemoInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, singleLine: Boolean = true) {
    TextField(value, onValueChange, modifier.fillMaxWidth().heightIn(min = 54.dp),
        placeholder = { Text(placeholder) }, singleLine = singleLine, shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(focusedContainerColor = JanuaryColors.Control, unfocusedContainerColor = JanuaryColors.Control, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodGlucoseSheet(client: JanuaryPartnerClient, foodId: FoodId, foodName: String, serving: ServingOption, quantity: Double, endUserId: PartnerUserId?, timezone: String, onDismiss: () -> Unit) {
    var result by remember { mutableStateOf<GlucosePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()
    fun predict() {
        loading = true; error = null
        scope.launch {
            runCatching { client.glucose.predict(PredictGlucoseRequest(
                GlucosePredictionProfile(42.0, Sex.FEMALE, Height(66.0, HeightUnit.INCHES), Weight(150.0, WeightUnit.POUNDS)),
                listOf(FoodSelection(foodId.value, ServingSelection(serving.id.value, quantity))), OffsetDateTime.now(), endUserId = endUserId, timezone = timezone,
            )) }.onSuccess { result = it }.onFailure { error = it }
            loading = false
        }
    }
    LaunchedEffect(foodId, serving.id, quantity) { predict() }
    AppModalSheet(title = "Glucose response", onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize().androidxScroll().padding(horizontal = DemoScreenPadding, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(foodName, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("${formatDemoNumber(quantity)} ${serving.unit}", fontSize = 15.sp, color = JanuaryColors.Muted)
            }
            when {
                loading -> DemoCard {
                    Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        LoadingSpinner(color = JanuaryColors.Green)
                        Text("Predicting your glucose response…", style = MaterialTheme.typography.titleMedium)
                        Text("This usually takes a few seconds.", fontSize = 15.sp, color = JanuaryColors.Muted)
                    }
                }
                error != null -> ErrorCard(error!!, ::predict)
                result != null -> FoodGlucoseResult(result!!)
            }
            DemoCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.AccountCircle, null); Text("Demo profile", style = MaterialTheme.typography.titleMedium) }
                Text("42 years · Female · 66 in · 150 lb · No reported condition", fontSize = 15.sp, color = JanuaryColors.Muted)
            }
            Text("This is an estimate for demonstration purposes, not medical advice.", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Modifier.androidxScroll(): Modifier = this.then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))

@Composable
private fun FoodGlucoseResult(result: GlucosePrediction) {
    DemoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.MonitorHeart, null, tint = glucoseImpactColor(result.impact.value))
            Text(glucoseImpactLabel(result.impact.value), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = glucoseImpactColor(result.impact.value))
            Text("Estimated impact", fontSize = 15.sp, color = JanuaryColors.Muted)
        }
    }
    GlucoseChart(result)
    val metrics = listOf("Peak" to result.prediction.maxOfOrNull { it.value }, "Target minimum" to result.minimum, "Target maximum" to result.maximum, "Data points" to result.prediction.size.toDouble())
    metrics.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { (label, value) ->
                DemoCard(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
                    Text((value?.let(::formatMetricNumber) ?: "—") + if (label == "Data points") "" else " mg/dL", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
    DemoCard {
        DetailDisclosure("Prediction data") {
            NutritionList(result.prediction.map { NutritionValue("+${formatMetricNumber(it.minutes)} min", "${formatMetricNumber(it.value)} mg/dL") })
        }
    }
}

internal fun glucoseImpactLabel(impact: String): String = impact.replace('_', ' ').replaceFirstChar(Char::uppercase).let { if (it.endsWith("impact", true)) it else "$it impact" }
internal fun glucoseImpactColor(impact: String): Color = when (impact.lowercase()) { "low", "low_impact" -> JanuaryColors.Green; "medium", "medium_impact" -> JanuaryColors.Gold; else -> JanuaryColors.Rust }
