package ai.january.partner.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun UserContextCard(
    endUserId: String,
    timezone: String,
    description: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DemoCard(modifier) {
        SectionLabel("Active user")
        Text(endUserId, style = MaterialTheme.typography.titleMedium)
        Text(timezone, color = JanuaryColors.Muted, fontFamily = FontFamily.Monospace)
        Text(description, color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onEdit) { Text("Edit in Settings") }
    }
}

@Preview(showBackground = true)
@Composable
private fun UserContextCardPreview() {
    JanuaryDemoTheme { UserContextCard("partner-user-123", "America/New_York", "The app owns and persists this identity.", {}) }
}
