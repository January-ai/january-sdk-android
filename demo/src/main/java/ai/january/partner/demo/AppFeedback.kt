package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyRequiredCard() {
    DemoCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
fun ErrorCard(message: String, retry: (() -> Unit)? = null) = ErrorCard(IllegalStateException(message), retry)

internal fun errorTitle(error: Throwable): String = when ((error as? ai.january.partner.JanuaryException)?.category) {
    ai.january.partner.ErrorCategory.AUTHENTICATION, ai.january.partner.ErrorCategory.AUTHORIZATION -> "Couldn’t use the configured credentials"
    ai.january.partner.ErrorCategory.VALIDATION -> "Check the information you entered"
    ai.january.partner.ErrorCategory.NOT_FOUND -> "No matching result was found"
    ai.january.partner.ErrorCategory.RATE_LIMITED -> "Too many requests"
    ai.january.partner.ErrorCategory.TIMEOUT -> "The request took too long"
    ai.january.partner.ErrorCategory.TRANSPORT -> "Check your connection"
    ai.january.partner.ErrorCategory.SERVER, ai.january.partner.ErrorCategory.DECODING -> "January couldn’t complete the request"
    null -> "Couldn’t complete that request"
}

@Composable
fun ErrorCard(error: Throwable, retry: (() -> Unit)? = null) {
    DemoCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = JanuaryColors.Rust)
                Text(errorTitle(error), style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Rust)
            }
            Text(error.localizedMessage ?: "The request could not be completed.", color = JanuaryColors.Body)
            (error as? ai.january.partner.JanuaryException)?.let { failure ->
                if (failure.httpStatus != null || failure.requestId != null) {
                    DetailDisclosure {
                        NutritionList(listOfNotNull(
                            failure.httpStatus?.let { NutritionValue("HTTP status", it.toString()) },
                            failure.code?.let { NutritionValue("Error code", it) },
                            failure.requestId?.let { NutritionValue("Request ID", it) },
                        ))
                    }
                }
            }
            retry?.let { androidx.compose.material3.TextButton(onClick = it) { Text("Try again", style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFeedbackPreview() {
    JanuaryDemoTheme { ErrorCard("Something went wrong.") }
}
