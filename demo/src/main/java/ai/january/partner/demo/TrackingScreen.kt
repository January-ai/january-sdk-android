package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.cancelAndJoin
import ai.january.partner.PartnerUserContext
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.FoodLogSummary
import ai.january.partner.foodlogs.LoggedFood
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.models.NutrientAmount
import ai.january.partner.waterlogs.DailyWaterTotal
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.weightlogs.DailyWeight
import ai.january.partner.waterlogs.Volume
import ai.january.partner.waterlogs.WaterAmount
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The Tracking tab, one day at a time: the food logs with the day's nutrient totals, the
 * day's water total, and the day's weight, with actions to add to each.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    val zone = remember(state.timezone) { runCatching { ZoneId.of(state.timezone) }.getOrDefault(ZoneId.systemDefault()) }
    val today = remember(zone) { LocalDate.now(zone) }
    var day by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDay = LocalDate.parse(day)
    val isToday = selectedDay == today
    var logs by remember { mutableStateOf<List<FoodLog>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var summary by remember { mutableStateOf<FoodLogSummary?>(null) }
    var summaryError by remember { mutableStateOf<Throwable?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<FoodLog?>(null) }
    val userContext = state.partnerContext
    val userClient = state.userClient
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var waterUnit by rememberSaveable { mutableStateOf(VolumeUnit.FL_OZ) }
    var waterText by rememberSaveable { mutableStateOf("8") }
    var waterTotal by remember { mutableStateOf<Volume?>(null) }
    var waterLoading by remember { mutableStateOf(false) }
    var waterSaving by remember { mutableStateOf(false) }
    var waterError by remember { mutableStateOf<Throwable?>(null) }
    var lastWaterLogId by remember { mutableStateOf<String?>(null) }
    var lastWaterLogged by remember { mutableStateOf<WaterAmount?>(null) }

    var weightUnit by rememberSaveable { mutableStateOf(WeightUnit.POUNDS) }
    var weightText by rememberSaveable { mutableStateOf("150") }
    var dayWeight by remember { mutableStateOf<Weight?>(null) }
    var weightLoading by remember { mutableStateOf(false) }
    var weightSaving by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf<Throwable?>(null) }
    var lastWeightLogged by remember { mutableStateOf<Weight?>(null) }

    // The history charts always end today, whichever day the day picker shows.
    var waterRange by rememberSaveable { mutableStateOf(ChartRange.WEEK) }
    var waterHistory by remember { mutableStateOf<List<DailyWaterTotal>?>(null) }
    var waterHistoryLoading by remember { mutableStateOf(false) }
    var waterHistoryError by remember { mutableStateOf<Throwable?>(null) }
    var waterHistoryJob by remember { mutableStateOf<Job?>(null) }
    var weightRange by rememberSaveable { mutableStateOf(ChartRange.WEEK) }
    var weightHistory by remember { mutableStateOf<List<DailyWeight>?>(null) }
    var weightHistoryLoading by remember { mutableStateOf(false) }
    var weightHistoryError by remember { mutableStateOf<Throwable?>(null) }
    var weightHistoryJob by remember { mutableStateOf<Job?>(null) }

    /** Entries logged for a day other than today are dated noon, local time, so they land on that day. */
    fun entryTimestamp(): String? = if (isToday) null else selectedDay.atTime(12, 0).atZone(zone).toOffsetDateTime().toString()

    fun loadFoodLogs() {
        loadJob?.cancel()
        val sdk = userClient ?: return
        loading = true
        error = null
        loadJob = coroutineScope.launch {
            try {
                logs = sdk.foodLogs.list(day, day).items
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure
            } finally {
                loading = false
            }
        }
    }

    fun loadSummary() {
        val sdk = userClient ?: return
        summaryError = null
        coroutineScope.launch {
            try {
                summary = sdk.foodLogs.getSummary(day, day)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                summary = null
                summaryError = failure
            }
        }
    }

    fun loadWater() {
        val sdk = userClient ?: return
        waterLoading = true
        waterError = null
        coroutineScope.launch {
            try {
                waterTotal = sdk.waterLogs.list(day, day, waterUnit).items.firstOrNull { it.date == day }?.total
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
                dayWeight = sdk.weightLogs.list(day, day).items.firstOrNull { it.date == day }?.weight
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                weightError = failure
            } finally {
                weightLoading = false
            }
        }
    }

    fun loadWaterHistory() {
        val sdk = userClient ?: return
        val span = chartSpan(waterRange, today)
        val unit = waterUnit
        waterHistoryJob?.cancel()
        waterHistoryLoading = true
        waterHistoryError = null
        waterHistoryJob = coroutineScope.launch {
            try {
                waterHistory = fetchInChunks(span, { it.date }) { chunk -> sdk.waterLogs.list(chunk.start.toString(), chunk.end.toString(), unit).items }
                waterHistoryLoading = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                waterHistory = null
                waterHistoryError = failure
                waterHistoryLoading = false
            }
        }
    }

    fun loadWeightHistory() {
        val sdk = userClient ?: return
        val span = chartSpan(weightRange, today)
        weightHistoryJob?.cancel()
        weightHistoryLoading = true
        weightHistoryError = null
        weightHistoryJob = coroutineScope.launch {
            try {
                weightHistory = fetchInChunks(span, { it.date }) { chunk -> sdk.weightLogs.list(chunk.start.toString(), chunk.end.toString()).items }
                weightHistoryLoading = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                weightHistory = null
                weightHistoryError = failure
                weightHistoryLoading = false
            }
        }
    }

    fun load() {
        loadFoodLogs()
        loadSummary()
        loadWater()
        loadWeight()
    }

    fun logWater() {
        val sdk = userClient ?: return
        val value = waterText.toDoubleOrNull() ?: return
        waterSaving = true
        waterError = null
        coroutineScope.launch {
            try {
                val log = sdk.waterLogs.create(WaterAmount(value, waterUnit), entryTimestamp())
                lastWaterLogId = log.id
                lastWaterLogged = log.amount
                loadWater()
                loadWaterHistory()
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
                loadWaterHistory()
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
                lastWeightLogged = sdk.weightLogs.create(Weight(value, weightUnit), entryTimestamp()).weight
                loadWeight()
                loadWeightHistory()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                weightError = failure
            } finally {
                weightSaving = false
            }
        }
    }

    LaunchedEffect(userContext, client, day) {
        loadJob?.cancelAndJoin()
        logs = emptyList()
        summary = null
        waterTotal = null
        dayWeight = null
        lastWaterLogId = null
        lastWaterLogged = null
        lastWeightLogged = null
        error = null
        summaryError = null
        waterError = null
        weightError = null
        loading = false
        if (userClient != null) load()
    }
    LaunchedEffect(waterUnit) { if (userClient != null) loadWater() }
    LaunchedEffect(userContext, client, waterRange, waterUnit) {
        waterHistory = null
        if (userClient != null) loadWaterHistory()
    }
    LaunchedEffect(userContext, client, weightRange) {
        weightHistory = null
        if (userClient != null) loadWeightHistory()
    }

    if (selectedLog != null && userContext != null && client != null) {
        FoodLogDetailScreen(
            state = state,
            log = selectedLog!!,
            user = userContext,
            onDismiss = { selectedLog = null },
            onChanged = { selectedLog = null; load() },
            modifier = modifier,
        )
        return
    }

    AppScreenScaffold(
        title = "Tracking", modifier = modifier.testTag("tracking-screen"), style = AppNavigationTitleStyle.Leading,
        trailing = {
            if (userContext != null && client != null) {
                AppNavigationButton(AppNavigationButtonKind.Add, title = "Add food log", testTag = "food-log-add", onClick = { showEditor = true })
            }
            AppNavigationButton(AppNavigationButtonKind.Settings, testTag = "settings-button", onClick = settingsAction)
        },
    ) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = { load(); loadWaterHistory(); loadWeightHistory() },
            modifier = Modifier.fillMaxSize(),
        ) {
            DemoScreen {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MealWorkflowGuide(
                        title = "Track one day at a time",
                        message = "Each day shows the user's food logs with their nutrient totals, the day's water total, and the day's weight.",
                        steps = listOf("Identify the user who owns the logs", "Pick a day, then log meals, water, or a weight", "Move between days to see the user's history"),
                        icon = Icons.Outlined.Assignment,
                    )
                    SectionLabel("User identity")
                    FoodLogUserCard(
                        userId = state.partnerUserId?.value,
                        modifier = Modifier.testTag("food-log-user-card"),
                        timezone = state.timezone,
                        onSave = { state.endUserId = it },
                        onSettings = settingsAction,
                    )
                    if (userContext != null) {
                        SectionLabel("Day")
                        DaySelector(selectedDay, isToday, onPrevious = { day = selectedDay.minusDays(1).toString() }, onNext = { day = selectedDay.plusDays(1).toString() }, onToday = { day = today.toString() })
                        DemoPrimaryButton("Create a food log", { showEditor = true }, Modifier.fillMaxWidth().testTag("food-log-create"), enabled = client != null,
                            icon = { Icon(Icons.Outlined.Add, null) })
                        SectionLabel("Food logs")
                        DayTotalsCard(summary, summaryError, ::loadSummary)
                        DemoPrimaryButton("Refresh food logs", ::load, Modifier.fillMaxWidth().testTag("food-logs-refresh"), enabled = userClient != null && !loading, loading = loading && logs.isEmpty())
                        if (loading && logs.isEmpty()) {
                            Row(Modifier.padding(16.dp).testTag("food-logs-loading"), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                LoadingSpinner(color = JanuaryColors.Green)
                                Text("Loading food logs…", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Muted)
                            }
                        }
                        error?.let { ErrorCard(it, ::loadFoodLogs, testTag = "food-logs-error", retryTestTag = "food-logs-retry") }
                        logs.forEachIndexed { index, log -> FoodLogRow(log, Modifier.testTag("food-log-$index")) { selectedLog = log } }
                        if (!loading && error == null && logs.isEmpty()) {
                            EmptyStateCard(
                                Icons.Outlined.Assignment,
                                "No food logs on this day",
                                "Create a log, add one or more foods to the meal, then save it for this user.",
                                Modifier.testTag("food-logs-empty"),
                            )
                        }

                        SectionLabel("Water")
                        DemoCard(Modifier.testTag("water-card")) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.WaterDrop, null, tint = JanuaryColors.Green)
                                    Text(if (isToday) "Today's total" else "Day's total", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    if (waterLoading) LoadingSpinner(Modifier.testTag("water-loading"), JanuaryColors.Green)
                                    else Text(
                                        waterTotal?.let { "${formatLogNumber(it.value)} ${it.unit.label()}" } ?: "Nothing logged",
                                        Modifier.testTag(if (waterTotal == null) "water-empty" else "water-total"),
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                UnitRow("Amount", VolumeUnit.entries, waterUnit, { it.label() }, { waterUnit = it }) {
                                    when (it) {
                                        VolumeUnit.FL_OZ -> "water-unit-fl-oz"
                                        VolumeUnit.ML -> "water-unit-ml"
                                        VolumeUnit.CUP -> "water-unit-cup"
                                    }
                                }
                                LogNumberField(waterText, { waterText = it }, "water-amount")
                                DemoPrimaryButton(
                                    "Log water", ::logWater, Modifier.fillMaxWidth().testTag("water-log"),
                                    enabled = !waterSaving && waterText.toDoubleOrNull() != null, loading = waterSaving,
                                )
                                lastWaterLogged?.let {
                                    Text("Logged ${formatLogNumber(it.value)} ${it.unit.label()}", Modifier.testTag("water-logged"), color = JanuaryColors.Green, fontWeight = FontWeight.SemiBold)
                                }
                                if (lastWaterLogId != null) {
                                    DemoSecondaryButton("Delete last water log", ::deleteLastWater, Modifier.fillMaxWidth().testTag("water-delete-last"))
                                }
                                val waterSpan = chartSpan(waterRange, today)
                                val bars = waterHistory?.let { items ->
                                    waterBars(waterRange, waterSpan, items.associate { LocalDate.parse(it.date) to it.total.value })
                                }
                                TrackingChartSection(
                                    idPrefix = "water-chart", range = waterRange, onRange = { waterRange = it },
                                    loading = waterHistoryLoading && waterHistory == null, failed = waterHistoryError != null,
                                    isEmpty = bars == null || bars.none { it.value > 0 }, emptyText = "No water logged in this range",
                                    headline = bars?.let { "${formatChartNumber(it.sumOf { bar -> bar.value })} ${waterUnit.label()} total" },
                                ) { WaterBarChart(bars.orEmpty(), waterRange, waterUnit.label()) }
                            }
                        }
                        waterError?.let { ErrorCard(it, ::loadWater, testTag = "water-error", retryTestTag = "water-retry") }
                        waterHistoryError?.let { ErrorCard(it, ::loadWaterHistory, testTag = "water-chart-error", retryTestTag = "water-chart-retry") }

                        SectionLabel("Weight")
                        DemoCard(Modifier.testTag("weight-card")) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Outlined.MonitorWeight, null, tint = JanuaryColors.Green)
                                    Text(if (isToday) "Today's weight" else "Day's weight", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    if (weightLoading) LoadingSpinner(Modifier.testTag("weight-loading"), JanuaryColors.Green)
                                    else Text(
                                        dayWeight?.let { "${formatLogNumber(it.value)} ${it.unit.value}" } ?: "Nothing logged",
                                        Modifier.testTag(if (dayWeight == null) "weight-empty" else "weight-day"),
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                UnitRow("Weight", WeightUnit.entries, weightUnit, { it.value }, { weightUnit = it }) {
                                    if (it == WeightUnit.POUNDS) "weight-unit-lb" else "weight-unit-kg"
                                }
                                LogNumberField(weightText, { weightText = it }, "weight-value")
                                DemoPrimaryButton(
                                    "Log weight", ::logWeight, Modifier.fillMaxWidth().testTag("weight-log"),
                                    enabled = !weightSaving && weightText.toDoubleOrNull() != null, loading = weightSaving,
                                )
                                lastWeightLogged?.let {
                                    Text("Logged ${formatLogNumber(it.value)} ${it.unit.value}", Modifier.testTag("weight-logged"), color = JanuaryColors.Green, fontWeight = FontWeight.SemiBold)
                                }
                                val weightSpan = chartSpan(weightRange, today)
                                val points = weightHistory?.let { items -> weightPoints(items.map { LocalDate.parse(it.date) to it.weight }, weightUnit) }.orEmpty()
                                TrackingChartSection(
                                    idPrefix = "weight-chart", range = weightRange, onRange = { weightRange = it },
                                    loading = weightHistoryLoading && weightHistory == null, failed = weightHistoryError != null,
                                    isEmpty = points.isEmpty(), emptyText = "No weight logged in this range",
                                    headline = points.lastOrNull()?.let { "Latest ${formatChartNumber(it.value)} ${weightUnit.value}" },
                                ) { WeightLineChart(points, weightSpan, weightRange, weightUnit.value) }
                            }
                        }
                        weightError?.let { ErrorCard(it, ::loadWeight, testTag = "weight-error", retryTestTag = "weight-retry") }
                        weightHistoryError?.let { ErrorCard(it, ::loadWeightHistory, testTag = "weight-chart-error", retryTestTag = "weight-chart-retry") }
                    }
                    if (client == null) AuthenticationRequiredCard()
                }
            }
        }

    }
    if (showEditor && userContext != null) {
        FoodLogEditorSheet(state, userContext, initialTimestamp = if (isToday) null else selectedDay.atTime(12, 0).atZone(zone).toOffsetDateTime(), onDismiss = { showEditor = false }, onSaved = ::load)
    }
}

