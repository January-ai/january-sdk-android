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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.MonitorHeart
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
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

@Composable
internal fun FoodDetailScreen(state: DemoState, food: FoodSearchItem, onBack: () -> Unit, modifier: Modifier = Modifier, isModal: Boolean = false) {
    var detailFood by remember(food) { mutableStateOf(food) }
    val initialServing = remember(food) { primaryServing(food) }
    var serving by remember(food) { mutableStateOf(initialServing) }
    var quantity by remember(food) { mutableDoubleStateOf(initialServing?.quantity ?: 1.0) }
    var detailLoadFailed by remember(food) { mutableStateOf(false) }
    var showAlternatives by remember { mutableStateOf(false) }
    var showGlucose by remember { mutableStateOf(false) }
    val portion = serving?.let { runCatching { detailFood.portion(it.id, quantity) }.getOrNull() }
    androidx.activity.compose.BackHandler(onBack = onBack)
    LaunchedEffect(food.id, state.client, state.partnerUserId) {
        val client = state.client ?: return@LaunchedEffect
        runCatching { client.foods.get(GetFoodRequest(food.id, state.partnerUserId)) }
            .onSuccess { detailFood = it; serving = primaryServing(it); quantity = serving?.quantity ?: 1.0; detailLoadFailed = false }
            .onFailure { detailLoadFailed = true }
    }
    AppScreenScaffold(
        title = "Food details", modifier = modifier,
        leading = {
            AppNavigationButton(
                if (isModal) AppNavigationButtonKind.Close else AppNavigationButtonKind.Back,
                title = if (isModal) "Close Food details" else "Back from Food details", onClick = onBack,
            )
        },
    ) {
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(JanuaryColors.Control), contentAlignment = Alignment.Center) {
                    NetworkImage(detailFood.photoUrl, detailFood.name, Modifier.fillMaxSize().padding(18.dp), contentScale = ContentScale.Fit)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(detailFood.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    detailFood.brandName?.let { Text(it, color = JanuaryColors.Muted) }
                }
                ServingControls(detailFood.servings, serving, quantity, { serving = it; quantity = it.quantity }, { quantity = it })
                DemoCard { ScanStyleMacroStrip(portion?.nutrition?.calories?.value, portion?.nutrition?.protein?.value, portion?.nutrition?.carbohydrates?.value, portion?.nutrition?.totalFat?.value) }
                val facts = listOf(
                    "Net carbohydrates" to portion?.nutrition?.netCarbohydrates,
                    "Saturated fat" to portion?.nutrition?.saturatedFat,
                    "Fiber" to portion?.nutrition?.fiber,
                    "Total sugars" to portion?.nutrition?.totalSugars,
                    "Added sugars" to portion?.nutrition?.addedSugars,
                    "Sodium" to portion?.nutrition?.sodium,
                    "Potassium" to portion?.nutrition?.potassium,
                    "Cholesterol" to portion?.nutrition?.cholesterol,
                ).mapNotNull { (label, amount) -> amount?.let { NutritionValue(label, "${formatDemoNumber(it.value)} ${it.unit}") } } + listOfNotNull(
                    portion?.glycemicIndex?.let { NutritionValue("Glycemic index", formatDemoNumber(it)) },
                    portion?.glycemicLoad?.let { NutritionValue("Glycemic load", formatDemoNumber(it)) },
                )
                if (facts.isNotEmpty()) DemoCard {
                    Text("Nutrition facts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    NutritionList(facts)
                }
                DemoPrimaryButton("Check glucose", { showGlucose = true }, Modifier.fillMaxWidth(), enabled = serving != null && state.client != null,
                    icon = { Icon(Icons.Outlined.MonitorHeart, null) })
                DemoPrimaryButton("Find alternatives", { showAlternatives = true }, Modifier.fillMaxWidth())
                DetailDisclosure {
                    NutritionList(listOf(NutritionValue("Food ID", detailFood.id.value.toString()), NutritionValue("Serving ID", serving?.id?.value?.toString() ?: "—")))
                }
                if (detailLoadFailed) Text("Complete serving details could not be loaded. Showing the serving returned by search.", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (showAlternatives) AlternativesSheet(state, detailFood) { showAlternatives = false }
    if (showGlucose && serving != null && state.client != null) FoodGlucoseSheet(state.client!!, detailFood.id, detailFood.name, serving!!, quantity, state.partnerUserId, state.timezone) { showGlucose = false }
}
internal fun servingLabel(serving: ServingOption): String = buildString {
    append(serving.unit)
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
