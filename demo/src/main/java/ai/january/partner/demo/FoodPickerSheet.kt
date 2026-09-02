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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ai.january.partner.FoodId
import ai.january.partner.foods.AutocompleteFoodsRequest
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.foods.FoodSuggestion
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.ServingOption
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
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
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<FoodSuggestion>>(emptyList()) }
    var autocompleteSuppressedQuery by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<FoodSearchItem>>(emptyList()) }
    var chosenFood by remember { mutableStateOf<FoodSearchItem?>(null) }
    var hydratingFoodId by remember { mutableStateOf<FoodId?>(null) }
    var failedFoodId by remember { mutableStateOf<FoodId?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    fun search(queryOverride: String? = null) {
        val value = (queryOverride ?: query).trim()
        if (value.isEmpty()) return
        autocompleteSuppressedQuery = value
        suggestions = emptyList()
        loading = true
        error = null
        failedFoodId = null
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        coroutineScope.launch {
            runCatching { client.foods.search(SearchFoodsRequest(value, endUserId = state.partnerUserId)).items }
                .onSuccess { results = it }
                .onFailure { error = it }
            loading = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    fun hydrate(foodId: FoodId) {
        if (hydratingFoodId != null) return
        hydratingFoodId = foodId
        error = null
        failedFoodId = null
        coroutineScope.launch {
            runCatching {
                client.foods.get(GetFoodRequest(foodId, state.partnerUserId))
            }
                .onSuccess { chosenFood = it }
                .onFailure {
                    error = it
                    failedFoodId = foodId
                }
            hydratingFoodId = null
        }
    }

    LaunchedEffect(query, state.partnerUserId) {
        val value = query.trim()
        if (value.length !in 2..64 || value == autocompleteSuppressedQuery) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        suggestions = runCatching {
            client.foods.autocomplete(
                AutocompleteFoodsRequest(query = value, limit = 8, endUserId = state.partnerUserId),
            ).items
        }.getOrDefault(emptyList())
    }

    AppModalSheet(title = "Add food", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FoodSearchField(
                query = query,
                onQueryChange = {
                    query = it
                    if (it != autocompleteSuppressedQuery) {
                        autocompleteSuppressedQuery = null
                        results = emptyList()
                        error = null
                    }
                    if (it.isEmpty()) {
                        suggestions = emptyList()
                        results = emptyList()
                        error = null
                    }
                },
                onSearch = { search() },
            )
            if (suggestions.isNotEmpty()) {
                Box(Modifier.padding(horizontal = DemoScreenPadding)) {
                    FoodSuggestionList(
                        suggestions = suggestions,
                        onSelect = { suggestion ->
                            val suggestionName = suggestion.name ?: return@FoodSuggestionList
                            autocompleteSuppressedQuery = suggestionName
                            query = suggestionName
                            suggestions = emptyList()
                            search(suggestionName)
                        },
                    )
                }
            }
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        LoadingSpinner(color = JanuaryColors.Green)
                    }
                }
                error != null -> {
                    Box(Modifier.padding(horizontal = DemoScreenPadding)) {
                        ErrorCard(error!!) { failedFoodId?.let(::hydrate) ?: search() }
                    }
                }
                suggestions.isEmpty() && results.isEmpty() -> {
                    EmptyStateCard(Icons.Outlined.RestaurantMenu, "Find a food", "Start typing for suggestions, or search January’s food database.", Modifier.padding(horizontal = DemoScreenPadding))
                }
                results.isNotEmpty() -> {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(
                            "Results · January food database",
                            Modifier.padding(horizontal = DemoScreenPadding + 6.dp),
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = DemoScreenPadding),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = JanuaryColors.Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            LazyColumn {
                                itemsIndexed(results, key = { _, food -> food.id.value }) { index, food ->
                                    FoodResultRow(
                                        food = food,
                                        loading = hydratingFoodId == food.id,
                                        enabled = hydratingFoodId == null,
                                        onClick = { hydrate(food.id) },
                                    )
                                    if (index < results.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 22.dp),
                                            color = JanuaryColors.Divider,
                                        )
                                    }
                                }
                            }
                        }
                        Text("Photos load from January’s food database.", modifier = Modifier.padding(horizontal = DemoScreenPadding).padding(bottom = 20.dp), style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
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
