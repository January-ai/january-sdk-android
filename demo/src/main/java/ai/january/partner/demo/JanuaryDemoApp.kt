package ai.january.partner.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.InsertChartOutlined
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private enum class Destination(val label: String, val icon: ImageVector) {
    SEARCH("Search", Icons.Filled.Search),
    SCAN("Scan", Icons.Filled.CameraAlt),
    FOOD_LOGS("Food logs", Icons.Filled.RestaurantMenu),
    GLUCOSE("Glucose", Icons.Filled.InsertChartOutlined),
}

@Composable
fun JanuaryDemoApp() {
    val state = remember { DemoState() }
    var destination by rememberSaveable { mutableStateOf(Destination.SEARCH) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == destination,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (destination) {
            Destination.SEARCH -> SearchScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            Destination.SCAN -> ScanScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            Destination.FOOD_LOGS -> FoodLogsScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
            Destination.GLUCOSE -> GlucoseScreen(state, { showSettings = true }, Modifier.padding(innerPadding))
        }
    }

    if (showSettings) {
        SettingsSheet(state = state, onDismiss = { showSettings = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoTopBar(title: String, settingsAction: () -> Unit) {
    LargeTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.displaySmall) },
        actions = {
            IconButton(onClick = settingsAction) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(state: DemoState, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Demo settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.padding(8.dp))
            OutlinedTextField(
                value = state.endUserId,
                onValueChange = { state.endUserId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("End user ID") },
                supportingText = { Text("Required for food logs; included on other requests when present.") },
                singleLine = true,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            Spacer(Modifier.padding(12.dp))
        }
    }
}
