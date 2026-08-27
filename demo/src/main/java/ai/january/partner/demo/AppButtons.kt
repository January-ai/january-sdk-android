package ai.january.partner.demo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            disabledContainerColor = if (loading) JanuaryColors.Ink else JanuaryColors.Control,
            disabledContentColor = if (loading) JanuaryColors.Paper else JanuaryColors.Subdued,
        ),
    ) {
        if (loading) LoadingSpinner(color = JanuaryColors.Paper) else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), maxLines = 1)
        }
    }
}

@Composable
fun LoadingSpinner(modifier: Modifier = Modifier, color: Color = JanuaryColors.Ink) {
    CircularProgressIndicator(modifier.size(22.dp), color = color, trackColor = Color.Transparent, strokeWidth = 2.5.dp)
}

@Preview(showBackground = true)
@Composable
private fun AppButtonsPreview() {
    JanuaryDemoTheme {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoPrimaryButton("Primary", {}, Modifier.weight(1f))
            DemoSecondaryButton("Secondary", {}, Modifier.weight(1f))
        }
    }
}
