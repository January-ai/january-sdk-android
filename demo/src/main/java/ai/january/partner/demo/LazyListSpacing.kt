package ai.january.partner.demo

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

fun LazyListScope.demoSpacer(height: Int = 8) {
    item { Spacer(Modifier.padding(vertical = height.dp / 2)) }
}
