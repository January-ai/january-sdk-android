package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.ServingOption
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class DemoSelectedFood(
    val food: FoodSearchItem,
    val serving: ServingOption,
    val quantity: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodPickerSheet(
    state: DemoState,
    onDismiss: () -> Unit,
    onSelect: (DemoSelectedFood) -> Unit,
) {
    val client = state.client ?: return
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodSearchItem>>(emptyList()) }
    var chosenFood by remember { mutableStateOf<FoodSearchItem?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun search() {
        val value = query.trim()
        if (value.isEmpty()) return
        loading = true
        error = null
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        coroutineScope.launch {
            runCatching { client.foods.search(SearchFoodsRequest(value, endUserId = state.partnerUserId)).items }
                .onSuccess { results = it }
                .onFailure { error = it.message ?: "Food search could not be completed." }
            loading = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JanuaryColors.Paper,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PickerHeader(title = "Add food", onCancel = onDismiss)
            FoodSearchField(
                query = query,
                onQueryChange = {
                    query = it
                    if (it.isEmpty()) {
                        results = emptyList()
                        error = null
                    }
                },
                onSearch = ::search,
            )
            when {
                loading -> {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding),
                        color = JanuaryColors.Ink,
                        trackColor = JanuaryColors.Control,
                    )
                }
                error != null -> {
                    Box(Modifier.padding(horizontal = DemoScreenPadding)) { ErrorCard(error!!, ::search) }
                }
                results.isEmpty() -> {
                    DemoEmptyFoodState(Modifier.padding(horizontal = DemoScreenPadding))
                }
                else -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(
                            "Results · January food database",
                            Modifier.padding(horizontal = DemoScreenPadding + 6.dp),
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = JanuaryColors.Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            LazyColumn {
                                itemsIndexed(results, key = { _, food -> food.id.value }) { index, food ->
                                    FoodResultRow(food = food, onClick = { chosenFood = food })
                                    if (index < results.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 22.dp),
                                            color = JanuaryColors.Divider,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    chosenFood?.let { food ->
        ServingSelectionSheet(
            food = food,
            onDismiss = { chosenFood = null },
            onSelect = {
                focusManager.clearFocus()
                onSelect(it)
                chosenFood = null
                onDismiss()
            },
        )
    }
}

@Composable
private fun PickerHeader(title: String, onCancel: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding)) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
            Text("Cancel", color = androidx.compose.ui.graphics.Color(0xFF6E5613))
        }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.titleLarge,
            color = JanuaryColors.Ink,
        )
    }
}

@Composable
private fun FoodSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding),
        placeholder = { Text("Search foods") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = JanuaryColors.Control,
            unfocusedContainerColor = JanuaryColors.Control,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
}

@Composable
private fun DemoEmptyFoodState(modifier: Modifier = Modifier) {
    DemoCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, tint = JanuaryColors.Green)
            Text("Find a food", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "Search January's food database, then choose a serving and quantity.",
                color = JanuaryColors.Body,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FoodResultRow(food: FoodSearchItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SubcomposeAsyncImage(
            model = food.photoUrl,
            contentDescription = null,
            modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(JanuaryColors.Control),
            contentScale = ContentScale.Crop,
            loading = { FoodImagePlaceholder() },
            error = { FoodImagePlaceholder() },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Ink)
            food.brandName?.takeIf(String::isNotBlank)?.let {
                Text(it, color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                food.calories?.let { Text("${it.roundToInt()} cal") }
                primaryServing(food)?.let { Text("${formatDemoNumber(it.quantity)} ${it.unit}") }
            }
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = JanuaryColors.Subdued)
    }
}

@Composable
private fun FoodImagePlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, tint = JanuaryColors.Green)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServingSelectionSheet(
    food: FoodSearchItem,
    onDismiss: () -> Unit,
    onSelect: (DemoSelectedFood) -> Unit,
) {
    val servings = food.servings.ifEmpty {
        listOf(
            ServingOption(
                id = ai.january.partner.ServingId(0),
                quantity = 1.0,
                unit = "serving",
                scalingFactor = 1.0,
                weightGrams = null,
                isPrimary = true,
            ),
        )
    }
    var serving by remember(food.id) { mutableStateOf(servings.firstOrNull { it.isPrimary } ?: servings.first()) }
    var quantity by remember(food.id) { mutableStateOf(1.0) }
    var servingMenuOpen by remember { mutableStateOf(false) }
    val scale = quantity * serving.scalingFactor / serving.quantity.takeUnless { it == 0.0 }.orEmptyOne()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JanuaryColors.Paper,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color(0xFF6E5613), fontWeight = FontWeight.SemiBold)
                }
                SectionLabel("Choose serving", Modifier.align(Alignment.Center))
            }
            Text(food.name, style = MaterialTheme.typography.headlineMedium, color = JanuaryColors.Ink)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = JanuaryColors.Paper,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, JanuaryColors.Border),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Serving", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Box {
                            TextButton(onClick = { servingMenuOpen = true }) {
                                Text("${formatDemoNumber(serving.quantity)} ${serving.unit}", color = androidx.compose.ui.graphics.Color(0xFF6E5613))
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF6E5613))
                            }
                            DropdownMenu(expanded = servingMenuOpen, onDismissRequest = { servingMenuOpen = false }) {
                                servings.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${formatDemoNumber(option.quantity)} ${option.unit}") },
                                        onClick = { serving = option; servingMenuOpen = false },
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = JanuaryColors.Border)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        DemoQuantityButton(symbol = "−", primary = false) { quantity = maxOf(0.25, quantity - 0.25) }
                        Text(
                            formatDemoNumber(quantity),
                            modifier = Modifier.padding(horizontal = 14.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                        DemoQuantityButton(symbol = "+", primary = true) { quantity += 0.25 }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ServingMetric("Calories", food.calories?.times(scale), "cal")
                ServingMetric("Carbs", food.carbohydrates?.times(scale), "g")
                ServingMetric("Protein", food.protein?.times(scale), "g")
                ServingMetric("Fat", food.totalFat?.times(scale), "g")
            }
            DemoPrimaryButton(
                text = "Add to meal",
                onClick = { onSelect(DemoSelectedFood(food, serving, quantity)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = serving.id.value != 0L,
            )
        }
    }
}

private fun Double?.orEmptyOne(): Double = this ?: 1.0

@Composable
internal fun DemoQuantityButton(symbol: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = if (primary) JanuaryColors.Ink else JanuaryColors.Control,
        contentColor = if (primary) JanuaryColors.Paper else JanuaryColors.Ink,
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.5.dp, JanuaryColors.Border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun ServingMetric(label: String, value: Double?, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value?.let(::formatMetricNumber) ?: "—", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
        }
    }
}

internal fun primaryServing(food: FoodSearchItem): ServingOption? =
    food.servings.firstOrNull { it.isPrimary } ?: food.servings.firstOrNull()

internal fun formatDemoNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

private fun formatMetricNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
