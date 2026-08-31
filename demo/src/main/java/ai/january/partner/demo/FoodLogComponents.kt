package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.january.partner.foods.ServingOption
import ai.january.partner.models.NutrientAmount

@Composable
internal fun FoodLogCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    DemoCard(modifier, content = content)
}

@Composable
internal fun FoodLogUserCard(userId: String?, timezone: String, onSave: (String) -> Unit, onSettings: () -> Unit) {
    var draft by remember(userId) { mutableStateOf(userId.orEmpty()) }
    val gold = Color(0xFF6E5613)
    Surface(shape = RoundedCornerShape(28.dp), color = JanuaryColors.GoldContainer, border = BorderStroke(1.5.dp, gold.copy(alpha = 0.28f))) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(if (userId == null) Icons.Outlined.PersonOutline else Icons.Filled.AccountCircle, null, Modifier.size(24.dp), tint = gold)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (userId == null) "Identify the user first" else "Logging for this user", style = MaterialTheme.typography.titleMedium)
                    Text(if (userId == null) "Food logs are stored and fetched by your app’s stable user ID." else "New logs and saved-log searches use this identity.", color = JanuaryColors.Body, fontSize = 15.sp, lineHeight = 20.sp)
                }
            }
            if (userId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SelectionContainer { Text(userId, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                    Text(timezone, fontSize = 12.sp, color = JanuaryColors.Muted)
                }
            } else {
                TextField(
                    value = draft, onValueChange = { draft = it }, placeholder = { Text("Stable end user ID", fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = JanuaryColors.Surface, unfocusedContainerColor = JanuaryColors.Surface, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                )
                DemoPrimaryButton("Use this user ID", { onSave(draft.trim()) }, Modifier.fillMaxWidth(), enabled = draft.isNotBlank())
            }
            TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) {
                Text(if (userId == null) "Set user ID and timezone in Settings" else "Change user or timezone", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = gold)
            }
        }
    }
}

@Composable
internal fun FoodLogTimeSpanPicker(span: FoodLogTimeSpan, range: FoodLogDateRange, onSelect: (FoodLogTimeSpan) -> Unit) {
    FoodLogCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SegmentedControl(FoodLogTimeSpan.entries, span, { it.title }, onSelect)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Text("Dates", style = MaterialTheme.typography.titleMedium)
                Text(range.displayText(), Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Muted, textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
internal fun FoodLogMealIcon() {
    Surface(Modifier.size(28.dp), shape = CircleShape, color = JanuaryColors.Green) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Restaurant, null, Modifier.size(18.dp), tint = Color.White) }
    }
}

@Composable
internal fun FoodLogSelectedFoodCard(selected: DemoSelectedFood, onServingChange: (ServingOption) -> Unit, onQuantityChange: (Double) -> Unit, onRemove: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    FoodLogCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                FoodLogMealIcon()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(selected.food.name, style = MaterialTheme.typography.titleMedium)
                    selected.food.brandName?.takeIf(String::isNotBlank)?.let { Text(it, fontSize = 15.sp, color = JanuaryColors.Muted) }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Icon(Icons.Outlined.DeleteOutline, "Remove ${selected.food.name}", tint = JanuaryColors.Error) }
            }
            HorizontalDivider(color = JanuaryColors.Divider)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Serving", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { menuOpen = true }, contentPadding = PaddingValues(0.dp)) {
                        Text("${formatDemoNumber(selected.serving.quantity)} ${selected.serving.unit}", color = JanuaryColors.Green)
                        Icon(Icons.Outlined.UnfoldMore, null, Modifier.size(18.dp), tint = JanuaryColors.Green)
                    }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        selected.food.servings.forEach { serving ->
                            DropdownMenuItem(text = { Text("${formatDemoNumber(serving.quantity)} ${serving.unit}") }, onClick = { onServingChange(serving); menuOpen = false })
                        }
                    }
                }
            }
            HorizontalDivider(color = JanuaryColors.Divider)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Quantity", style = MaterialTheme.typography.titleMedium)
                    Text(formatDemoNumber(selected.quantity), fontSize = 15.sp, fontFamily = FontFamily.Monospace, color = JanuaryColors.Muted)
                }
                Row(Modifier.clip(RoundedCornerShape(9.dp)).background(JanuaryColors.Control), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onQuantityChange((selected.quantity - 0.25).coerceAtLeast(0.25)) }, enabled = selected.quantity > 0.25) { Icon(Icons.Outlined.Remove, "Decrease quantity") }
                    VerticalDivider(Modifier.height(20.dp), color = JanuaryColors.Border)
                    IconButton(onClick = { onQuantityChange((selected.quantity + 0.25).coerceAtMost(10_000.0)) }, enabled = selected.quantity < 10_000.0) { Icon(Icons.Outlined.Add, "Increase quantity") }
                }
            }
        }
    }
}

@Composable
internal fun FoodLogMacros(values: List<Pair<String, NutrientAmount?>>) {
    MacroGrid(values.map { (label, amount) -> MacroValue(label, amount?.value?.let(::formatMetricNumber) ?: "—", amount?.unit ?: if (label == "Calories") "cal" else "g") })
}
