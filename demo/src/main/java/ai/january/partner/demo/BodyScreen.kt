package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.waterlogs.DailyWaterTotal
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.waterlogs.WaterAmount
import ai.january.partner.weightlogs.DailyWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Water intake and body weight: log one entry, browse a range of daily totals, delete the last water entry. */
@Composable
fun BodyScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val userClient = state.userClient
    val coroutineScope = rememberCoroutineScope()
    var span by remember { mutableStateOf(FoodLogTimeSpan.CURRENT_WEEK) }
    val range = span.dateRange(timezone = state.timezone)

    var waterUnit by rememberSaveable { mutableStateOf(VolumeUnit.FL_OZ) }
    var waterText by rememberSaveable { mutableStateOf("8") }
    var waterDays by remember { mutableStateOf<List<DailyWaterTotal>>(emptyList()) }
    var waterLoading by remember { mutableStateOf(false) }
    var waterSaving by remember { mutableStateOf(false) }
    var waterError by remember { mutableStateOf<Throwable?>(null) }
    var lastWaterLogId by remember { mutableStateOf<String?>(null) }
    var lastWaterLogged by remember { mutableStateOf<WaterAmount?>(null) }

    var weightUnit by rememberSaveable { mutableStateOf(WeightUnit.POUNDS) }
    var weightText by rememberSaveable { mutableStateOf("150") }
    var weightDays by remember { mutableStateOf<List<DailyWeight>>(emptyList()) }
    var weightLoading by remember { mutableStateOf(false) }
    var weightSaving by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf<Throwable?>(null) }
    var lastWeightLogged by remember { mutableStateOf<Weight?>(null) }

    fun loadWater() {
        val sdk = userClient ?: return
        waterLoading = true
        waterError = null
        coroutineScope.launch {
            try {
                waterDays = sdk.waterLogs.list(range.apiStart, range.apiEnd, waterUnit).items
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                waterError = failure
            } finally {
                waterLoading = false
            }
        }
    }

    fun loadWeight() {
        val sdk = userClient ?: return
        weightLoading = true
        weightError = null
        coroutineScope.launch {
            try {
                weightDays = sdk.weightLogs.list(range.apiStart, range.apiEnd).items
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                weightError = failure
            } finally {
                weightLoading = false
            }
        }
    }

    fun logWater() {
        val sdk = userClient ?: return
        val value = waterText.toDoubleOrNull() ?: return
        waterSaving = true
        waterError = null
        coroutineScope.launch {
            try {
                val log = sdk.waterLogs.create(WaterAmount(value, waterUnit))
                lastWaterLogId = log.id
                lastWaterLogged = log.amount
                loadWater()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                waterError = failure
            } finally {
                waterSaving = false
            }
        }
    }

    fun deleteLastWater() {
        val sdk = userClient ?: return
        val id = lastWaterLogId ?: return
        waterSaving = true
        waterError = null
        coroutineScope.launch {
            try {
                sdk.waterLogs.delete(id)
                lastWaterLogId = null
                lastWaterLogged = null
                loadWater()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                waterError = failure
            } finally {
                waterSaving = false
            }
        }
    }

    fun logWeight() {
        val sdk = userClient ?: return
        val value = weightText.toDoubleOrNull() ?: return
        weightSaving = true
        weightError = null
        coroutineScope.launch {
            try {
                lastWeightLogged = sdk.weightLogs.create(Weight(value, weightUnit)).weight
                loadWeight()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                weightError = failure
            } finally {
                weightSaving = false
            }
        }
    }

    LaunchedEffect(userClient, span, waterUnit) {
        waterDays = emptyList()
        weightDays = emptyList()
        waterError = null
        weightError = null
        if (userClient != null) {
            loadWater()
            loadWeight()
        }
    }

    AppScreenScaffold(
        title = "Body", modifier = modifier.testTag("body-screen"), style = AppNavigationTitleStyle.Leading,
        trailing = { AppNavigationButton(AppNavigationButtonKind.Settings, testTag = "settings-button", onClick = settingsAction) },
    ) {
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MealWorkflowGuide(
                    title = "Track water and weight",
                    message = "Water logs are capped at 24 L per day and listed as one total per day. Weight logs keep every measurement and list the latest one per day.",
                    steps = listOf("Identify the user who owns the logs", "Log an amount of water or a weight", "Browse daily totals for a date range"),
                    icon = Icons.Outlined.WaterDrop,
                )
                SectionLabel("User identity")
                FoodLogUserCard(
                    userId = state.partnerUserId?.value,
                    modifier = Modifier.testTag("body-user-card"),
                    timezone = state.timezone,
                    onSave = { state.endUserId = it },
                    onSettings = settingsAction,
                )
                if (userClient != null) {
                    SectionLabel("Water")
                    DemoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            UnitRow("Amount", VolumeUnit.entries, waterUnit, { if (it == VolumeUnit.FL_OZ) "fl oz" else "ml" }, { waterUnit = it }) {
                                if (it == VolumeUnit.FL_OZ) "water-unit-fl-oz" else "water-unit-ml"
                            }
                            NumberField(waterText, { waterText = it }, "water-amount")
                            DemoPrimaryButton(
                                "Log water", ::logWater, Modifier.fillMaxWidth().testTag("water-log"),
                                enabled = !waterSaving && waterText.toDoubleOrNull() != null, loading = waterSaving,
                            )
                            lastWaterLogged?.let {
                                Text("Logged ${formatBodyNumber(it.value)} ${it.unit.label()}", Modifier.testTag("water-logged"), color = JanuaryColors.Green, fontWeight = FontWeight.SemiBold)
                            }
                            if (lastWaterLogId != null) {
                                DemoSecondaryButton("Delete last water log", ::deleteLastWater, Modifier.fillMaxWidth().testTag("water-delete-last"))
                            }
                        }
                    }
                    SectionLabel("Daily totals")
                    DemoCard {
                        SegmentedControl(
                            FoodLogTimeSpan.entries, span, { it.title }, { span = it },
                            testTag = {
                                when (it) {
                                    FoodLogTimeSpan.TODAY -> "body-range-today"
                                    FoodLogTimeSpan.CURRENT_WEEK -> "body-range-week"
                                    FoodLogTimeSpan.LAST_MONTH -> "body-range-month"
                                }
                            },
                        )
                        Text(range.displayText(), fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = JanuaryColors.Muted)
                    }
                    DemoPrimaryButton("Refresh", { loadWater(); loadWeight() }, Modifier.fillMaxWidth().testTag("body-refresh"), enabled = !waterLoading && !weightLoading)
                    if (waterLoading && waterDays.isEmpty()) {
                        Row(Modifier.padding(16.dp).testTag("water-loading"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LoadingSpinner(color = JanuaryColors.Green)
                            Text("Loading water totals…", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Muted)
                        }
                    }
                    waterError?.let { ErrorCard(it, ::loadWater, testTag = "water-error", retryTestTag = "water-retry") }
                    waterDays.forEachIndexed { index, day ->
                        DailyRow(Icons.Outlined.WaterDrop, day.date, "${formatBodyNumber(day.total.value)} ${day.total.unit.label()}", Modifier.testTag("water-day-$index"))
                    }
                    if (!waterLoading && waterError == null && waterDays.isEmpty()) {
                        EmptyStateCard(Icons.Outlined.WaterDrop, "No water logged in this range", "Log an amount above, then it appears here as part of that day's total.", Modifier.testTag("water-empty"))
                    }

                    SectionLabel("Weight")
                    DemoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            UnitRow("Weight", WeightUnit.entries, weightUnit, { it.value }, { weightUnit = it }) {
                                if (it == WeightUnit.POUNDS) "weight-unit-lb" else "weight-unit-kg"
                            }
                            NumberField(weightText, { weightText = it }, "weight-value")
                            DemoPrimaryButton(
                                "Log weight", ::logWeight, Modifier.fillMaxWidth().testTag("weight-log"),
                                enabled = !weightSaving && weightText.toDoubleOrNull() != null, loading = weightSaving,
                            )
                            lastWeightLogged?.let {
                                Text("Logged ${formatBodyNumber(it.value)} ${it.unit.value}", Modifier.testTag("weight-logged"), color = JanuaryColors.Green, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (weightLoading && weightDays.isEmpty()) {
                        Row(Modifier.padding(16.dp).testTag("weight-loading"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LoadingSpinner(color = JanuaryColors.Green)
                            Text("Loading weights…", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Muted)
                        }
                    }
                    weightError?.let { ErrorCard(it, ::loadWeight, testTag = "weight-error", retryTestTag = "weight-retry") }
                    weightDays.forEachIndexed { index, day ->
                        DailyRow(Icons.Outlined.MonitorWeight, day.date, "${formatBodyNumber(day.weight.value)} ${day.weight.unit.value}", Modifier.testTag("weight-day-$index"))
                    }
                    if (!weightLoading && weightError == null && weightDays.isEmpty()) {
                        EmptyStateCard(Icons.Outlined.MonitorWeight, "No weight logged in this range", "Log a weight above; each day shows its latest measurement.", Modifier.testTag("weight-empty"))
                    }
                }
                if (client == null) AuthenticationRequiredCard()
            }
        }
    }
}

@Composable
private fun <T> UnitRow(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, testTag: (T) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        SegmentedControl(options, selected, text, onSelect, Modifier.width(170.dp), testTag)
    }
}

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, testTag: String) {
    BasicTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= 7 && candidate.all { it.isDigit() || it == '.' } && candidate.count { it == '.' } <= 1) onValueChange(candidate)
        },
        modifier = Modifier.fillMaxWidth().background(JanuaryColors.Control, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 12.dp).testTag(testTag),
        textStyle = TextStyle(color = JanuaryColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}

@Composable
private fun DailyRow(icon: androidx.compose.ui.graphics.vector.ImageVector, date: String, value: String, modifier: Modifier = Modifier) {
    DemoCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.Icon(icon, null, tint = JanuaryColors.Green)
            Text(formatBodyDate(date), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(Modifier.padding(top = 4.dp), color = androidx.compose.ui.graphics.Color.Transparent)
    }
}

private fun VolumeUnit.label(): String = if (this == VolumeUnit.FL_OZ) "fl oz" else "ml"

private fun formatBodyDate(value: String): String =
    runCatching { LocalDate.parse(value).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }.getOrDefault(value)

private fun formatBodyNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(java.util.Locale.US, value)
