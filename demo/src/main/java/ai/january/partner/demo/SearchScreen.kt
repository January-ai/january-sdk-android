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
import androidx.compose.material3.CircularProgressIndicator
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
import coil3.compose.AsyncImage

private enum class SearchScope { FOODS, RESTAURANTS }
private enum class FoodMode { NAME, DESCRIPTION, BARCODE }
private enum class RestaurantMode { RESTAURANTS, MENU_ITEMS }
private data class SearchCity(val id: String, val name: String, val latitude: Double, val longitude: Double)

private val searchCities = listOf(
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
    var naturalResult by remember { mutableStateOf<SearchFoodsByNaturalLanguageResponse?>(null) }
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var menuItems by remember { mutableStateOf<List<RestaurantMenuItem>>(emptyList()) }
    var selectedFood by remember { mutableStateOf<FoodSearchItem?>(null) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var selectedMenuItem by remember { mutableStateOf<RestaurantMenuItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
        foodSuggestions = emptyList()
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
                        FoodMode.DESCRIPTION -> naturalResult = client.foods.searchNaturalLanguage(
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
            }.onFailure { error = it.message ?: "The request failed." }
            loading = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(scope, foodMode, category, query, client, state.partnerUserId) {
        val value = query.trim()
        val autocompleteCategory = when (category) {
            FoodCategory.GENERAL -> AutocompleteFoodCategory.GENERAL
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
            client = client,
            restaurant = restaurant,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            resultLimit = resultLimit,
            menuQuery = query,
            endUserId = state.partnerUserId,
            onBack = { selectedRestaurant = null },
            modifier = modifier,
        )
        return
    }

    selectedMenuItem?.let { item ->
        MenuItemDetailScreen(item, { selectedMenuItem = null }, modifier)
        return
    }

    Column(modifier.fillMaxSize()) {
        DemoScreen {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Text("Search", style = MaterialTheme.typography.displaySmall) }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it != autocompleteSuppressedQuery) autocompleteSuppressedQuery = null
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        placeholder = {
                            Text(
                                if (scope == SearchScope.RESTAURANTS) {
                                    if (restaurantMode == RestaurantMode.RESTAURANTS) "Restaurant name" else "Dish or restaurant"
                                } else when (foodMode) {
                                    FoodMode.NAME -> "Food name"
                                    FoodMode.DESCRIPTION -> "Describe what was eaten"
                                    FoodMode.BARCODE -> "6–14 digit barcode"
                                },
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = JanuaryColors.Control,
                            unfocusedContainerColor = JanuaryColors.Control,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submit() }),
                    )
                }
                if (foodSuggestions.isNotEmpty()) {
                    item {
                        DemoCard(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 4.dp)) {
                            foodSuggestions.forEachIndexed { index, suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            autocompleteSuppressedQuery = suggestion.name
                                            query = suggestion.name
                                            foodSuggestions = emptyList()
                                            submit(suggestion.name)
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = suggestion.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                if (index < foodSuggestions.lastIndex) {
                                    HorizontalDivider(color = JanuaryColors.Divider)
                                }
                            }
                        }
                    }
                }
                item {
                    ChoiceRow(
                        options = SearchScope.entries,
                        selected = scope,
                        label = { if (it == SearchScope.FOODS) "Foods" else "Restaurants" },
                        onSelect = { scope = it; clearResults() },
                    )
                }
                if (scope == SearchScope.FOODS) {
                    item {
                        ChoiceRow(
                            options = FoodMode.entries,
                            selected = foodMode,
                            label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                            onSelect = { foodMode = it; clearResults() },
                        )
                    }
                    if (foodMode == FoodMode.NAME) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "All" to null,
                                        "General" to FoodCategory.GENERAL,
                                        "Branded" to FoodCategory.BRANDED,
                                    ).forEach { (label, value) ->
                                        FilterChip(
                                            selected = category == value,
                                            onClick = { category = value },
                                            modifier = Modifier.heightIn(min = 44.dp),
                                            label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                labelColor = JanuaryColors.Body,
                                                selectedContainerColor = JanuaryColors.Ink,
                                                selectedLabelColor = JanuaryColors.Paper,
                                            ),
                                        )
                                    }
                                }
                                Row {
                                    FilterChip(
                                        selected = category == FoodCategory.RECIPE,
                                        onClick = { category = FoodCategory.RECIPE },
                                        modifier = Modifier.heightIn(min = 44.dp),
                                        label = { Text("Recipe", style = MaterialTheme.typography.labelLarge) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            labelColor = JanuaryColors.Body,
                                            selectedContainerColor = JanuaryColors.Ink,
                                            selectedLabelColor = JanuaryColors.Paper,
                                        ),
                                    )
                                }
                            }
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
                                        .addOnFailureListener { error = it.message ?: "Barcode scanning is unavailable." }
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
                        ChoiceRow(
                            options = RestaurantMode.entries,
                            selected = restaurantMode,
                            label = { if (it == RestaurantMode.RESTAURANTS) "Restaurants" else "Menu items" },
                            onSelect = { restaurantMode = it; clearResults() },
                        )
                    }
                    item {
                        DemoCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Search location", fontWeight = FontWeight.SemiBold)
                                    Text(if (isUsingCurrentLocation) "Current location" else selectedCity.name, color = JanuaryColors.Muted)
                                    Text("${"%.1f".format(radius / 1609.344)} mi · $resultLimit results", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                                }
                                IconButton(onClick = { showFilters = true }) { Icon(Icons.Outlined.FilterList, "Search filters") }
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
                            SectionLabel("Meal summary")
                            DemoCard { ScanStyleMacroStrip(nutrients.calories?.value, nutrients.protein?.value, nutrients.carbohydrates?.value, nutrients.totalFat?.value) }
                        }
                    }
                    if (natural.detections.isNotEmpty()) item { SectionLabel("Foods detected") }
                    items(natural.detections) { detection ->
                        DemoCard {
                            Text(detection.food.name, style = MaterialTheme.typography.titleMedium)
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
                    item { SectionLabel("Nearby restaurants") }
                    items(restaurants, key = { it.id }) { restaurant ->
                        RestaurantResultCard(restaurant) { selectedRestaurant = restaurant }
                    }
                }
                if (menuItems.isNotEmpty()) {
                    item { SectionLabel("Nearby menu items") }
                    items(menuItems, key = { it.id }) { item ->
                        MenuItemResultCard(item) { selectedMenuItem = item }
                    }
                }
                if (!loading && error == null && query.isNotBlank() && foodResults.isEmpty() && naturalResult == null && restaurants.isEmpty() && menuItems.isEmpty()) {
                    item {
                        EmptySearchCard(
                            if (scope == SearchScope.FOODS) "No foods found" else "No nearby matches",
                            if (scope == SearchScope.FOODS) "Try a different search." else "Try another name, location, or radius.",
                        )
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (showFilters) {
        RestaurantFiltersSheet(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.heightIn(min = 50.dp),
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = JanuaryColors.Surface,
                    activeContentColor = JanuaryColors.Ink,
                    activeBorderColor = JanuaryColors.ControlStrong,
                    inactiveContainerColor = JanuaryColors.ControlStrong,
                    inactiveContentColor = JanuaryColors.Muted,
                    inactiveBorderColor = JanuaryColors.Border,
                ),
                icon = {},
                label = { Text(label(option), maxLines = 1, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
private fun EmptySearchCard(title: String, message: String) {
    DemoCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun FoodResultCard(food: FoodSearchItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(JanuaryColors.Control), contentAlignment = Alignment.Center) {
            if (food.photoUrl != null) AsyncImage(food.photoUrl, food.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = JanuaryColors.Green)
        }
        Column(Modifier.weight(1f)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium)
            Text(
                listOfNotNull(food.brandName, food.calories?.let { "${it.toInt()} cal" }, food.servings.firstOrNull()?.unit).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun RestaurantResultCard(restaurant: Restaurant, onClick: () -> Unit) {
    DemoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(restaurant.name, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(restaurant.city, restaurant.distance?.let { "%.1f mi".format(it / 1609.344) }).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun SimpleResultCard(title: String, subtitle: String) {
    DemoCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MenuItemResultCard(item: RestaurantMenuItem, onClick: () -> Unit) {
    DemoCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(JanuaryColors.Control), contentAlignment = Alignment.Center) {
                if (item.photoUrl != null) AsyncImage(item.photoUrl, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.Restaurant, null, tint = JanuaryColors.Green)
            }
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(item.restaurantName, color = JanuaryColors.Muted)
                item.calories?.let { Text("${it.toInt()} cal", style = MaterialTheme.typography.bodySmall) }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = JanuaryColors.Subdued)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDetailScreen(
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
        runCatching { sdk.foods.getFood(GetFoodRequest(food.id, state.partnerUserId)) }
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
        TopAppBar(
            title = { Text("Food details") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back to results") }
            },
        )
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)).background(JanuaryColors.Control),
                    contentAlignment = Alignment.Center,
                ) {
                    if (detailFood.photoUrl != null) AsyncImage(detailFood.photoUrl, detailFood.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = JanuaryColors.Green)
                }
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Text("Glucose response", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showGlucoseSheet = false }, modifier = Modifier.weight(1f)) { Text("Done") }
                }
                Text(detailFood.name, style = MaterialTheme.typography.titleMedium)
                Text("${formatNumber(quantity)} ${serving!!.unit}", color = JanuaryColors.Muted, fontFamily = FontFamily.Monospace)
                if (loading) {
                    DemoCard {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
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

private fun servingLabel(serving: ServingOption): String = buildString {
    append("${formatNumber(serving.quantity)} ${serving.unit}")
    serving.weightGrams?.let { append(" · ${formatNumber(it)} g") }
}

@Composable
private fun NutritionRow(label: String, value: Double, unit: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("${formatNumber(value)}${if (unit.isEmpty()) "" else " $unit"}", fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ScanStyleMacroStrip(calories: Double?, protein: Double?, carbs: Double?, fat: Double?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Metric("Calories", calories, "cal")
        Metric("Protein", protein, "g")
        Metric("Carbs", carbs, "g")
        Metric("Fat", fat, "g")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestaurantFiltersSheet(
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Search filters", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onDismiss) { Text("Done") }
            }
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
private fun RestaurantDetailScreen(
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
        TopAppBar(
            title = { Text("Restaurant") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
        )
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
private fun MenuItemDetailScreen(item: RestaurantMenuItem, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Menu item") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlternativesSheet(state: DemoState, food: FoodSearchItem, onDismiss: () -> Unit) {
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Close") }
                Text("Alternatives", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.weight(.2f))
            }
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
private fun <T> DietChoices(values: List<T>, selected: Set<T>, onChange: (Set<T>) -> Unit, label: (T) -> String) {
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

private fun String.normalizedRestaurantName(): String = substringBefore("(").lowercase().filter { it.isLetterOrDigit() || it == ' ' }.trim()

@Composable
private fun Metric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let(::formatNumber) ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text(unit, style = MaterialTheme.typography.bodySmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Suppress("MissingPermission")
private fun Context.bestLastKnownLocation(): Location? {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull(Location::getTime)
}
