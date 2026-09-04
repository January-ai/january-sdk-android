package ai.january.partner.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DemoSetupScreen() {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding).padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(Modifier.size(48.dp), shape = CircleShape, color = JanuaryColors.TargetBand) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint = JanuaryColors.Green) } }
        Text("Welcome to January", style = MaterialTheme.typography.displaySmall)
        Text("Start the local token server, then point this demo at it. Your January API key stays on the server.", color = JanuaryColors.Body)
        DemoCard {
            SetupOption("1", "Start the token server", "Local", "In january-server-sdk-node, run npm run demo:token-server.")
            HorizontalDivider(Modifier.padding(vertical = 18.dp), color = JanuaryColors.Divider)
            SetupOption("2", "Connect this app", "Client token", "Set january.partnerTokenUrl and january.partnerSessionToken in local.properties.")
        }
        DemoCard {
            SectionLabel("Where to configure")
            Text("local.properties", style = MaterialTheme.typography.titleMedium)
            Text("Copy the two values from the root README, then build again.", fontSize = 15.sp, color = JanuaryColors.Body)
        }
    }
}

@Composable
private fun SetupOption(number: String, title: String, badge: String, message: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(Modifier.size(28.dp), shape = CircleShape, color = JanuaryColors.TargetBand) { Box(contentAlignment = Alignment.Center) { Text(number, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = JanuaryColors.Green) } }
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(shape = CircleShape, color = JanuaryColors.TargetBand) { Text(badge.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = JanuaryColors.Green) }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message, fontSize = 15.sp, lineHeight = 20.sp, color = JanuaryColors.Body)
        }
    }
}
