package ai.january.partner.demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.january.partner.foods.FoodCategory
import ai.january.partner.foods.AutocompleteFoodCategory
import ai.january.partner.foods.AutocompleteFoodsRequest
import ai.january.partner.foods.DietPreference
import ai.january.partner.foods.DietRestriction
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.FoodSuggestion
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageResponse
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.ServingOption
import ai.january.partner.foods.SuggestFoodAlternativesRequest
import ai.january.partner.foods.portion
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
import ai.january.partner.restaurants.Restaurant
import ai.january.partner.restaurants.RestaurantMenuItem
import ai.january.partner.restaurants.SearchRestaurantsRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodDetailScreen(
    state: DemoState,
    food: FoodSearchItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var detailFood by remember(food) { mutableStateOf(food) }
    val initialServing = remember(food) { food.servings.firstOrNull { it.isPrimary } ?: food.servings.firstOrNull() }
    var serving by remember(food) { mutableStateOf(initialServing) }
    var quantity by remember(food) { mutableDoubleStateOf(initialServing?.quantity?.takeIf { it > 0 } ?: 1.0) }
    var servingMenuExpanded by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf<GlucosePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAlternatives by remember { mutableStateOf(false) }
    var showGlucoseSheet by remember { mutableStateOf(false) }

    val portion = serving?.let { runCatching { detailFood.portion(it.id, quantity) }.getOrNull() }

    LaunchedEffect(food.id, state.client, state.partnerUserId) {
        val sdk = state.client ?: return@LaunchedEffect
        runCatching { sdk.foods.get(GetFoodRequest(food.id, state.partnerUserId)) }
            .onSuccess { fullFood ->
                detailFood = fullFood
                val primary = fullFood.servings.firstOrNull { it.isPrimary } ?: fullFood.servings.firstOrNull()
                serving = primary
                quantity = primary?.quantity?.takeIf { it > 0 } ?: 1.0
            }
            .onFailure { error = it.message ?: "Complete serving details could not be loaded." }
    }

    fun predict() {
        val sdk = state.client ?: return
        val selectedPortion = portion ?: return
        showGlucoseSheet = true
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                sdk.glucose.predict(
                    PredictGlucoseRequest(
                        userProfile = GlucosePredictionProfile(
                            age = 42.0,
                            sex = Sex.FEMALE,
                            height = Height(66.0, HeightUnit.INCHES),
                            weight = Weight(150.0, WeightUnit.POUNDS),
                        ),
                        foods = listOf(selectedPortion.selection),
                        startTime = OffsetDateTime.now(),
                        endUserId = state.partnerUserId,
                        timezone = state.timezone,
                    ),
                )
            }.onSuccess { prediction = it }
                .onFailure { error = it.message ?: "The prediction failed." }
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        AppNavigationBar(title = "Food details", onBack = onBack)
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NetworkImage(
                    detailFood.photoUrl,
                    detailFood.name,
                    Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)),
                )
                Text(detailFood.name, style = MaterialTheme.typography.headlineMedium)
                detailFood.brandName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                DemoCard {
                    if (detailFood.servings.isEmpty()) {
                        Text("No serving options were returned.")
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = servingMenuExpanded,
                            onExpandedChange = { servingMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = serving?.let(::servingLabel).orEmpty(),
                                onValueChange = {},
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                readOnly = true,
                                label = { Text("Serving") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = servingMenuExpanded) },
                            )
                            ExposedDropdownMenu(
                                expanded = servingMenuExpanded,
                                onDismissRequest = { servingMenuExpanded = false },
                            ) {
                                detailFood.servings.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(servingLabel(option)) },
                                        onClick = {
                                            serving = option
                                            quantity = option.quantity.takeIf { it > 0 } ?: 1.0
                                            prediction = null
                                            servingMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Quantity: ${formatNumber(quantity)}", fontFamily = FontFamily.Monospace)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { quantity = (quantity - .25).coerceAtLeast(.25); prediction = null }) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "Decrease quantity")
                                }
                                IconButton(onClick = { quantity = (quantity + .25).coerceAtMost(100.0); prediction = null }) {
                                    Icon(Icons.Outlined.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }
                    }
                }

                DemoCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric("Calories", portion?.nutrition?.calories?.value, portion?.nutrition?.calories?.unit ?: "cal")
                        Metric("Protein", portion?.nutrition?.protein?.value, portion?.nutrition?.protein?.unit ?: "g")
                        Metric("Carbs", portion?.nutrition?.carbohydrates?.value, portion?.nutrition?.carbohydrates?.unit ?: "g")
                        Metric("Fat", portion?.nutrition?.totalFat?.value, portion?.nutrition?.totalFat?.unit ?: "g")
                    }
                }

                val facts = listOf(
                    "Net carbohydrates" to portion?.nutrition?.netCarbohydrates,
                    "Saturated fat" to portion?.nutrition?.saturatedFat,
                    "Fiber" to portion?.nutrition?.fiber,
                    "Total sugars" to portion?.nutrition?.totalSugars,
                    "Added sugars" to portion?.nutrition?.addedSugars,
                    "Sodium" to portion?.nutrition?.sodium,
                    "Potassium" to portion?.nutrition?.potassium,
                    "Cholesterol" to portion?.nutrition?.cholesterol,
                ).filter { it.second != null }
                if (facts.isNotEmpty() || portion?.glycemicIndex != null || portion?.glycemicLoad != null) {
                    SectionLabel("Nutrition facts")
                    DemoCard {
                        facts.forEachIndexed { index, (label, value) ->
                            NutritionRow(label, value!!.value, value.unit)
                            if (index < facts.lastIndex) HorizontalDivider()
                        }
                        portion?.glycemicIndex?.let { NutritionRow("Glycemic index", it, "") }
                        portion?.glycemicLoad?.let { NutritionRow("Glycemic load", it, "") }
                    }
                }

                DemoPrimaryButton(
                    text = "Check glucose",
                    onClick = ::predict,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serving != null && state.client != null,
                    loading = loading,
                )
                DemoPrimaryButton(
                    text = "Find alternatives",
                    onClick = { showAlternatives = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.client == null) ApiKeyRequiredCard()
                error?.let { ErrorCard(it, ::predict) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showAlternatives) {
        AlternativesSheet(state, detailFood, onDismiss = { showAlternatives = false })
    }
    if (showGlucoseSheet && serving != null) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGlucoseSheet = false },
            sheetState = sheetState,
            containerColor = JanuaryColors.Paper,
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppModalHeader(title = "Glucose response", onDismiss = { showGlucoseSheet = false })
                Text(detailFood.name, style = MaterialTheme.typography.titleMedium)
                Text("${formatNumber(quantity)} ${serving!!.unit}", color = JanuaryColors.Muted, fontFamily = FontFamily.Monospace)
                if (loading) {
                    DemoCard {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            LoadingSpinner()
                            Text("Predicting your glucose response…")
                            Text("This usually takes a few seconds.", color = JanuaryColors.Muted)
                        }
                    }
                } else {
                    error?.let { ErrorCard(it, ::predict) }
                    prediction?.let { GlucosePredictionResult(it, listOf(DemoSelectedFood(detailFood, serving!!, quantity))) }
                }
                DemoCard {
                    Text("Demo profile", style = MaterialTheme.typography.titleMedium)
                    Text("42 years · Female · 66 in · 150 lb · No reported condition", color = JanuaryColors.Muted)
                }
                Text("This is an estimate for demonstration purposes, not medical advice.", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
internal fun servingLabel(serving: ServingOption): String = buildString {
    append("${formatNumber(serving.quantity)} ${serving.unit}")
    serving.weightGrams?.let { append(" · ${formatNumber(it)} g") }
}

@Composable
internal fun NutritionRow(label: String, value: Double, unit: String) {
    NutritionList(listOf(NutritionValue(label, "${formatNumber(value)}${if (unit.isEmpty()) "" else " $unit"}")))
}

@Composable
internal fun ScanStyleMacroStrip(calories: Double?, protein: Double?, carbs: Double?, fat: Double?) {
    MacroGrid(
        listOf(
            MacroValue("Calories", calories?.let(::formatNumber) ?: "—", "cal"),
            MacroValue("Protein", protein?.let(::formatNumber) ?: "—", "g"),
            MacroValue("Carbs", carbs?.let(::formatNumber) ?: "—", "g"),
            MacroValue("Fat", fat?.let(::formatNumber) ?: "—", "g"),
        ),
    )
}
