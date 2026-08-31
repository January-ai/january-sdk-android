package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DemoScreenPadding = 16.dp

@Composable
fun DemoScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = DemoScreenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp)) { content() }
    }
}

@Composable
fun DemoCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = JanuaryColors.Ink.copy(alpha = 0.08f), spotColor = JanuaryColors.Ink.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, JanuaryColors.Ink.copy(alpha = 0.06f)),
    ) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(horizontal = 6.dp),
        style = MaterialTheme.typography.labelMedium,
        color = JanuaryColors.Muted,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.15.sp,
    )
}

@Preview(showBackground = true)
@Composable
private fun ScreenShellPreview() {
    JanuaryDemoTheme { DemoCard { SectionLabel("Preview"); Text("A reusable January surface") } }
}
