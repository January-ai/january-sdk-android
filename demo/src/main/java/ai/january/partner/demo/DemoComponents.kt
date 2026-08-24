package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DemoScreenPadding = 16.dp

@Composable
fun DemoScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DemoScreenPadding),
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, JanuaryColors.Ink.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    }
}

@Composable
fun DemoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = JanuaryColors.Ink,
            contentColor = JanuaryColors.Paper,
            disabledContainerColor = JanuaryColors.Control,
            disabledContentColor = JanuaryColors.Subdued,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = JanuaryColors.Paper,
                trackColor = Color.Transparent,
                strokeWidth = 2.5.dp,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon?.invoke()
                Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
fun DemoSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = JanuaryColors.Control,
            contentColor = JanuaryColors.Ink,
            disabledContainerColor = JanuaryColors.Control,
            disabledContentColor = JanuaryColors.Subdued,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun DemoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, JanuaryColors.Border),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = JanuaryColors.Surface,
            contentColor = JanuaryColors.Ink,
            disabledContentColor = JanuaryColors.Subdued,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), maxLines = 1)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.15.sp,
    )
}

@Composable
fun ApiKeyRequiredCard() {
    DemoCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text("Add your API key", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "Add january.apiKey=YOUR_KEY to local.properties, then rebuild the demo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorCard(message: String, retry: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            if (retry != null) DemoPrimaryButton("Try again", retry, Modifier.fillMaxWidth())
        }
    }
}

fun LazyListScope.demoSpacer(height: Int = 8) {
    item { androidx.compose.foundation.layout.Spacer(Modifier.padding(vertical = height.dp / 2)) }
}