@Composable
private fun DaySelector(day: LocalDate, isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    FoodLogCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.testTag("logs-day-previous")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous day") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (isToday) "Today" else day.format(DateTimeFormatter.ofPattern("EEEE")), style = MaterialTheme.typography.titleMedium)
                Text(day.toString(), Modifier.testTag("logs-day-label"), fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = JanuaryColors.Muted)
            }
            IconButton(onClick = onNext, modifier = Modifier.testTag("logs-day-next")) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next day") }
        }
        if (!isToday) {
            TextButton(onClick = onToday, modifier = Modifier.testTag("logs-day-today")) { Text("Back to today", fontWeight = FontWeight.SemiBold, color = JanuaryColors.Green) }
        }
    }
}

@Composable
private fun DayTotalsCard(summary: FoodLogSummary?, error: Throwable?, retry: () -> Unit) {
    if (error != null) {
        ErrorCard(error, retry, testTag = "food-logs-summary-error", retryTestTag = "food-logs-summary-retry")
        return
    }
    val bucket = summary?.buckets?.firstOrNull()
    FoodLogCard(Modifier.testTag("food-logs-totals")) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Day totals", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    bucket?.let { "${it.logsCount} log${if (it.logsCount == 1) "" else "s"}" } ?: "—",
                    Modifier.testTag("food-logs-totals-count"), fontFamily = FontFamily.Monospace, color = JanuaryColors.Muted,
                )
            }
            val nutrients = bucket?.nutrients
            FoodLogMacros(listOf("Calories" to nutrients?.calories, "Protein" to nutrients?.protein, "Carbs" to nutrients?.carbohydrates, "Fat" to nutrients?.totalFat))
        }
    }
}

@Composable
private fun <T> UnitRow(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, testTag: (T) -> String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        SegmentedControl(options, selected, text, onSelect, Modifier.width(if (options.size > 2) 210.dp else 170.dp), testTag = testTag)
    }
}

@Composable
private fun LogNumberField(value: String, onValueChange: (String) -> Unit, testTag: String) {
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

private fun VolumeUnit.label(): String = when (this) {
    VolumeUnit.FL_OZ -> "fl oz"
    VolumeUnit.ML -> "ml"
    VolumeUnit.CUP -> "cup"
}

/**
 * Fetches [span] in consecutive chunks the list endpoints can answer in full (each returns at
 * most 100 days), all at once, and joins the results oldest first.
 */
private suspend fun <T> fetchInChunks(span: DateSpan, date: (T) -> String, fetch: suspend (DateSpan) -> List<T>): List<T> = coroutineScope {
    mergeChunks(requestChunks(span).map { chunk -> async { fetch(chunk) } }.awaitAll(), date)
}
