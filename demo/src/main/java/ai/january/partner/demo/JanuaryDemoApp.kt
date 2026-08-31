package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JanuaryDemoApp(providedState: DemoState? = null) {
    val context = LocalContext.current
    val state = providedState ?: remember { DemoState(context.applicationContext) }
    var destination by rememberSaveable { mutableStateOf(AppDestination.SEARCH) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (state.client == null) { DemoSetupScreen(); return }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)) {
                if (state.isDevelopmentAuthentication) {
                    Row(Modifier.fillMaxWidth().background(JanuaryColors.GoldContainer).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Warning, null, Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color(0xFF6E5613))
                        Text("Local testing mode — do not distribute this build with a development API key.", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color(0xFF6E5613))
                    }
                }
            }
        },
        bottomBar = { AppTabBar(selected = destination, onSelect = { destination = it }) },
    ) { innerPadding ->
        when (destination) {
            AppDestination.SEARCH -> SearchScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            AppDestination.SCAN -> ScanScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            AppDestination.FOOD_LOGS -> FoodLogsScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            AppDestination.GLUCOSE -> GlucoseScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
        }
    }

    if (showSettings) {
        SettingsSheet(state = state, onDismiss = { showSettings = false })
    }
}
