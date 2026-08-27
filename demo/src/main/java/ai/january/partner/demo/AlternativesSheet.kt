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
internal fun AlternativesSheet(state: DemoState, food: FoodSearchItem, onDismiss: () -> Unit) {
    val client = state.client ?: return
    val coroutineScope = rememberCoroutineScope()
    var restrictions by remember { mutableStateOf<Set<DietRestriction>>(emptySet()) }
    var preferences by remember { mutableStateOf<Set<DietPreference>>(emptySet()) }
    var results by remember { mutableStateOf<ai.january.partner.foods.SuggestFoodAlternativesResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun load() {
        loading = true; error = null
        coroutineScope.launch {
            runCatching {
                client.foods.suggestAlternatives(SuggestFoodAlternativesRequest(food.id.value, restrictions.toList(), preferences.toList(), state.partnerUserId))
            }.onSuccess { results = it }.onFailure { error = it.message ?: "Alternatives could not be loaded." }
            loading = false
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = JanuaryColors.Paper) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppModalHeader(title = "Alternatives", onDismiss = onDismiss)
            SectionLabel("Dietary restrictions")
            DietChoices(DietRestriction.entries, restrictions, { restrictions = it }, { it.value.replace('_', ' ').replaceFirstChar(Char::uppercase) })
            SectionLabel("Dietary preferences")
            DietChoices(DietPreference.entries, preferences, { preferences = it }, { it.value.replace('_', ' ').replaceFirstChar(Char::uppercase) })
            DemoPrimaryButton("Find alternatives", ::load, Modifier.fillMaxWidth(), loading = loading)
            error?.let { ErrorCard(it, ::load) }
            results?.alternatives.orEmpty().forEach { alternative ->
                DemoCard {
                    Text(alternative.food.name, style = MaterialTheme.typography.titleMedium)
                    alternative.food.brandName?.let { Text(it, color = JanuaryColors.Muted) }
                    ScanStyleMacroStrip(
                        alternative.food.nutrients.calories?.value,
                        alternative.food.nutrients.protein?.value,
                        alternative.food.nutrients.carbohydrates?.value,
                        alternative.food.nutrients.totalFat?.value,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun <T> DietChoices(values: List<T>, selected: Set<T>, onChange: (Set<T>) -> Unit, label: (T) -> String) {
    values.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { value ->
                FilterChip(
                    selected = value in selected,
                    onClick = { onChange(if (value in selected) selected - value else selected + value) },
                    label = { Text(label(value), maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

internal fun String.normalizedRestaurantName(): String = substringBefore("(").lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()

@Composable
internal fun Metric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let(::formatNumber) ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text(unit, style = MaterialTheme.typography.bodySmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

internal fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
