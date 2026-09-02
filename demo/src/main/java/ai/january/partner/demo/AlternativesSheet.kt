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
import androidx.compose.material.icons.outlined.Eco
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlternativesSheet(state: DemoState, food: FoodSearchItem, onDismiss: () -> Unit) {
    val client = state.client ?: return
    val scope = rememberCoroutineScope()
    var restrictions by remember { mutableStateOf<Set<DietRestriction>>(emptySet()) }
    var preferences by remember { mutableStateOf<Set<DietPreference>>(emptySet()) }
    var result by remember { mutableStateOf<ai.january.partner.foods.SuggestFoodAlternativesResponse?>(null) }
    var details by remember { mutableStateOf<Map<String, FoodSearchItem>>(emptyMap()) }
    var selected by remember { mutableStateOf<FoodSearchItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    fun load() {
        loading = true; error = null
        scope.launch {
            runCatching {
                val response = client.foods.suggestAlternatives(SuggestFoodAlternativesRequest(food.id.value, restrictions.toList(), preferences.toList(), state.partnerUserId))
                result = response; details = emptyMap()
                details = kotlinx.coroutines.coroutineScope {
                    response.alternatives.mapNotNull { it.id }.distinct().map { id -> async {
                        runCatching { id to client.foods.get(GetFoodRequest(ai.january.partner.FoodId(id), state.partnerUserId)) }.getOrNull()
                    } }.awaitAll().filterNotNull().toMap()
                }
            }.onFailure { error = it }
            loading = false
        }
    }
    AppModalSheet(title = "Food alternatives", onDismiss = onDismiss, showNavigationBar = selected == null) {
        if (selected != null) {
            FoodDetailScreen(state, selected!!, { selected = null })
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                DemoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Eco, null, tint = JanuaryColors.Green); Text("Personalized suggestions", style = MaterialTheme.typography.labelMedium, color = JanuaryColors.Green) }
                        Text(food.name ?: "Unnamed food", style = MaterialTheme.typography.headlineMedium)
                        Text("Choose any dietary needs that should shape January’s recommendations.", color = JanuaryColors.Body)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Dietary restrictions")
                    DietChoices(DietRestriction.entries, restrictions, { restrictions = it }) { it.value.dietLabel() }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Dietary preferences")
                    DietChoices(DietPreference.entries, preferences, { preferences = it }) { it.value.dietLabel() }
                }
                DemoPrimaryButton(if (result == null) "Find alternatives" else "Refresh alternatives", ::load, Modifier.fillMaxWidth(), enabled = !loading, loading = loading && result == null, icon = { Icon(Icons.Outlined.Eco, null) })
                error?.let { ErrorCard(it, ::load) }
                result?.let { response ->
                    if (response.alternatives.isEmpty()) EmptyStateCard(Icons.Outlined.Eco, "No suitable alternatives", "No foods matched every selected dietary need.")
                    else SectionLabel("Suggestions · ${response.alternatives.size}")
                    response.alternatives.forEach { alternative ->
                        val detail = alternative.id?.let(details::get) ?: alternativeDetailFood(alternative)
                        DemoCard(if (detail != null) Modifier.clickable { selected = detail } else Modifier) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                NetworkImage(detail?.photoUrl, null, Modifier.size(58.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text(alternative.name ?: "Unnamed food", style = MaterialTheme.typography.titleMedium)
                                    alternative.brandName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted) }
                                    alternative.servings?.firstOrNull()?.let { Text("${formatDemoNumber(it.quantity ?: 1.0)} ${it.unit.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted) }
                                    val n = alternative.nutrients
                                    Text(listOfNotNull(n.calories?.value?.let { "${it.toInt()} cal" }, n.protein?.value?.let { "P ${formatMetricNumber(it)}g" }, n.carbohydrates?.value?.let { "C ${formatMetricNumber(it)}g" }, n.totalFat?.value?.let { "F ${formatMetricNumber(it)}g" }).joinToString("  "), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = JanuaryColors.Muted)
                                }
                                if (detail != null) Icon(Icons.Outlined.ChevronRight, "Open food details", tint = JanuaryColors.Subdued)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

internal fun alternativeDetailFood(food: ai.january.partner.foods.DetectedFood): FoodSearchItem? {
    val id = food.id ?: return null
    val servings = food.servings?.takeIf { it.isNotEmpty() } ?: return null
    val n = food.nutrients
    return FoodSearchItem(id = ai.january.partner.FoodId(id), name = food.name, brandName = food.brandName,
        calories = n.calories?.value, protein = n.protein?.value, carbohydrates = n.carbohydrates?.value,
        netCarbohydrates = n.netCarbohydrates?.value, totalFat = n.totalFat?.value, saturatedFat = n.saturatedFat?.value,
        fiber = n.fiber?.value, totalSugars = n.totalSugars?.value, addedSugars = n.addedSugars?.value, sodium = n.sodium?.value,
        potassium = null, cholesterol = null, glycemicIndex = null, glycemicLoad = null, photoUrl = null,
        servings = servings.mapIndexed { index, serving -> ServingOption(serving.id?.let { ai.january.partner.ServingId(it) }, serving.quantity ?: 1.0, serving.unit, 1.0, weightGrams = null, isPrimary = index == 0) })
}

private fun String.dietLabel(): String = replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

@Composable
internal fun <T> DietChoices(values: List<T>, selected: Set<T>, onChange: (Set<T>) -> Unit, label: (T) -> String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { value -> DemoChoiceChip(label(value), value in selected, { onChange(if (value in selected) selected - value else selected + value) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

internal fun String.normalizedRestaurantName(): String = java.text.Normalizer.normalize(substringBefore("("), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().split(Regex("[^\\p{L}\\p{N}]+" )).filter(String::isNotBlank).joinToString(" ")

@Composable
internal fun Metric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let(::formatNumber) ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text(unit, style = MaterialTheme.typography.bodySmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
