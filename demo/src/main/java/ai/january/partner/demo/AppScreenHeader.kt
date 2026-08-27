package ai.january.partner.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppScreenHeader(
    title: String,
    onSettings: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        action?.invoke()
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
    }
}

@Composable
internal fun AppModalHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        AppNavigationIconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close $title")
        }
        Text(
            title,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 64.dp),
            style = MaterialTheme.typography.titleMedium,
            color = JanuaryColors.Ink,
            maxLines = 1,
        )
        action?.let { trailing -> Box(Modifier.align(Alignment.CenterEnd)) { trailing() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppNavigationBar(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            AppNavigationIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back from $title")
            }
        },
        actions = { action?.invoke() },
    )
}

@Composable
private fun AppNavigationIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = JanuaryColors.Control,
    ) {
        IconButton(onClick = onClick) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppScreenHeaderPreview() {
    JanuaryDemoTheme { AppScreenHeader("Search", {}) }
}
