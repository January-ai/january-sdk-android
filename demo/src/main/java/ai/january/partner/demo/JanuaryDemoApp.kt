package ai.january.partner.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import java.time.ZoneId

private enum class Destination(val label: String, val icon: ImageVector) {
    SEARCH("Search", Icons.Filled.Search),
    SCAN("Scan", Icons.Filled.CenterFocusWeak),
    FOOD_LOGS("Food Logs", Icons.Filled.ListAlt),
    GLUCOSE("Glucose", Icons.Filled.ShowChart),
}

@Composable
fun JanuaryDemoApp() {
    val context = LocalContext.current
    val state = remember { DemoState(context.applicationContext) }
    var destination by rememberSaveable { mutableStateOf(Destination.SEARCH) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
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
                NavigationBar(
                    modifier = Modifier.height(64.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                ) {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == destination,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp)) },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JanuaryColors.Ink,
                                selectedTextColor = JanuaryColors.Ink,
                                indicatorColor = JanuaryColors.ControlStrong,
                                unselectedIconColor = JanuaryColors.Subdued,
                                unselectedTextColor = JanuaryColors.Muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (destination) {
            Destination.SEARCH -> SearchScreen(state, { showSettings = true }, Modifier.padding(innerPadding).windowInsetsPadding(WindowInsets.statusBars))
            Destination.SCAN -> ScanScreen(state, { showSettings = true }, Modifier.padding(innerPadding).windowInsetsPadding(WindowInsets.statusBars))
            Destination.FOOD_LOGS -> FoodLogsScreen(state, { showSettings = true }, Modifier.padding(innerPadding).windowInsetsPadding(WindowInsets.statusBars))
            Destination.GLUCOSE -> GlucoseScreen(state, { showSettings = true }, Modifier.padding(innerPadding).windowInsetsPadding(WindowInsets.statusBars))
        }
    }

    if (showSettings) {
        SettingsSheet(state = state, onDismiss = { showSettings = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoTopBar(
    title: String,
    settingsAction: () -> Unit,
    additionalAction: (() -> Unit)? = null,
    additionalActionDescription: String = "Add",
) {
    LargeTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.displaySmall) },
        actions = {
            additionalAction?.let { action ->
                IconButton(onClick = action) {
                    Icon(Icons.Outlined.Add, contentDescription = additionalActionDescription)
                }
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
            Text("Settings", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(8.dp))
            SectionLabel("Connection")
            DemoCard {
                Text("Authentication", style = MaterialTheme.typography.titleMedium)
                Text(if (state.client == null) "API key missing" else "API key configured", color = if (state.client == null) JanuaryColors.Rust else JanuaryColors.Green)
            }
            Spacer(Modifier.padding(4.dp))
            SectionLabel("Request context")
            DemoCard {
                OutlinedTextField(
                    value = state.endUserId,
                    onValueChange = { state.endUserId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End user ID") },
                    supportingText = { Text("Required for food logs; included on other requests when present.") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.timezone,
                    onValueChange = { state.timezone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Timezone") },
                    supportingText = { Text("IANA timezone sent with user-scoped requests.") },
                    singleLine = true,
                )
            }
            Spacer(Modifier.padding(4.dp))
            SectionLabel("About")
            DemoCard {
                Row(Modifier.fillMaxWidth()) { Text("App version"); Spacer(Modifier.weight(1f)); Text(BuildConfig.VERSION_NAME) }
                HorizontalDivider(color = JanuaryColors.Divider)
                Row(Modifier.fillMaxWidth()) { Text("Environment"); Spacer(Modifier.weight(1f)); Text("Development") }
            }
            Spacer(Modifier.padding(8.dp))
            DemoPrimaryButton("Done", onDismiss, Modifier.fillMaxWidth())
            Spacer(Modifier.padding(12.dp))
        }
    }
}
