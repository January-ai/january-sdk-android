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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AuthenticationRequiredCard() {
    DemoCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Key, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text("Connect the token relay", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                "Set january.partnerTokenUrl in local.properties, then rebuild. For a LAN or hosted relay, also set january.partnerSessionToken. For the Debug-only API-key shortcut, see the README.",
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

/**
 * Whether a failed create may have been recorded anyway. Creates are not idempotent, and a timeout,
 * a dropped connection, a server error or an unreadable answer can come after the server saved the
 * entry. Only a request the API refused, or one that was never sent, is safe to send again as it is.
 */
internal fun mayHaveBeenSaved(failure: Throwable): Boolean = when ((failure as? ai.january.partner.JanuaryException)?.category) {
    ai.january.partner.ErrorCategory.VALIDATION,
    ai.january.partner.ErrorCategory.AUTHENTICATION,
    ai.january.partner.ErrorCategory.AUTHORIZATION,
    ai.january.partner.ErrorCategory.NOT_FOUND,
    ai.january.partner.ErrorCategory.RATE_LIMITED,
    -> false
    ai.january.partner.ErrorCategory.TIMEOUT,
    ai.january.partner.ErrorCategory.TRANSPORT,
    ai.january.partner.ErrorCategory.SERVER,
    ai.january.partner.ErrorCategory.DECODING,
    null,
    -> true
}

/**
 * [testTag] tags the card; its technical-details disclosure becomes "$testTag-details" and the
 * disclosed body "$testTag-details-body". [retryTestTag] tags the retry button. [note] follows the
 * error's message, for what the retry will do.
 */
@Composable
fun ErrorCard(error: Throwable, retry: (() -> Unit)? = null, testTag: String? = null, retryTestTag: String? = null, note: String? = null) {
    DemoCard(testTag?.let { Modifier.testTag(it) } ?: Modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = JanuaryColors.Rust)
                Text(errorTitle(error), style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Rust)
            }
            Text(error.localizedMessage ?: "The request could not be completed.", color = JanuaryColors.Body)
            note?.let { Text(it, color = JanuaryColors.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
            (error as? ai.january.partner.JanuaryException)?.let { failure ->
                if (failure.httpStatus != null || failure.requestId != null) {
                    DetailDisclosure(
                        modifier = testTag?.let { Modifier.testTag("$it-details") } ?: Modifier,
                        bodyTestTag = testTag?.let { "$it-details-body" },
                    ) {
                        NutritionList(listOfNotNull(
                            failure.httpStatus?.let { NutritionValue("HTTP status", it.toString()) },
                            failure.code?.let { NutritionValue("Error code", it) },
                            failure.requestId?.let { NutritionValue("Request ID", it) },
                        ))
                    }
                }
            }
            retry?.let {
                androidx.compose.material3.TextButton(onClick = it, modifier = retryTestTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier) {
                    Text("Try again", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppFeedbackPreview() {
    JanuaryDemoTheme { ErrorCard("Something went wrong.") }
}
