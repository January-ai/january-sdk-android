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
internal fun RestaurantFiltersSheet(
    selectedCity: SearchCity,
    onCity: (SearchCity) -> Unit,
    radius: Double,
    onRadius: (Double) -> Unit,
    limit: Int,
    onLimit: (Int) -> Unit,
    locationLabel: String,
    onCurrentLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    var cityMenu by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JanuaryColors.Paper) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppModalHeader(title = "Search filters", onDismiss = onDismiss)
            SectionLabel("Location")
            DemoCard {
                ExposedDropdownMenuBox(cityMenu, { cityMenu = it }) {
                    OutlinedTextField(
                        selectedCity.name,
                        {},
                        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        readOnly = true,
                        label = { Text("City") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cityMenu) },
                    )
                    ExposedDropdownMenu(cityMenu, { cityMenu = false }) {
                        searchCities.forEach { city -> DropdownMenuItem({ Text(city.name) }, { onCity(city); cityMenu = false }) }
                    }
                }
                DemoOutlinedButton(
                    "Use my current location",
                    onCurrentLocation,
                    Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Outlined.LocationOn, null) },
                )
                Text(locationLabel, color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            SectionLabel("Radius")
            DemoCard {
                Slider(radius.toFloat(), { onRadius(it.toDouble()) }, valueRange = 500f..17000f)
                Text("${"%.1f".format(radius / 1609.344)} mi · ${radius.toInt()} m", fontFamily = FontFamily.Monospace)
            }
            SectionLabel("Results")
            DemoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Limit: $limit", Modifier.weight(1f))
                    IconButton(onClick = { onLimit((limit - 1).coerceAtLeast(1)) }) { Icon(Icons.Outlined.Remove, "Decrease limit") }
                    IconButton(onClick = { onLimit((limit + 1).coerceAtMost(100)) }) { Icon(Icons.Outlined.Add, "Increase limit") }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestaurantDetailScreen(
    client: JanuaryPartnerClient?,
    restaurant: Restaurant,
    latitude: Double,
    longitude: Double,
    radius: Double,
    resultLimit: Int,
    menuQuery: String,
    endUserId: PartnerUserId?,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    var items by remember(restaurant.id) { mutableStateOf<List<RestaurantMenuItem>>(emptyList()) }
    var loading by remember(restaurant.id) { mutableStateOf(true) }
    var error by remember(restaurant.id) { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<RestaurantMenuItem?>(null) }
    LaunchedEffect(restaurant.id) {
        if (client == null) { loading = false; return@LaunchedEffect }
        runCatching {
            client.restaurants.searchMenuItems(
                SearchRestaurantsRequest(menuQuery.ifBlank { restaurant.name }, latitude, longitude, radius, resultLimit, endUserId),
            ).items.filter {
                val a = it.restaurantName.normalizedRestaurantName()
                val b = restaurant.name.normalizedRestaurantName()
                a.contains(b) || b.contains(a)
            }
        }.onSuccess { items = it }.onFailure { error = it.message ?: "Menu items could not be loaded." }
        loading = false
    }
    selected?.let { MenuItemDetailScreen(it, { selected = null }, modifier); return }
    Column(modifier.fillMaxSize()) {
        AppNavigationBar(title = "Restaurant", onBack = onBack)
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(restaurant.name, style = MaterialTheme.typography.displaySmall)
                SectionLabel("Location")
                DemoCard {
                    restaurant.city?.let {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("City")
                            Text(it, color = JanuaryColors.Muted)
                        }
                    }
                    restaurant.address1?.let { Text(it) }
                    restaurant.address2?.let { Text(it) }
                    restaurant.distance?.let { Text("${"%.1f".format(it / 1609.344)} mi", fontFamily = FontFamily.Monospace) }
                }
                SectionLabel("Menu items")
                if (loading) DemoCard { Text("Loading menu…") }
                error?.let { ErrorCard(it) }
                if (!loading && error == null && items.isEmpty()) EmptySearchCard("No menu items found", "January did not return menu items for this restaurant.")
                items.forEach { item ->
                    MenuItemResultCard(item) { selected = item }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MenuItemDetailScreen(item: RestaurantMenuItem, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        AppNavigationBar(title = "Menu item", onBack = onBack)
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(item.name, style = MaterialTheme.typography.displaySmall)
                Text(item.restaurantName, color = JanuaryColors.Muted)
                DemoCard { ScanStyleMacroStrip(item.calories, item.protein, item.carbohydrates, item.totalFat) }
                DemoCard {
                    listOfNotNull(
                        item.netCarbohydrates?.let { "Net carbohydrates" to it },
                        item.fiber?.let { "Fiber" to it },
                        item.totalSugars?.let { "Total sugars" to it },
                        item.addedSugars?.let { "Added sugars" to it },
                        item.glycemicIndex?.let { "Glycemic index" to it },
                        item.glycemicLoad?.let { "Glycemic load" to it },
                    ).forEach { (name, value) -> NutritionRow(name, value, if (name.contains("index") || name.contains("load")) "" else "g") }
                }
                if (item.servings.isNotEmpty()) {
                    SectionLabel("Servings")
                    DemoCard { item.servings.forEach { Text("${formatNumber(it.quantity)} ${it.unit}") } }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
