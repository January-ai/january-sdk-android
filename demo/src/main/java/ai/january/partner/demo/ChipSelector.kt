package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class ChipOption<T>(val value: T, val label: String)

@Composable
fun <T> ChipSelector(
    options: List<ChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option.value == selected,
                onClick = { onSelect(option.value) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                label = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(option.label, maxLines = 1, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = JanuaryColors.Body,
                    selectedContainerColor = JanuaryColors.Ink,
                    selectedLabelColor = JanuaryColors.Paper,
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChipSelectorPreview() {
    JanuaryDemoTheme {
        ChipSelector(
            options = listOf("All", "General", "Branded", "Recipe").map { ChipOption(it, it) },
            selected = "All",
            onSelect = {},
        )
    }
}
