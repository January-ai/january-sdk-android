package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> SegmentedControl(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color(0xFFEBE9E6)).padding(2.dp).selectableGroup()) {
        options.forEach { option ->
            Box(
                Modifier.weight(1f).heightIn(min = 28.dp).clip(RoundedCornerShape(50))
                    .background(if (option == selected) Color.White else Color.Transparent)
                    .selectable(selected = option == selected, role = Role.Tab, onClick = { onSelect(option) })
                    .padding(horizontal = 3.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label(option), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1) }
        }
    }
}
