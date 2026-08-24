package ai.january.partner.demo

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ai.january.partner.foodlogs.FoodLog
import ai.january.partner.foodlogs.FoodLogUserContext
import ai.january.partner.foodlogs.ListFoodLogsRequest
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

@Composable
fun FoodLogsScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val coroutineScope = rememberCoroutineScope()
    var from by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var logs by remember { mutableStateOf<List<FoodLog>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        val user = state.partnerUserId ?: return
        val sdk = client ?: return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                sdk.foodLogs.list(
                    ListFoodLogsRequest(from.toString(), to.toString(), FoodLogUserContext(user, ZoneId.systemDefault().id)),
                ).items
            }.onSuccess { logs = it }
                .onFailure { error = it.message ?: "Food logs could not be loaded." }
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        DemoTopBar("Food logs", settingsAction)
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.partnerUserId == null) {
                    androidx.compose.material3.Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = JanuaryColors.GoldContainer),
                    ) {
                        Column(androidx.compose.ui.Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("One thing first")
                            Text("Logs are stored per person. Add the partner's stable user ID before loading or creating logs.")
                            Button(onClick = settingsAction, modifier = Modifier.fillMaxWidth()) { Text("Open settings") }
                        }
                    }
                }
                SectionLabel("Date range")
                DemoCard {
                    DateRow("From", from.toString())
                    androidx.compose.material3.HorizontalDivider()
                    DateRow("To", to.toString())
                }
                Button(
                    onClick = ::load,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.partnerUserId != null && client != null && !loading,
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    else Text("Load logs")
                }
                if (client == null) ApiKeyRequiredCard()
                error?.let { ErrorCard(it, ::load) }
                if (!loading && error == null && logs.isEmpty() && state.partnerUserId != null) {
                    DemoCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("No food logs", style = MaterialTheme.typography.titleLarge)
                            Text("There are no logs in this date range.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                logs.forEach { log ->
                    DemoCard {
                        Text(log.name?.takeIf(String::isNotBlank) ?: "Meal", style = MaterialTheme.typography.titleMedium)
                        Text(log.timestampUtc, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(log.foods.joinToString { it.name }, maxLines = 2)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun DateRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
