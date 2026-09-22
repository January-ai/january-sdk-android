package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** [tag] is the Maestro id; it stays stable when a label is renamed. */
internal enum class AppDestination(val label: String, val icon: ImageVector, val tag: String) {
    SEARCH("Search", Icons.Filled.Search, "tab-search"),
    SCAN("Scan", Icons.Filled.CenterFocusWeak, "tab-scan"),
    TRACKING("Tracking", Icons.Filled.Insights, "tab-tracking"),
    FOOD_LOGS("Logs", Icons.Filled.EventNote, "tab-food-logs"),
    GLUCOSE("Glucose", Icons.Filled.ShowChart, "tab-glucose"),
}


@Composable
internal fun AppTabBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) {
    Surface(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .shadow(18.dp, RoundedCornerShape(38.dp))
            .clip(RoundedCornerShape(38.dp)),
        shape = RoundedCornerShape(38.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(4.dp).selectableGroup()) {
            AppDestination.entries.forEach { item ->
                Column(
                    Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(50))
                        .background(if (item == selected) androidx.compose.ui.graphics.Color(0xFFEBE9E6) else androidx.compose.ui.graphics.Color.Transparent)
                        .selectable(selected = item == selected, role = Role.Tab, onClick = { onSelect(item) })
                        .testTag(item.tag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(25.dp), tint = JanuaryColors.Ink)
                    Text(item.label, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = JanuaryColors.Ink)
                }
            }
        }
    }
}
