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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.january.partner.PartnerUserContext
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.LoggedFood
import ai.january.partner.models.NutrientAmount
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

@Composable
fun FoodLogsScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    var span by remember { mutableStateOf(FoodLogTimeSpan.TODAY) }
    val range = span.dateRange()
    var logs by remember { mutableStateOf<List<FoodLog>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<FoodLog?>(null) }
    val userContext = state.partnerContext
    val userClient = state.userClient

    fun load() {
        val sdk = userClient ?: return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching { sdk.foodLogs.list(range.apiStart, range.apiEnd).items }
                .onSuccess { logs = it }
                .onFailure { error = it.message ?: "Food logs could not be loaded." }
            loading = false
        }
    }

    LaunchedEffect(userContext, client, span) {
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

    Column(modifier.fillMaxSize()) {
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppScreenHeader("Food logs", settingsAction) {
                    if (userContext != null && client != null) {
                        IconButton(onClick = { showEditor = true }) { Icon(Icons.Outlined.Add, "Add food log") }
                    }
                }
                Text(
                    "One food log represents one meal and can contain one or more foods. Add every food, choose its serving and quantity, then save the meal once.",
                    color = JanuaryColors.Muted,
                )
                if (state.partnerUserId == null) {
                    val featureShape = RoundedCornerShape(22.dp)
                    androidx.compose.material3.Card(
                        modifier = Modifier.border(1.5.dp, Color(0xFFD9C25F), featureShape),
                        shape = featureShape,
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = JanuaryColors.GoldContainer),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("One thing first")
                            Text("Logs are stored per person. Add the partner's stable user ID before loading or creating logs.")
                            DemoPrimaryButton("Open settings", settingsAction, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    UserContextCard(
                        endUserId = state.endUserId,
                        timezone = state.timezone,
                        description = "The app persists this identity; the SDK only reuses the context for requests.",
                        onEdit = settingsAction,
                    )
                }
                SectionLabel("Date range")
                DemoCard {
                    SegmentedControl(FoodLogTimeSpan.entries, span, { it.title }, { span = it })
                    Text(range.displayText(), color = JanuaryColors.Muted, fontFamily = FontFamily.Monospace)
                    Text("${range.apiStart} through ${range.apiEnd}, inclusive", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
                }
                DemoPrimaryButton(if (logs.isEmpty()) "Load logs" else "Refresh logs", ::load, Modifier.fillMaxWidth(), userClient != null, loading)
                if (userContext == null) {
                    Text("Available once a user ID is set.", Modifier.fillMaxWidth(), color = JanuaryColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                if (client == null) ApiKeyRequiredCard()
                error?.let { ErrorCard(it, ::load) }
                if (!loading && error == null && logs.isEmpty() && userContext != null) {
                    DemoCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.RestaurantMenu, null, tint = JanuaryColors.Green)
                            Text("No food logs", style = MaterialTheme.typography.titleLarge)
                            Text("There are no logs in this date range.", color = JanuaryColors.Muted)
                        }
                    }
                }
                logs.forEach { log -> FoodLogRow(log) { selectedLog = log } }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showEditor && userContext != null) {
        FoodLogEditorSheet(state, userContext, onDismiss = { showEditor = false }, onSaved = ::load)
    }
}

@Composable
private fun FoodLogRow(log: FoodLog, onClick: () -> Unit) {
    DemoCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.RestaurantMenu, null, tint = JanuaryColors.Green)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(log.name?.takeIf(String::isNotBlank) ?: "Meal", style = MaterialTheme.typography.titleMedium)
                Text(log.foods.joinToString { it.name }, maxLines = 2)
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
    var error by remember { mutableStateOf<String?>(null) }

    Column(modifier.fillMaxSize()) {
        AppNavigationBar(
            title = "Food log",
            onBack = onDismiss,
            action = { TextButton(onClick = { editing = true }) { Text("Edit") } },
        )
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(log.name?.takeIf(String::isNotBlank) ?: "Meal", style = MaterialTheme.typography.displaySmall)
                Text(localLogDate(log.timestampUtc), color = JanuaryColors.Muted)
                log.foods.forEach { LoggedFoodCard(it) }
                DemoCard { Text("Log ID · ${log.id}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                error?.let { ErrorCard(it) }
                DemoOutlinedButton("Delete food log", { confirmingDelete = true }, Modifier.fillMaxWidth(), enabled = !deleting)
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
                TextButton(onClick = {
                    confirmingDelete = false
                    deleting = true
                    coroutineScope.launch {
                        runCatching { client.forUser(user).foodLogs.delete(log.id) }
                            .onSuccess { onChanged() }
                            .onFailure { error = it.message ?: "The food log could not be deleted." }
                        deleting = false
                    }
                }) { Text("Delete food log", color = JanuaryColors.Rust) }
            },
        )
    }
}

@Composable
private fun LoggedFoodCard(food: LoggedFood) {
    DemoCard {
        Text(food.name, style = MaterialTheme.typography.titleMedium)
        food.brandName?.let { Text(it, color = JanuaryColors.Muted) }
        Text("${formatLogNumber(food.consumedServing.quantity)} × ${formatLogNumber(food.servingDetails.quantity)} ${food.servingDetails.unit}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LogMetric("Calories", food.nutrients.calories)
            LogMetric("Protein", food.nutrients.protein)
            LogMetric("Carbs", food.nutrients.carbohydrates)
            LogMetric("Fat", food.nutrients.totalFat)
        }
        listOfNotNull(
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
        ).filter { it.second != null }.forEach { (name, amount) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name)
                Text("${formatLogNumber(amount!!.value)} ${amount.unit}", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun LogMetric(label: String, value: NutrientAmount?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.value?.let(::formatLogNumber) ?: "—", fontFamily = FontFamily.Monospace)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun localLogDate(value: String): String = runCatching {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(value)

private fun formatLogNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
