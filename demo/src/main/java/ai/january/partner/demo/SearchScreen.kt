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
import androidx.compose.ui.platform.testTag
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
import ai.january.partner.photos.FoodScan
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

private enum class SearchScope { FOODS, RESTAURANTS }
private enum class FoodMode { NAME, DESCRIPTION, BARCODE }
private enum class RestaurantMode { RESTAURANTS, MENU_ITEMS }
internal data class SearchCity(val id: String, val name: String, val latitude: Double, val longitude: Double)

internal val searchCities = listOf(
    SearchCity("san-francisco", "San Francisco, CA", 37.7749, -122.4194),
    SearchCity("new-york", "New York, NY", 40.7128, -74.0060),
    SearchCity("los-angeles", "Los Angeles, CA", 34.0522, -118.2437),
    SearchCity("chicago", "Chicago, IL", 41.8781, -87.6298),
    SearchCity("austin", "Austin, TX", 30.2672, -97.7431),
    SearchCity("miami", "Miami, FL", 25.7617, -80.1918),
    SearchCity("seattle", "Seattle, WA", 47.6062, -122.3321),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var scope by remember { mutableStateOf(SearchScope.FOODS) }
    var foodMode by remember { mutableStateOf(FoodMode.NAME) }
    var restaurantMode by remember { mutableStateOf(RestaurantMode.RESTAURANTS) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<FoodCategory?>(null) }
    var foodSuggestions by remember { mutableStateOf<List<FoodSuggestion>>(emptyList()) }
    var autocompleteSuppressedQuery by remember { mutableStateOf<String?>(null) }
    var foodResults by remember { mutableStateOf<List<FoodSearchItem>>(emptyList()) }
    var foodResultLimit by remember { mutableStateOf(10) }
    var foodLimitMenuOpen by remember { mutableStateOf(false) }
    var naturalResult by remember { mutableStateOf<FoodScan?>(null) }
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var menuItems by remember { mutableStateOf<List<RestaurantMenuItem>>(emptyList()) }
    var selectedFood by remember { mutableStateOf<FoodSearchItem?>(null) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var selectedMenuItem by remember { mutableStateOf<RestaurantMenuItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var latitude by remember { mutableDoubleStateOf(37.7749) }
    var longitude by remember { mutableDoubleStateOf(-122.4194) }
    var selectedCity by remember { mutableStateOf(searchCities.first()) }
    var isUsingCurrentLocation by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var locationLabel by remember { mutableStateOf("Preset city · ${searchCities.first().name}") }
    var radius by remember { mutableDoubleStateOf(8000.0) }
    var resultLimit by remember { mutableStateOf(10) }
    var showFilters by remember { mutableStateOf(false) }

    fun clearResults() {
        foodSuggestions = emptyList(); foodResults = emptyList(); naturalResult = null; restaurants = emptyList(); menuItems = emptyList(); error = null
    }

    fun updateLocation() {
        val location = context.bestLastKnownLocation()
        if (location != null) {
            latitude = location.latitude
            longitude = location.longitude
            isUsingCurrentLocation = true
            locationLabel = "Current location · %.4f, %.4f".format(latitude, longitude)
        } else {
            isUsingCurrentLocation = false
            locationLabel = "Location unavailable · using ${selectedCity.name}"
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) updateLocation() else {
            isUsingCurrentLocation = false
            locationLabel = "Location denied · using ${selectedCity.name}"
        }
    }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            updateLocation()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun submit(forcedRestaurantName: String? = null) {
        val value = (forcedRestaurantName ?: query).trim()
        if (value.isEmpty() || client == null) return
        clearResults()
        autocompleteSuppressedQuery = value
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                when (scope) {
                    SearchScope.FOODS -> when (foodMode) {
                        FoodMode.NAME -> foodResults = client.foods.search(
                            SearchFoodsRequest(value, category, foodResultLimit, state.partnerUserId),
                        ).items
                        FoodMode.DESCRIPTION -> naturalResult = client.foodAnalysis.analyzeDescription(
                            SearchFoodsByNaturalLanguageRequest(value, state.partnerUserId),
                        )
                        FoodMode.BARCODE -> foodResults = client.foods.lookupBarcode(
                            LookupFoodByBarcodeRequest(value, state.partnerUserId),
                        ).items
                    }
                    SearchScope.RESTAURANTS -> when (restaurantMode) {
                        RestaurantMode.RESTAURANTS -> restaurants = client.restaurants.search(
                            SearchRestaurantsRequest(value, latitude, longitude, radius, resultLimit, state.partnerUserId),
                        ).items
                        RestaurantMode.MENU_ITEMS -> menuItems = client.restaurants.searchMenuItems(
                            SearchRestaurantsRequest(value, latitude, longitude, radius, resultLimit, state.partnerUserId),
                        ).items
                    }
                }
            }.onFailure { error = it }
            loading = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(scope, foodMode, category, query, client, state.partnerUserId) {
        val value = query.trim()
        val autocompleteCategory = when (category) {
            FoodCategory.GENERIC, FoodCategory.GENERAL -> AutocompleteFoodCategory.GENERIC
            FoodCategory.BRANDED -> AutocompleteFoodCategory.BRANDED
            FoodCategory.RECIPE, null -> null
        }
        val activeClient = client
        val canAutocomplete = scope == SearchScope.FOODS &&
            foodMode == FoodMode.NAME &&
            category != FoodCategory.RECIPE &&
            value.length in 2..64 &&
            value != autocompleteSuppressedQuery &&
            activeClient != null

        if (!canAutocomplete) {
            foodSuggestions = emptyList()
            return@LaunchedEffect
        }

        delay(300)
        foodSuggestions = runCatching {
            requireNotNull(activeClient).foods.autocomplete(
                AutocompleteFoodsRequest(
                    query = value,
                    category = autocompleteCategory,
                    limit = 8,
                    endUserId = state.partnerUserId,
                ),
            ).items
        }.getOrDefault(emptyList())
    }

    selectedFood?.let { food ->
        FoodDetailScreen(state = state, food = food, onBack = { selectedFood = null }, modifier = modifier)
        return
    }

    selectedRestaurant?.let { restaurant ->
        RestaurantDetailScreen(
            state = state,
            restaurant = restaurant,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            resultLimit = resultLimit,
            endUserId = state.partnerUserId,
            onBack = { selectedRestaurant = null },
            modifier = modifier,
        )
        return
    }

    selectedMenuItem?.let { item ->
        MenuItemDetailScreen(state, item, { selectedMenuItem = null }, modifier)
        return
    }

    AppScreenScaffold(
        title = "Search", modifier = modifier, style = AppNavigationTitleStyle.Leading,
        trailing = { AppNavigationButton(AppNavigationButtonKind.Settings, onClick = settingsAction) },
    ) {
        DemoScreen {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp).testTag("search-content"),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    SearchField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it != autocompleteSuppressedQuery) autocompleteSuppressedQuery = null
                        },
                        placeholder = if (scope == SearchScope.RESTAURANTS) {
                            if (restaurantMode == RestaurantMode.RESTAURANTS) "Restaurant name" else "Dish or restaurant"
                        } else when (foodMode) {
                            FoodMode.NAME -> "Food name"
                            FoodMode.DESCRIPTION -> "Describe what was eaten"
                            FoodMode.BARCODE -> "6–14 digit barcode"
                        },
                        onSearch = { submit() },
                    )
                }
                if (foodSuggestions.isNotEmpty()) {
                    item {
                        FoodSuggestionList(foodSuggestions) { suggestion ->
                            val suggestionName = suggestion.name ?: return@FoodSuggestionList
                            autocompleteSuppressedQuery = suggestionName
                            query = suggestionName
                            foodSuggestions = emptyList()
                            submit(suggestionName)
                        }
                    }
                }
                item {
                    SegmentedControl(
                        options = SearchScope.entries,
                        selected = scope,
                        label = { if (it == SearchScope.FOODS) "Foods" else "Restaurants" },
                        onSelect = { scope = it; clearResults() },
                    )
                }
                if (scope == SearchScope.FOODS) {
                    item {
                        SegmentedControl(
                            options = FoodMode.entries,
                            selected = foodMode,
                            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                            onSelect = { foodMode = it; clearResults() },
                        )
                    }
                    if (foodMode == FoodMode.NAME) {
                        item {
                            ChipSelector(
                                options = listOf(
                                    ChipOption(null, "All"),
                                    ChipOption(FoodCategory.GENERIC, "General"),
                                    ChipOption(FoodCategory.BRANDED, "Branded"),
                                    ChipOption(FoodCategory.RECIPE, "Recipe"),
                                ),
                                selected = category,
                                onSelect = { category = it },
                            )
                        }
                    } else if (foodMode == FoodMode.BARCODE) {
                        item {
                            DemoOutlinedButton(
                                text = "Scan barcode",
                                onClick = {
                                    GmsBarcodeScanning.getClient(context).startScan()
                                        .addOnSuccessListener { barcode ->
                                            barcode.rawValue?.let { value -> query = value; submit(value) }
                                        }
                                        .addOnFailureListener { error = it }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
                            )
                        }
                    } else {
                        item { Text("Try “a bowl of oatmeal with honey and a banana.”", color = JanuaryColors.Muted) }
                    }
                } else {
                    item {
                        SegmentedControl(
                            options = RestaurantMode.entries,
                            selected = restaurantMode,
                            label = { if (it == RestaurantMode.RESTAURANTS) "Restaurants" else "Menu items" },
                            onSelect = { restaurantMode = it; clearResults() },
                        )
                    }
                    item {
                        DemoCard(Modifier.clickable { showFilters = true }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.LocationOn, null, tint = JanuaryColors.Green)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Search location", style = MaterialTheme.typography.titleMedium)
                                    Text(locationLabel, color = JanuaryColors.Muted, style = MaterialTheme.typography.labelSmall)
                                }
                                Text("${"%.1f".format(radius / 1609.344)} mi", style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Outlined.ChevronRight, "Search filters", tint = JanuaryColors.Subdued)
                            }
                        }
                    }
                }
                if (query.isBlank()) {
                    item {
                        EmptySearchCard(
                            title = if (scope == SearchScope.RESTAURANTS) "Search nearby" else when (foodMode) {
                                FoodMode.NAME -> "Find a food"
                                FoodMode.DESCRIPTION -> "Describe a meal"
                                FoodMode.BARCODE -> "Enter or scan a barcode"
                            },
                            message = if (scope == SearchScope.RESTAURANTS) "Find restaurants or dishes around a location." else if (foodMode == FoodMode.DESCRIPTION) {
                                "January will identify foods, servings, and nutrition from a sentence."
                            } else "Search January's database, then choose a serving and quantity.",
                        )
                    }
                }
                item {
                    DemoPrimaryButton(
                        text = if (scope == SearchScope.RESTAURANTS) "Search nearby" else when (foodMode) {
                            FoodMode.NAME -> "Search foods"
                            FoodMode.DESCRIPTION -> "Parse meal"
                            FoodMode.BARCODE -> "Look up barcode"
                        },
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = client != null,
                        loading = loading,
                    )
                }
                if (client == null) item { ApiKeyRequiredCard() }
                error?.let { message -> item { ErrorCard(message) { submit() } } }
                if (foodResults.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            SectionLabel("Results · January food database", Modifier.weight(1f))
                            Box {
                                TextButton(onClick = { foodLimitMenuOpen = true }) { Text("${foodResults.size}", fontFamily = FontFamily.Monospace) }
                                androidx.compose.material3.DropdownMenu(foodLimitMenuOpen, { foodLimitMenuOpen = false }) {
                                    listOf(10, 20, 40).forEach { limit ->
                                        DropdownMenuItem({ Text("$limit results") }, { foodResultLimit = limit; foodLimitMenuOpen = false })
                                    }
                                }
                            }
                        }
                        DemoCard(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 4.dp)) {
                            foodResults.forEachIndexed { index, food ->
                                FoodResultCard(food, onClick = { selectedFood = food })
                                if (index < foodResults.lastIndex) HorizontalDivider(color = JanuaryColors.Divider)
                            }
                        }
                    }
                }
                naturalResult?.let { natural ->
                    natural.totalNutrients?.let { nutrients ->
                        item {
                            Text("Meal nutrition", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            DemoCard { ScanStyleMacroStrip(nutrients.calories?.value, nutrients.protein?.value, nutrients.carbohydrates?.value, nutrients.totalFat?.value) }
                        }
                    }
                    items(natural.detections) { detection ->
                        DemoCard {
                            Text(detection.food.name ?: "Unnamed food", style = MaterialTheme.typography.titleMedium)
                            detection.food.brandName?.let { Text(it, color = JanuaryColors.Muted) }
                            ScanStyleMacroStrip(
                                detection.food.nutrients.calories?.value,
                                detection.food.nutrients.protein?.value,
                                detection.food.nutrients.carbohydrates?.value,
                                detection.food.nutrients.totalFat?.value,
                            )
                        }
                    }
                }
                if (restaurants.isNotEmpty()) {
                    item { Text("Nearby restaurants", style = MaterialTheme.typography.titleLarge) }
                    items(restaurants, key = { it.id }) { restaurant ->
                        RestaurantResultCard(restaurant) { selectedRestaurant = restaurant }
                    }
                }
                if (menuItems.isNotEmpty()) {
                    item { Text("Nearby menu items", style = MaterialTheme.typography.titleLarge) }
                    items(menuItems, key = { it.id }) { item ->
                        MenuItemResultCard(item) { selectedMenuItem = item }
                    }
                }
                if (!loading && error == null && query.isNotBlank() && foodResults.isEmpty() && naturalResult == null && restaurants.isEmpty() && menuItems.isEmpty()) {
                    item {
                        EmptySearchCard(
                            if (scope == SearchScope.FOODS) "No foods found" else "No nearby matches",
                            if (scope == SearchScope.FOODS) "Try another name or broaden the selected food category." else "Try another name, location, or search radius.",
                        )
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (showFilters) {
        RestaurantFiltersSheet(
            latitude = latitude,
            longitude = longitude,
            selectedCity = selectedCity,
            onCity = { city ->
                selectedCity = city; latitude = city.latitude; longitude = city.longitude
                isUsingCurrentLocation = false; locationLabel = "Preset city · ${city.name}"
            },
            radius = radius,
            onRadius = { radius = it },
            limit = resultLimit,
            onLimit = { resultLimit = it },
            locationLabel = locationLabel,
            onCurrentLocation = ::requestLocation,
            onDismiss = { showFilters = false },
        )
    }
}


@Suppress("MissingPermission")
private fun Context.bestLastKnownLocation(): Location? {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull(Location::getTime)
}
