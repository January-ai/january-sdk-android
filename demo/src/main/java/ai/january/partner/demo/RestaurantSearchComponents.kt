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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.UnfoldMore
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
import ai.january.partner.JanuaryException
import ai.january.partner.PartnerUserId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestaurantFiltersSheet(selectedCity: SearchCity, onCity: (SearchCity) -> Unit, radius: Double, onRadius: (Double) -> Unit, limit: Int, onLimit: (Int) -> Unit, locationLabel: String, onCurrentLocation: () -> Unit, onDismiss: () -> Unit, latitude: Double, longitude: Double) {
    var cityMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    AppModalSheet(title = "Search filters", onDismiss = onDismiss) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding).padding(top = 28.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SectionLabel("Location")
            DemoCard {
                Column {
                    Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.LocationOn, null, tint = JanuaryColors.Green)
                        Column { Text("Location access", style = MaterialTheme.typography.titleMedium); Text(if (hasLocation) "Location access allowed" else "Location access not granted", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted) }
                    }
                    HorizontalDivider(color = JanuaryColors.Divider)
                    Box {
                        Row(Modifier.fillMaxWidth().clickable { cityMenu = true }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Search city", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(if (locationLabel.startsWith("Current")) "Current location" else selectedCity.name, style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Green)
                            Icon(Icons.Outlined.UnfoldMore, "Choose search city", Modifier.size(18.dp), tint = JanuaryColors.Green)
                        }
                        androidx.compose.material3.DropdownMenu(cityMenu, { cityMenu = false }) { searchCities.forEach { city -> DropdownMenuItem({ Text(city.name) }, { onCity(city); cityMenu = false }) } }
                    }
                    HorizontalDivider(color = JanuaryColors.Divider)
                    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coordinates", style = MaterialTheme.typography.titleMedium)
                        Text("%.3f, %.3f".format(latitude, longitude), style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                    }
                }
            }
            DemoOutlinedButton("Use my current location", onCurrentLocation, Modifier.fillMaxWidth(), icon = { Icon(Icons.Outlined.LocationOn, null) })
            if (locationLabel.startsWith("Location denied") || locationLabel.startsWith("Location unavailable")) {
                ErrorCard(locationLabel, onCurrentLocation)
                DemoOutlinedButton("Open location settings", { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))) }, Modifier.fillMaxWidth())
            }
            SectionLabel("Search radius")
            DemoCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nearby distance", style = MaterialTheme.typography.titleMedium)
                        Text("${"%.1f".format(radius / 1609.344)} mi", color = JanuaryColors.Green, fontFamily = FontFamily.Monospace)
                    }
                    Slider(radius.toFloat(), { onRadius(it.toDouble()) }, valueRange = 500f..17000f, steps = 32,
                        thumb = { androidx.compose.material3.Surface(Modifier.size(24.dp), shape = androidx.compose.foundation.shape.CircleShape, color = Color.White, shadowElevation = 3.dp) {} },
                        track = {
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(JanuaryColors.ControlStrong)) {
                                Box(Modifier.fillMaxWidth(((radius - 500.0) / 16500.0).toFloat().coerceIn(0f, 1f)).height(4.dp).background(JanuaryColors.Green))
                            }
                        })
                    Text("Search within ${"%,d".format(radius.toInt())} meters of the selected location.", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                }
            }
            SectionLabel("Results")
            DemoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Maximum results", style = MaterialTheme.typography.titleMedium)
                        Text("Up to $limit nearby matches", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                    }
                    QuantityStepper(limit.toDouble(), { onLimit(it.toInt()) }, minimum = 1.0, maximum = 100.0, step = 1.0)
                }
            }
            DemoPrimaryButton("Apply filters", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun RestaurantDetailScreen(state: DemoState, restaurant: Restaurant, latitude: Double, longitude: Double, radius: Double, resultLimit: Int, endUserId: PartnerUserId?, onBack: () -> Unit, modifier: Modifier) {
    val client = state.client
    var items by remember(restaurant.id) { mutableStateOf<List<RestaurantMenuItem>>(emptyList()) }
    var loading by remember(restaurant.id) { mutableStateOf(true) }
    var error by remember(restaurant.id) { mutableStateOf<Throwable?>(null) }
    var selected by remember { mutableStateOf<RestaurantMenuItem?>(null) }
    var attempt by remember { mutableStateOf(0) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    LaunchedEffect(restaurant.id, attempt) {
        if (client == null) { loading = false; return@LaunchedEffect }
        loading = true; error = null
        runCatching {
            try {
                val menu = mutableListOf<RestaurantMenuItem>()
                do {
                    val page = client.restaurants.getMenuItems(ai.january.partner.restaurants.GetRestaurantMenuItemsRequest(restaurant.id, offset = menu.size, endUserId = endUserId))
                    menu.addAll(page.items.mapIndexed { index, entry ->
                        RestaurantMenuItem(
                            type = "menu_item", id = entry.id ?: "menu-${menu.size + index}",
                            name = entry.name, restaurantName = restaurant.name,
                            calories = entry.calories, protein = entry.protein,
                            carbohydrates = entry.carbohydrates, netCarbohydrates = entry.netCarbohydrates,
                            totalFat = entry.totalFat, fiber = entry.fiber,
                            totalSugars = entry.totalSugars, addedSugars = entry.addedSugars,
                            glycemicIndex = entry.glycemicIndex, glycemicLoad = entry.glycemicLoad,
                            servings = entry.servings,
                        )
                    })
                } while (page.items.size == 100)
                menu.toList()
            } catch (failure: JanuaryException) {
                if (!failure.isRestaurantMenuUnavailable()) throw failure
                val page = client.restaurants.searchMenuItems(SearchRestaurantsRequest(
                    query = restaurant.name ?: "Restaurant",
                    latitude = latitude,
                    longitude = longitude,
                    radius = radius,
                    limit = resultLimit.coerceIn(1, 100),
                    endUserId = endUserId,
                ))
                val selectedName = normalizedRestaurantName(restaurant.name.orEmpty())
                page.items.filter { normalizedRestaurantName(it.restaurantName.orEmpty()) == selectedName }
            }
        }.onSuccess { items = it }.onFailure { error = it }
        loading = false
    }
    selected?.let { MenuItemDetailScreen(state, it, { selected = null }, modifier); return }
    AppScreenScaffold(
        title = "Restaurant", modifier = modifier,
        leading = { AppNavigationButton(AppNavigationButtonKind.Back, title = "Back from Restaurant", onClick = onBack) },
    ) {
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(restaurant.name ?: "Restaurant", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                DemoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Location")
                        restaurant.city?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("City"); Text(it, color = JanuaryColors.Muted) } }
                        restaurant.address1?.let { Text(it) }; restaurant.address2?.let { Text(it) }
                        restaurant.distance?.let { HorizontalDivider(color = JanuaryColors.Divider); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Distance"); Text("${"%.1f".format(it / 1609.344)} mi", color = JanuaryColors.Muted) } }
                    }
                }
                SectionLabel("Menu items")
                when {
                    loading -> DemoCard { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { LoadingSpinner(color = JanuaryColors.Green); Text("Loading menu", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Muted) } }
                    error != null -> ErrorCard(error!!) { attempt++ }
                    items.isEmpty() -> EmptySearchCard("No menu items found", "January did not return menu items for this restaurant.")
                    else -> DemoCard {
                        items.forEachIndexed { index, item ->
                            MenuItemRow(item, Modifier.clickable { selected = item }.padding(vertical = 12.dp))
                            if (index < items.lastIndex) HorizontalDivider(color = JanuaryColors.Divider)
                        }
                    }
                }
                DetailDisclosure {
                    restaurant.isChain?.let { Text("Type · ${if (it) "Chain" else "Independent"}") }
                    Text("Restaurant ID · ${restaurant.id}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun JanuaryException.isRestaurantMenuUnavailable(): Boolean = httpStatus == 404

private fun normalizedRestaurantName(value: String): String = value
    .substringBefore('(')
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

@Composable
internal fun MenuItemDetailScreen(state: DemoState, item: RestaurantMenuItem, onBack: () -> Unit, modifier: Modifier) {
    var serving by remember(item.id) { mutableStateOf(item.servings.firstOrNull { it.isPrimary == true } ?: item.servings.firstOrNull()) }
    var quantity by remember(item.id) { mutableDoubleStateOf(serving?.quantity ?: 1.0) }
    var showGlucose by remember { mutableStateOf(false) }
    val foodId = item.id.toLongOrNull()?.let { ai.january.partner.FoodId(it) }
    androidx.activity.compose.BackHandler(onBack = onBack)
    AppScreenScaffold(
        title = "Menu item", modifier = modifier,
        leading = { AppNavigationButton(AppNavigationButtonKind.Back, title = "Back from Menu item", onClick = onBack) },
    ) {
        DemoScreen {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(JanuaryColors.Control), contentAlignment = Alignment.Center) {
                    NetworkImage(item.photoUrl, item.name ?: "Menu item", Modifier.fillMaxSize().padding(18.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit, placeholderSize = 44.dp)
                }
                Text(item.name ?: "Unnamed menu item", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(item.restaurantName ?: "Restaurant", color = JanuaryColors.Muted)
                DemoCard { ScanStyleMacroStrip(item.calories, item.protein, item.carbohydrates, item.totalFat) }
                DemoCard { NutritionList(listOfNotNull(
                    item.netCarbohydrates?.let { NutritionValue("Net carbohydrates", "${formatNumber(it)} g") },
                    item.fiber?.let { NutritionValue("Fiber", "${formatNumber(it)} g") },
                    item.totalSugars?.let { NutritionValue("Total sugars", "${formatNumber(it)} g") },
                    item.addedSugars?.let { NutritionValue("Added sugars", "${formatNumber(it)} g") },
                    item.glycemicIndex?.let { NutritionValue("Glycemic index", formatNumber(it)) },
                    item.glycemicLoad?.let { NutritionValue("Glycemic load", formatNumber(it)) },
                )) }
                ServingControls(item.servings, serving, quantity, { serving = it; quantity = it.quantity ?: 1.0 }, { quantity = it }, menuItem = true)
                DemoPrimaryButton("See glucose impact", { showGlucose = true }, Modifier.fillMaxWidth(), enabled = serving != null && foodId != null && state.client != null,
                    icon = { Icon(Icons.Outlined.MonitorHeart, null) })
                DetailDisclosure { Text("Menu item ID · ${item.id}", style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (showGlucose && serving != null && foodId != null && state.client != null) FoodGlucoseSheet(state.client!!, foodId, item.name ?: "Menu item", serving!!, quantity, state.partnerUserId, state.timezone) { showGlucose = false }
}
