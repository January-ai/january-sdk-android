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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.sp
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.FoodSuggestion
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.ServingOption
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun FoodSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    SearchField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding),
        placeholder = "Search foods",
        onSearch = onSearch,
        onClear = { onQueryChange("") },
    )
}

@Composable
internal fun DemoEmptyFoodState(modifier: Modifier = Modifier) {
    EmptyStateCard(
        icon = Icons.Outlined.RestaurantMenu,
        title = "Find a food",
        description = "Search ${LocalAppBranding.current.name}'s food database, then choose a serving and quantity.",
        modifier = modifier,
    )
}

@Composable
internal fun FoodSuggestionList(
    suggestions: List<FoodSuggestion>,
    loadingFoodId: String? = null,
    onSelect: (FoodSuggestion) -> Unit,
) {
    DemoCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 4.dp)) {
        suggestions.forEachIndexed { index, suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = loadingFoodId == null) { onSelect(suggestion) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = suggestion.name ?: "Unnamed food",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = JanuaryColors.Ink,
                    )
                    suggestion.brandName?.takeIf(String::isNotBlank)?.let { brand ->
                        Text(brand, style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
                    }
                }
                if (loadingFoodId == suggestion.id.value) {
                    LoadingSpinner(Modifier.size(22.dp), JanuaryColors.Subdued)
                }
            }
            if (index < suggestions.lastIndex) {
                HorizontalDivider(color = JanuaryColors.Divider)
            }
        }
    }
}

@Composable
internal fun FoodResultRow(
    food: FoodSearchItem,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FoodRow(
        name = food.name ?: "Unnamed food",
        subtitle = food.brandName,
        meta = listOfNotNull(
            food.calories?.let { "${it.roundToInt()} cal" },
            primaryServing(food)?.let { "${formatDemoNumber(it.quantity ?: 1.0)} ${it.unit.orEmpty()}" },
        ).joinToString(" · "),
        imageUrl = food.photoUrl,
        loading = loading,
        enabled = enabled,
        onClick = onClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServingSelectionSheet(
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
    var serving by remember(food.id) { mutableStateOf(servings.firstOrNull { it.isPrimary == true } ?: servings.first()) }
    var quantity by remember(food.id) { mutableStateOf(1.0) }
    var servingMenuOpen by remember { mutableStateOf(false) }
    val scale = quantity * serving.scalingFactor / serving.quantity.takeUnless { it == 0.0 }.orEmptyOne()

    AppModalSheet(title = "Choose serving", onDismiss = onDismiss, expanded = false) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DemoScreenPadding).padding(top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(food.name ?: "Unnamed food", style = MaterialTheme.typography.headlineMedium, color = JanuaryColors.Ink)
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
                                Text("${formatDemoNumber(serving.quantity ?: 1.0)} ${serving.unit.orEmpty()}", color = androidx.compose.ui.graphics.Color(0xFF6E5613))
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF6E5613))
                            }
                            DropdownMenu(expanded = servingMenuOpen, onDismissRequest = { servingMenuOpen = false }) {
                                servings.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("${formatDemoNumber(option.quantity ?: 1.0)} ${option.unit.orEmpty()}") },
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
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
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
                enabled = serving.id?.value != "0",
            )
        }
    }
}

internal fun Double?.orEmptyOne(): Double = this ?: 1.0

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
internal fun ServingMetric(label: String, value: Double?, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value?.let(::formatMetricNumber) ?: "—", style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp), fontFamily = FontFamily.Monospace)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = JanuaryColors.Muted)
        }
    }
}

internal fun primaryServing(food: FoodSearchItem): ServingOption? =
    food.servings.firstOrNull { it.isPrimary == true } ?: food.servings.firstOrNull()

internal fun formatDemoNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

internal fun formatMetricNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
