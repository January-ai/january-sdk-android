package ai.january.partner.demo

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** One presentation and navigation policy for all demo sheets, including nested detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppModalSheet(
    title: String,
    onDismiss: () -> Unit,
    expanded: Boolean = true,
    showNavigationBar: Boolean = true,
    testTag: String? = null,
    closeTestTag: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // The sheet is its own window, so it needs its own testTagsAsResourceId for UiAutomator/Maestro.
        modifier = Modifier.semantics { testTagsAsResourceId = true }.then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = expanded),
        dragHandle = null,
        containerColor = JanuaryColors.Paper,
        properties = ModalBottomSheetProperties(isAppearanceLightStatusBars = true, isAppearanceLightNavigationBars = true),
    ) {
        if (showNavigationBar) {
            AppNavigationBar(
                title = title,
                leading = { AppNavigationButton(AppNavigationButtonKind.Close, title = "Close $title", testTag = closeTestTag, onClick = onDismiss) },
                trailing = trailing,
            )
        }
        content()
    }
}
