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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.january.partner.foodlogs.CreateFoodLogRequest
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.FoodLogUserContext
import ai.january.partner.foodlogs.UpdateFoodLogRequest
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.ServingOption
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import ai.january.partner.FoodId
import ai.january.partner.ServingId
import java.time.OffsetDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodLogEditorSheet(
    state: DemoState,
    user: FoodLogUserContext,
    existing: FoodLog? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val client = state.client ?: return
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var timestamp by remember(existing) {
        mutableStateOf(existing?.timestampUtc?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() } ?: OffsetDateTime.now())
    }
    var foods by remember(existing) { mutableStateOf(existing?.foods?.map(::selectedFood).orEmpty()) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun save() {
        if (foods.isEmpty()) return
        saving = true
        error = null
        coroutineScope.launch {
            runCatching {
                val selections = foods.map {
                    FoodSelection(it.food.id.value, ServingSelection(it.serving.id.value, it.quantity))
                }
                if (existing == null) {
                    client.foodLogs.create(
                        CreateFoodLogRequest(
                            foods = selections,
                            timestampUtc = timestamp.toString(),
                            name = name.trim().takeIf(String::isNotEmpty),
                            user = user,
                        ),
                    )
                } else {
                    client.foodLogs.update(
                        UpdateFoodLogRequest(
                            id = existing.id,
                            foods = selections,
                            timestampUtc = timestamp.toString(),
                            name = name.trim().takeIf(String::isNotEmpty),
                            user = user,
                        ),
                    )
                }
            }.onSuccess {
                onSaved()
                onDismiss()
            }.onFailure { error = it.message ?: "The food log could not be saved." }
            saving = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JanuaryColors.Paper,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding, vertical = 20.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color(0xFF6E5613))
                }
                Text(if (existing == null) "New food log" else "Edit food log", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = ::save,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enabled = foods.isNotEmpty() && !saving,
                ) { Text(if (existing == null) "Save" else "Update") }
            }
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = DemoScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionLabel("Meal")
                DemoCard {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name (optional)") },
                        singleLine = true,
                    )
                    HorizontalDivider(color = JanuaryColors.Divider)
                    StartTimeRow(timestamp) { timestamp = it }
                }

                SectionLabel("Foods")
                DemoCard {
                    foods.forEachIndexed { index, selected ->
                        if (index > 0) HorizontalDivider(color = JanuaryColors.Divider)
                        SelectedFoodRow(
                            selected = selected,
                            onServingChange = { serving ->
                                foods = foods.toMutableList().also { it[index] = selected.copy(serving = serving) }
                            },
                            onQuantityChange = { quantity ->
                                foods = if (quantity < 0.25) {
                                    foods.filterIndexed { itemIndex, _ -> itemIndex != index }
                                } else {
                                    foods.toMutableList().also { it[index] = selected.copy(quantity = quantity) }
                                }
                            },
                            onRemove = { foods = foods.filterIndexed { itemIndex, _ -> itemIndex != index } },
                        )
                    }
                    if (foods.isNotEmpty()) HorizontalDivider(color = JanuaryColors.Divider)
                    TextButton(onClick = { showFoodPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("＋  Add food", color = androidx.compose.ui.graphics.Color(0xFF6E5613), style = MaterialTheme.typography.titleMedium)
                    }
                }

                error?.let { ErrorCard(it, ::save) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showFoodPicker) {
        FoodPickerSheet(
            state = state,
            onDismiss = { showFoodPicker = false },
            onSelect = { foods = foods + it },
        )
    }
}

private fun selectedFood(logged: ai.january.partner.foodlogs.LoggedFood): DemoSelectedFood {
    val serving = ServingOption(
        id = ServingId(logged.servingDetails.id),
        quantity = logged.servingDetails.quantity,
        unit = logged.servingDetails.unit,
        scalingFactor = 1.0,
        weightGrams = logged.servingDetails.weightGrams,
        isPrimary = true,
    )
    return DemoSelectedFood(
        food = FoodSearchItem(
            id = FoodId(logged.id),
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
        quantity = logged.consumedServing.quantity,
    )
}
