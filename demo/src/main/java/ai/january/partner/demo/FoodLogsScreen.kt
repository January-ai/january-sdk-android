package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.cancelAndJoin
import ai.january.partner.PartnerUserContext
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.LoggedFood
import ai.january.partner.models.NutrientAmount
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogsScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    var span by remember { mutableStateOf(FoodLogTimeSpan.CURRENT_WEEK) }
    val range = span.dateRange(timezone = state.timezone)
    var logs by remember { mutableStateOf<List<FoodLog>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<FoodLog?>(null) }
    val userContext = state.partnerContext
    val userClient = state.userClient
    var loadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun load() {
        loadJob?.cancel()
        val sdk = userClient ?: return
        loading = true
        error = null
        loadJob = coroutineScope.launch {
            try {
                logs = sdk.foodLogs.list(range.apiStart, range.apiEnd).items
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(userContext, client, span) {
        loadJob?.cancelAndJoin()
        logs = emptyList()
        error = null
        loading = false
        if (userClient != null) load()
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
        title = "Food logs", modifier = modifier, style = AppNavigationTitleStyle.Leading,
        trailing = {
            if (userContext != null && client != null) {
                AppNavigationButton(AppNavigationButtonKind.Add, title = "Add food log", onClick = { showEditor = true })
            }
            AppNavigationButton(AppNavigationButtonKind.Settings, onClick = settingsAction)
        },
    ) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = ::load,
            modifier = Modifier.fillMaxSize(),
        ) {
            DemoScreen {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MealWorkflowGuide(
                        title = "Build one complete meal",
                        message = "One food log represents one meal or eating event. It can contain multiple foods, each with its own serving and quantity.",
                        steps = listOf("Identify the user who owns the log", "Create a log and add every food in the meal", "Save it, then browse that user’s history"),
                        icon = Icons.Outlined.Assignment,
                    )
                    SectionLabel("User identity")
                    FoodLogUserCard(
                        userId = state.partnerUserId?.value,
                        timezone = state.timezone,
                        onSave = { state.endUserId = it },
                        onSettings = settingsAction,
                    )
                    if (userContext != null) {
                        DemoPrimaryButton("Create a food log", { showEditor = true }, Modifier.fillMaxWidth(), enabled = client != null,
                            icon = { Icon(Icons.Outlined.Add, null) })
                        SectionLabel("Browse saved logs")
                        Text("Food logs are fetched for the selected user ID and date range.", fontSize = 15.sp, lineHeight = 20.sp, color = JanuaryColors.Body)
                        FoodLogTimeSpanPicker(span, range) { span = it }
                        DemoPrimaryButton("Refresh food logs", ::load, Modifier.fillMaxWidth(), enabled = userClient != null && !loading, loading = loading && logs.isEmpty())
                        if (loading && logs.isEmpty()) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                LoadingSpinner(color = JanuaryColors.Green)
                                Text("Loading food logs…", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Muted)
                            }
                        }
                        error?.let { ErrorCard(it, ::load) }
                        logs.forEach { log -> FoodLogRow(log) { selectedLog = log } }
                        if (!loading && error == null && logs.isEmpty()) {
                            EmptyStateCard(
                                Icons.Outlined.Assignment,
                                "No food logs in this range",
                                "Create a log, add one or more foods to the meal, then save it for this user.",
                            )
                        }
                    }
                    if (client == null) AuthenticationRequiredCard()
                }
            }
        }

    }
    if (showEditor && userContext != null) {
        FoodLogEditorSheet(state, userContext, onDismiss = { showEditor = false }, onSaved = ::load)
    }
}

