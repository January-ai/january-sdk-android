package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import ai.january.partner.PartnerUserContext
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.ServingOption
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.FoodId
import ai.january.partner.ServingId
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodLogEditorSheet(
    state: DemoState,
    user: PartnerUserContext,
    existing: FoodLog? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val client = state.client ?: return
    val userClient = remember(client, user) { client.forUser(user) }
    val coroutineScope = rememberCoroutineScope()
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var timestamp by remember(existing) {
        mutableStateOf(existing?.timestampUtc?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(java.time.ZoneId.systemDefault()).toOffsetDateTime() }.getOrNull() } ?: OffsetDateTime.now())
    }
    var foods by remember(existing) { mutableStateOf(existing?.foods?.map(::selectedFood).orEmpty()) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    fun save() {
        if (foods.isEmpty() || saving) return
        saving = true
        error = null
        coroutineScope.launch {
            runCatching {
                val selections = foods.map {
                    FoodSelection(it.food.id.value, ServingSelection(requireNotNull(it.serving.id).value, it.quantity))
                }
                val timestampUtc = timestamp.withOffsetSameInstant(ZoneOffset.UTC).toString()
                if (existing == null) {
                    userClient.foodLogs.create(
                        foods = selections,
                        timestampUtc = timestampUtc,
                        name = name.trim().takeIf(String::isNotEmpty),
                    )
                } else {
                    userClient.foodLogs.update(
                        id = requireNotNull(existing.id),
                        foods = selections,
                        timestampUtc = timestampUtc,
                        name = name.trim().takeIf(String::isNotEmpty),
                    )
                }
            }.onSuccess {
                onSaved()
                onDismiss()
            }.onFailure { error = it }
            saving = false
        }
    }

    AppModalSheet(title = if (existing == null) "New food log" else "Edit food log", onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = DemoScreenPadding).padding(top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MealWorkflowGuide(
                    title = if (existing == null) "Build this meal" else "Update this meal",
                    message = "A log is one meal. Add every food that belongs to it, then choose each serving and quantity before saving.",
                    steps = listOf("Set the meal time", "Add one or more foods", "Review servings and save"),
                    icon = Icons.Outlined.Restaurant,
                )
                SectionLabel("Meal details")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meal name", style = MaterialTheme.typography.titleMedium)
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        placeholder = { Text("Optional name") },
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(focusedContainerColor = JanuaryColors.Control, unfocusedContainerColor = JanuaryColors.Control, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        singleLine = true,
                    )
                }
                FoodLogCard { StartTimeRow(timestamp, label = "Date and time") { timestamp = it } }
                SectionLabel("Foods in this meal · ${foods.size}")
                if (foods.isEmpty()) {
                    EmptyStateCard(Icons.Outlined.AddCircleOutline, "No foods added", "Start with one food, then keep adding until the complete meal is represented.")
                }
                foods.forEachIndexed { index, selected ->
                    FoodLogSelectedFoodCard(
                        selected = selected,
                        onServingChange = { serving -> foods = foods.toMutableList().also { it[index] = selected.copy(serving = serving) } },
                        onQuantityChange = { quantity -> foods = foods.toMutableList().also { it[index] = selected.copy(quantity = quantity) } },
                        onRemove = { foods = foods.filterIndexed { itemIndex, _ -> itemIndex != index } },
                    )
                }
                DemoOutlinedButton(
                    if (foods.isEmpty()) "Add first food" else "Add another food", { showFoodPicker = true }, Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Outlined.Add, null) },
                )

                error?.let { ErrorCard(it, ::save) }
                DemoPrimaryButton(if (existing == null) "Save food log" else "Update food log", ::save, Modifier.fillMaxWidth(), enabled = foods.isNotEmpty(), loading = saving)
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    if (showFoodPicker) {
        FoodPickerSheet(
            state = state,
            onDismiss = { showFoodPicker = false },
            onSelect = { foods = foods + it; showFoodPicker = false },
        )
    }
}

private fun selectedFood(logged: ai.january.partner.foodlogs.LoggedFood): DemoSelectedFood {
    val serving = ServingOption(
        id = logged.servingDetails.id?.let(::ServingId),
        quantity = logged.servingDetails.quantity ?: 1.0,
        unit = logged.servingDetails.unit,
        scalingFactor = 1.0,
        weightGrams = logged.servingDetails.weightGrams,
        isPrimary = true,
    )
    return DemoSelectedFood(
        food = FoodSearchItem(
            id = FoodId(requireNotNull(logged.id)),
            name = logged.name,
            brandName = logged.brandName,
            calories = logged.nutrients.calories?.value,
            protein = logged.nutrients.protein?.value,
            carbohydrates = logged.nutrients.carbohydrates?.value,
            netCarbohydrates = logged.nutrients.netCarbohydrates?.value,
            totalFat = logged.nutrients.totalFat?.value,
            saturatedFat = logged.nutrients.saturatedFat?.value,
            fiber = logged.nutrients.fiber?.value,
            totalSugars = logged.nutrients.totalSugars?.value,
            addedSugars = logged.nutrients.addedSugars?.value,
            sodium = logged.nutrients.sodium?.value,
            potassium = logged.nutrients.potassium?.value,
            cholesterol = logged.nutrients.cholesterol?.value,
            glycemicIndex = logged.glycemicIndex,
            glycemicLoad = logged.glycemicLoad,
            photoUrl = logged.imageUrl,
            servings = listOf(serving),
        ),
        serving = serving,
        quantity = logged.consumedServing.quantity ?: 1.0,
    )
}
