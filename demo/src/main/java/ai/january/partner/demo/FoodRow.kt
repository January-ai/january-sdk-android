package ai.january.partner.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FoodRow(
    name: String,
    subtitle: String?,
    meta: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NetworkImage(imageUrl, null, Modifier.size(58.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Ink)
            subtitle?.takeIf(String::isNotBlank)?.let { Text(it, color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall) }
            meta?.takeIf(String::isNotBlank)?.let { Text(it, color = JanuaryColors.Muted, style = MaterialTheme.typography.labelSmall) }
        }
        if (loading) {
            LoadingSpinner(Modifier.size(22.dp), JanuaryColors.Subdued)
        } else {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = JanuaryColors.Subdued)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoodRowPreview() {
    JanuaryDemoTheme { FoodRow("Greek yogurt", "January Foods", "120 cal · 1 cup", null, {}) }
}
