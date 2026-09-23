package ai.january.partner.demo

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/** Keyboard options for a single-line field whose Done key finishes editing. */
internal fun doneKeyboard(keyboardType: KeyboardType = KeyboardType.Text): KeyboardOptions =
    KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done)

/**
 * Done (or Enter on a hardware keyboard) lets go of the field, which closes the keyboard, so the
 * controls it covered, such as a Log button or a chart, are in reach again.
 */
@Composable
internal fun rememberDoneActions(): KeyboardActions {
    val focusManager = LocalFocusManager.current
    return remember(focusManager) { KeyboardActions(onDone = { focusManager.clearFocus() }) }
}
