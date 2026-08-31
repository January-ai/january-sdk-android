package ai.january.partner.demo

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class AppNavigationButtonKind(val title: String) {
    Back("Back"), Close("Close"), Done("Done"), Add("Add"), Edit("Edit"), Settings("Settings")
}

/** The same semantic actions as iOS AppNavigationButton, with a 48dp Android touch target. */
@Composable
internal fun AppNavigationButton(
    kind: AppNavigationButtonKind,
    title: String = kind.title,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val icon = when (kind) {
        AppNavigationButtonKind.Back -> Icons.AutoMirrored.Outlined.KeyboardArrowLeft
        AppNavigationButtonKind.Close -> Icons.Outlined.Close
        AppNavigationButtonKind.Add -> Icons.Outlined.Add
        AppNavigationButtonKind.Settings -> Icons.Outlined.Settings
        else -> null
    }
    if (icon != null) {
        Surface(shape = CircleShape, color = JanuaryColors.Control, modifier = Modifier.size(48.dp)) {
            IconButton(onClick = onClick, enabled = enabled) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = JanuaryColors.Ink.copy(alpha = if (enabled) 1f else 0.38f))
            }
        }
    } else {
        TextButton(onClick = onClick, enabled = enabled) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6E5613).copy(alpha = if (enabled) 1f else 0.38f))
        }
    }
}
