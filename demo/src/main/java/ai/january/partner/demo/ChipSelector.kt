package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChipOption<T>(val value: T, val label: String)

@Composable
fun <T> ChipSelector(options: List<ChipOption<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option -> DemoChoiceChip(option.label, option.value == selected, { onSelect(option.value) }) }
            }
        }
    }
}

@Composable
internal fun DemoChoiceChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick, modifier = modifier.heightIn(min = 44.dp).semantics { selected = isSelected; role = Role.Checkbox },
        shape = RoundedCornerShape(18.dp), color = if (isSelected) JanuaryColors.Ink else JanuaryColors.Surface,
        border = if (isSelected) null else BorderStroke(1.5.dp, JanuaryColors.Border),
    ) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = if (isSelected) JanuaryColors.Paper else JanuaryColors.Ink)
        }
    }
}
