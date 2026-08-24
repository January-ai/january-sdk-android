package ai.january.partner.demo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.january.partner.foods.FoodCategory
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.LookupFoodByBarcodeRequest
import ai.january.partner.foods.SearchFoodsByNaturalLanguageRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.ServingOption
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
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

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
    var scope by remember { mutableStateOf(SearchScope.FOODS) }
    var foodMode by remember { mutableStateOf(FoodMode.NAME) }
    var restaurantMode by remember { mutableStateOf(RestaurantMode.RESTAURANTS) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<FoodCategory?>(null) }
    var foodResults by remember { mutableStateOf<List<FoodSearchItem>>(emptyList()) }
    var naturalResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var restaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var menuItems by remember { mutableStateOf<List<RestaurantMenuItem>>(emptyList()) }
    var selectedFood by remember { mutableStateOf<FoodSearchItem?>(null) }
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var latitude by remember { mutableDoubleStateOf(37.7749) }
    var longitude by remember { mutableDoubleStateOf(-122.4194) }
    var selectedCity by remember { mutableStateOf(searchCities.first()) }
    var isUsingCurrentLocation by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var locationLabel by remember { mutableStateOf("Preset city · ${searchCities.first().name}") }

    fun clearResults() {
        foodResults = emptyList(); naturalResults = emptyList(); restaurants = emptyList(); menuItems = emptyList(); error = null
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
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                when (scope) {
                    SearchScope.FOODS -> when (foodMode) {
                        FoodMode.NAME -> foodResults = client.foods.search(
                            SearchFoodsRequest(value, category, 10, state.partnerUserId),
                        ).items
                        FoodMode.DESCRIPTION -> naturalResults = client.foods.searchNaturalLanguage(
                            SearchFoodsByNaturalLanguageRequest(value, state.partnerUserId),
                        ).detections.map { it.food.name }
                        FoodMode.BARCODE -> foodResults = client.foods.lookupBarcode(
                            LookupFoodByBarcodeRequest(value, state.partnerUserId),
                        ).items
                    }
                    SearchScope.RESTAURANTS -> when (restaurantMode) {
                        RestaurantMode.RESTAURANTS -> restaurants = client.restaurants.search(
                            SearchRestaurantsRequest(value, latitude, longitude, endUserId = state.partnerUserId),
                        ).items
                        RestaurantMode.MENU_ITEMS -> menuItems = client.restaurants.searchMenuItems(
                            SearchRestaurantsRequest(value, latitude, longitude, endUserId = state.partnerUserId),
                        ).items
                    }
                }
            }.onFailure { error = it.message ?: "The request failed." }
            loading = false
        }
    }

    selectedFood?.let { food ->
        FoodDetailScreen(state = state, food = food, onBack = { selectedFood = null }, modifier = modifier)
        return
    }

    Column(modifier.fillMaxSize()) {
        DemoTopBar("Search", settingsAction)
        DemoScreen {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (scope == SearchScope.FOODS) "Food name" else "Restaurant or dish") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { submit() }),
                    )
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "All" to null,
                                    "General" to FoodCategory.GENERAL,
                                    "Branded" to FoodCategory.BRANDED,
                                    "Recipe" to FoodCategory.RECIPE,
                                ).forEach { (label, value) ->
                                    FilterChip(selected = category == value, onClick = { category = value }, label = { Text(label) })
                                }
                            }
                        }
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
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Search location", fontWeight = FontWeight.SemiBold)
                                ExposedDropdownMenuBox(
                                    expanded = cityMenuExpanded,
                                    onExpandedChange = { cityMenuExpanded = it },
                                ) {
                                    OutlinedTextField(
                                        value = if (isUsingCurrentLocation) "Current location" else selectedCity.name,
                                        onValueChange = {},
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                        readOnly = true,
                                        label = { Text("U.S. city") },
                                        leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityMenuExpanded) },
                                    )
                                    ExposedDropdownMenu(
                                        expanded = cityMenuExpanded,
                                        onDismissRequest = { cityMenuExpanded = false },
                                    ) {
                                        searchCities.forEach { city ->
                                            DropdownMenuItem(
                                                text = { Text(city.name) },
                                                onClick = {
                                                    selectedCity = city
                                                    latitude = city.latitude
                                                    longitude = city.longitude
                                                    isUsingCurrentLocation = false
                                                    locationLabel = "Preset city · ${city.name}"
                                                    cityMenuExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "%.4f, %.4f".format(latitude, longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Button(onClick = ::requestLocation, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null)
                                    Text(" Use my current location")
                                }
                                Text(locationLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (query.isBlank()) {
                    item {
                        EmptySearchCard(
                            title = if (scope == SearchScope.FOODS) "Find a food" else "Search nearby",
                            message = if (scope == SearchScope.FOODS) "Search January's database, then choose a serving and quantity." else "Find restaurants and menu items near a location.",
                        )
                    }
                }
                item {
                    Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth(), enabled = query.isNotBlank() && client != null && !loading) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text(if (scope == SearchScope.FOODS) "Search foods" else "Search nearby")
                    }
                }
                if (client == null) item { ApiKeyRequiredCard() }
                error?.let { message -> item { ErrorCard(message) { submit() } } }
                if (foodResults.isNotEmpty()) {
                    item { SectionLabel("Results · January food database") }
                    items(foodResults, key = { it.id.value }) { food ->
                        FoodResultCard(food, onClick = { selectedFood = food })
                    }
                }
                if (naturalResults.isNotEmpty()) {
                    item { SectionLabel("Foods detected") }
                    items(naturalResults) { name -> SimpleResultCard(name, "Parsed from meal description") }
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
                        SimpleResultCard(item.name, listOfNotNull(item.restaurantName, item.calories?.let { "${it.toInt()} cal" }).joinToString(" · "))
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    selectedRestaurant?.let { restaurant ->
        RestaurantSheet(
            restaurant = restaurant,
            onDismiss = { selectedRestaurant = null },
            onMenu = {
                selectedRestaurant = null
                scope = SearchScope.RESTAURANTS
                restaurantMode = RestaurantMode.MENU_ITEMS
                query = restaurant.name
                submit(restaurant.name)
            },
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
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option), maxLines = 1) },
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
    DemoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
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
}

@Composable
private fun RestaurantResultCard(restaurant: Restaurant, onClick: () -> Unit) {
    DemoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(restaurant.name, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(restaurant.city, restaurant.distance?.let { "%.1f mi".format(it) }).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDetailScreen(
    state: DemoState,
    food: FoodSearchItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val initialServing = remember(food) { food.servings.firstOrNull { it.isPrimary } ?: food.servings.firstOrNull() }
    var serving by remember(food) { mutableStateOf(initialServing) }
    var quantity by remember(food) { mutableDoubleStateOf(initialServing?.quantity?.takeIf { it > 0 } ?: 1.0) }
    var servingMenuExpanded by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf<GlucosePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val baseline = serving?.quantity?.takeIf { it > 0 } ?: 1.0
    val scale = serving?.let { quantity * it.scalingFactor / baseline } ?: quantity

    fun predict() {
        val sdk = state.client ?: return
        val selectedServing = serving ?: return
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
                        foods = listOf(FoodSelection(food.id.value, ServingSelection(selectedServing.id.value, quantity))),
                        startTime = OffsetDateTime.now(),
                        endUserId = state.partnerUserId,
                        timezone = ZoneId.systemDefault().id,
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
                DemoCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(food.name, style = MaterialTheme.typography.headlineMedium)
                food.brandName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                DemoCard {
                    if (food.servings.isEmpty()) {
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
                                food.servings.forEach { option ->
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
                        Metric("Calories", food.calories?.times(scale), "cal")
                        Metric("Protein", food.protein?.times(scale), "g")
                        Metric("Carbs", food.carbohydrates?.times(scale), "g")
                        Metric("Fat", food.totalFat?.times(scale), "g")
                    }
                }

                val facts = listOf(
                    "Net carbohydrates" to food.netCarbohydrates,
                    "Saturated fat" to food.saturatedFat,
                    "Fiber" to food.fiber,
                    "Total sugars" to food.totalSugars,
                    "Added sugars" to food.addedSugars,
                    "Sodium" to food.sodium,
                    "Potassium" to food.potassium,
                    "Cholesterol" to food.cholesterol,
                ).filter { it.second != null }
                if (facts.isNotEmpty() || food.glycemicIndex != null || food.glycemicLoad != null) {
                    SectionLabel("Nutrition facts")
                    DemoCard {
                        facts.forEachIndexed { index, (label, value) ->
                            NutritionRow(label, value!! * scale, if (label in listOf("Sodium", "Potassium", "Cholesterol")) "mg" else "g")
                            if (index < facts.lastIndex) HorizontalDivider()
                        }
                        food.glycemicIndex?.let { NutritionRow("Glycemic index", it, "") }
                        food.glycemicLoad?.let { NutritionRow("Glycemic load", it, "") }
                    }
                }

                Button(
                    onClick = ::predict,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serving != null && state.client != null && !loading,
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    else Text("Check glucose")
                }
                if (state.client == null) ApiKeyRequiredCard()
                error?.let { ErrorCard(it, ::predict) }
                prediction?.let { GlucosePredictionResult(it, food, quantity) }
                Spacer(Modifier.height(20.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestaurantSheet(restaurant: Restaurant, onDismiss: () -> Unit, onMenu: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(restaurant.name, style = MaterialTheme.typography.headlineMedium)
            restaurant.address1?.let { Text(it) }
            restaurant.city?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Button(onClick = onMenu, modifier = Modifier.fillMaxWidth()) { Text("Show menu items") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

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