@Composable
private fun FoodLogRow(log: FoodLog, onClick: () -> Unit) {
    FoodLogCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FoodLogMealIcon()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(log.name?.takeIf(String::isNotBlank) ?: "Meal", style = MaterialTheme.typography.titleMedium)
                Text(log.foods.joinToString { it.name ?: "Unnamed food" }, maxLines = 2)
                Text("${localLogDate(log.timestampUtc)} · ${log.foods.size} food${if (log.foods.size == 1) "" else "s"}", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = JanuaryColors.Subdued)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodLogDetailScreen(
    state: DemoState,
    log: FoodLog,
    user: PartnerUserContext,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
    modifier: Modifier,
) {
    val client = state.client ?: return
    val coroutineScope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var showTechnicalDetails by remember { mutableStateOf(false) }

    fun deleteLog() {
        if (deleting) return
        confirmingDelete = false
        deleting = true
        error = null
        coroutineScope.launch {
            runCatching { client.forUser(user).foodLogs.delete(requireNotNull(log.id)) }
                .onSuccess { onChanged() }
                .onFailure { error = it }
            deleting = false
        }
    }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    AppScreenScaffold(
        title = "Food log", modifier = modifier,
        leading = { AppNavigationButton(AppNavigationButtonKind.Back, title = "Back from Food log", onClick = onDismiss) },
        trailing = { AppNavigationButton(AppNavigationButtonKind.Edit, onClick = { editing = true }) },
    ) {
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(log.name?.takeIf(String::isNotBlank) ?: "Meal", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(localLogDate(log.timestampUtc), color = JanuaryColors.Muted)
                log.foods.forEach { LoggedFoodCard(it) }
                Column {
                    Row(Modifier.fillMaxWidth().clickable { showTechnicalDetails = !showTechnicalDetails }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Technical details", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Icon(if (showTechnicalDetails) Icons.Outlined.ExpandMore else Icons.Outlined.ChevronRight, null)
                    }
                    if (showTechnicalDetails) Text("Log ID · ${log.id}", style = MaterialTheme.typography.bodySmall)
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    enabled = !deleting,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, JanuaryColors.Rust.copy(alpha = 0.35f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFAEBE1), contentColor = Color(0xFF8C4A2F)),
                ) { Text("Delete food log", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                error?.let { ErrorCard(it, ::deleteLog) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (editing) {
        FoodLogEditorSheet(state, user, existing = log, onDismiss = { editing = false }, onSaved = onChanged)
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete this food log?") },
            text = { Text("This action can't be undone.") },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
            confirmButton = {
                TextButton(
                    onClick = ::deleteLog,
                    modifier = Modifier.testTag("confirm-delete-food-log"),
                ) { Text("Delete food log", color = JanuaryColors.Rust) }
            },
        )
    }
}

@Composable
private fun LoggedFoodCard(food: LoggedFood) {
    FoodLogCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(food.name ?: "Unnamed food", style = MaterialTheme.typography.titleMedium)
            food.brandName?.let { Text(it, color = JanuaryColors.Muted) }
            Text("${formatLogNumber(food.consumedServing.quantity ?: 1.0)} × ${formatLogNumber(food.servingDetails.quantity ?: 1.0)} ${food.servingDetails.unit.orEmpty()}", fontSize = 15.sp)
            FoodLogMacros(listOf("Calories" to food.nutrients.calories, "Protein" to food.nutrients.protein, "Carbs" to food.nutrients.carbohydrates, "Fat" to food.nutrients.totalFat))
            NutritionList(listOf(
                "Net carbohydrates" to food.nutrients.netCarbohydrates,
                "Trans fat" to food.nutrients.transFat,
                "Saturated fat" to food.nutrients.saturatedFat,
                "Fiber" to food.nutrients.fiber,
                "Total sugars" to food.nutrients.totalSugars,
                "Added sugars" to food.nutrients.addedSugars,
                "Cholesterol" to food.nutrients.cholesterol,
                "Calcium" to food.nutrients.calcium,
                "Iron" to food.nutrients.iron,
                "Potassium" to food.nutrients.potassium,
                "Sodium" to food.nutrients.sodium,
                "Vitamin D" to food.nutrients.vitaminD,
            ).mapNotNull { (label, amount) -> amount?.let { NutritionValue(label, "${formatLogNumber(it.value)} ${it.unit}") } })
        }
    }
}

private fun localLogDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(value)

private fun formatLogNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
