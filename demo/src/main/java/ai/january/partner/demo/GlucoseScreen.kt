package ai.january.partner.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.glucose.GlucosePrediction
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.Height
import ai.january.partner.glucose.HeightUnit
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    var age by remember { mutableStateOf("42") }
    var sex by remember { mutableStateOf(Sex.FEMALE) }
    var height by remember { mutableStateOf("66") }
    var weight by remember { mutableStateOf("150") }
    var food by remember { mutableStateOf<FoodSearchItem?>(null) }
    var showFoodSearch by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<GlucosePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun predict() {
        val selected = food ?: return
        val serving = selected.servings.firstOrNull() ?: return
        val sdk = client ?: return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                sdk.glucose.predict(
                    PredictGlucoseRequest(
                        userProfile = GlucosePredictionProfile(
                            age = age.toDouble(),
                            sex = sex,
                            height = Height(height.toDouble(), HeightUnit.INCHES),
                            weight = Weight(weight.toDouble(), WeightUnit.POUNDS),
                        ),
                        foods = listOf(FoodSelection(selected.id.value, ServingSelection(serving.id.value, 1.0))),
                        startTime = OffsetDateTime.now(),
                        endUserId = state.partnerUserId,
                        timezone = ZoneId.systemDefault().id,
                    ),
                )
            }.onSuccess { result = it }
                .onFailure { error = it.message ?: "The prediction failed." }
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        DemoTopBar("Glucose", settingsAction)
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (result == null) {
                    SectionLabel("About you")
                    DemoCard {
                        NumberField("Age", age, { age = it }, "years")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            Sex.entries.forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = sex == option,
                                    onClick = { sex = option },
                                    shape = SegmentedButtonDefaults.itemShape(index, Sex.entries.size),
                                    label = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) },
                                )
                            }
                        }
                        NumberField("Height", height, { height = it }, "in")
                        NumberField("Weight", weight, { weight = it }, "lb")
                    }
                    SectionLabel("This meal")
                    DemoCard {
                        if (food == null) {
                            Button(onClick = { showFoodSearch = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Add, contentDescription = null); Text(" Add food")
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(food!!.name, style = MaterialTheme.typography.titleMedium)
                                    Text("1 × ${food!!.servings.firstOrNull()?.unit ?: "serving"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { food = null }) { Icon(Icons.Outlined.Close, contentDescription = "Remove food") }
                            }
                        }
                    }
                    Button(onClick = ::predict, modifier = Modifier.fillMaxWidth(), enabled = food != null && client != null && !loading) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Predict glucose response")
                    }
                    if (client == null) ApiKeyRequiredCard()
                    error?.let { ErrorCard(it, ::predict) }
                } else {
                    PredictionResult(result!!, food)
                    Button(onClick = { result = null }, modifier = Modifier.fillMaxWidth()) { Text("Adjust meal") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showFoodSearch && client != null) {
        GlucoseFoodPicker(
            state = state,
            onDismiss = { showFoodSearch = false },
            onSelect = { food = it; showFoodSearch = false },
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, unit: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> if (next.all { it.isDigit() || it == '.' }) onChange(next) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        suffix = { Text(unit) },
        singleLine = true,
    )
}

@Composable
private fun PredictionResult(result: GlucosePrediction, food: FoodSearchItem?) {
    val peak = result.prediction.maxByOrNull { it.value }
    Text("Estimated response", style = MaterialTheme.typography.headlineMedium)
    Text("${result.impact.value.replaceFirstChar(Char::uppercase)} impact", color = when (result.impact.value) {
        "high" -> MaterialTheme.colorScheme.error
        "medium" -> JanuaryColors.Rust
        else -> MaterialTheme.colorScheme.secondary
    }, fontWeight = FontWeight.Bold)
    DemoCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                SectionLabel("Likely peak")
                Text(peak?.value?.toInt()?.toString() ?: "—", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
            }
            Text("mg/dL", fontWeight = FontWeight.SemiBold)
        }
        GlucoseChart(result)
    }
    food?.let {
        DemoCard {
            SectionLabel("Meal")
            Text(it.name, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun GlucoseChart(result: GlucosePrediction) {
    val lineColor = JanuaryColors.Rust
    val bandColor = MaterialTheme.colorScheme.secondaryContainer
    Column {
        Canvas(Modifier.fillMaxWidth().height(230.dp)) {
            val points = result.prediction
            if (points.isEmpty()) return@Canvas
            val minX = points.minOf { it.minutes }
            val maxX = points.maxOf { it.minutes }.coerceAtLeast(minX + 1)
            val minY = result.chart.min
            val maxY = result.chart.max.coerceAtLeast(minY + 1)
            drawRect(bandColor, topLeft = Offset(0f, size.height * .35f), size = androidx.compose.ui.geometry.Size(size.width, size.height * .35f))
            val path = Path()
            points.forEachIndexed { index, point ->
                val x = ((point.minutes - minX) / (maxX - minX) * size.width).toFloat()
                val y = (size.height - ((point.value - minY) / (maxY - minY) * size.height)).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "40", "80", "120 min").forEach { Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlucoseFoodPicker(state: DemoState, onDismiss: () -> Unit, onSelect: (FoodSearchItem) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodSearchItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun search() {
        val client = state.client ?: return
        if (query.isBlank()) return
        loading = true
        coroutineScope.launch {
            runCatching { client.foods.search(SearchFoodsRequest(query, endUserId = state.partnerUserId)).items }
                .onSuccess { results = it }
                .onFailure { error = it.message ?: "Search failed." }
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add food", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search foods") }, singleLine = true)
            Button(onClick = ::search, modifier = Modifier.fillMaxWidth(), enabled = query.isNotBlank() && !loading) { Text("Search") }
            error?.let { ErrorCard(it, ::search) }
            results.take(6).forEach { item ->
                DemoCard(modifier = Modifier.clickable { onSelect(item) }) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.calories?.let { "${it.toInt()} cal" } ?: "Nutrition unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
