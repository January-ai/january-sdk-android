package ai.january.partner.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(state: DemoState, onDismiss: () -> Unit) {
    var zoneMenu by remember { mutableStateOf(false) }
    val zones = remember { ZoneId.getAvailableZoneIds().sorted() }
    AppModalSheet(title = "Settings", onDismiss = onDismiss, expanded = false) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding).padding(top = 28.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SectionLabel("Connection")
            DemoCard {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = JanuaryColors.Green)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("January SDK", style = MaterialTheme.typography.titleMedium)
                        Text(state.authenticationDescription, fontSize = 15.sp, lineHeight = 20.sp, color = JanuaryColors.Muted)
                    }
                    Surface(shape = CircleShape, color = JanuaryColors.TargetBand) {
                        Text("Connected", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JanuaryColors.Green)
                    }
                }
            }
            SectionLabel("Request context")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("End user ID", style = MaterialTheme.typography.titleMedium)
                DemoInput(state.endUserId, { state.endUserId = it }, "Partner user identifier")
                Text("Food Logs requires a stable ID. Other requests include it when available.", style = MaterialTheme.typography.bodySmall, color = JanuaryColors.Muted)
            }
            DemoCard {
                Box {
                    Row(Modifier.fillMaxWidth().clickable { zoneMenu = true }, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Timezone", style = MaterialTheme.typography.titleMedium)
                            Text(state.timezone.replace('_', ' '), fontSize = 15.sp, color = JanuaryColors.Muted)
                        }
                        Icon(Icons.Outlined.UnfoldMore, "Choose timezone", tint = JanuaryColors.Green)
                    }
                    DropdownMenu(zoneMenu, { zoneMenu = false }) {
                        zones.forEach { zone -> DropdownMenuItem(text = { Text(zone.replace('_', ' ')) }, onClick = { state.timezone = zone; zoneMenu = false }) }
                    }
                }
            }
            SectionLabel("About")
            DemoCard {
                NutritionList(listOf(NutritionValue("App version", BuildConfig.VERSION_NAME), NutritionValue("January API", "Production")))
            }
        }
    }
}
