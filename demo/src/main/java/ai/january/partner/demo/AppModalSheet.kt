package ai.january.partner.demo

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/** One presentation and navigation policy for all demo sheets, including nested detail screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppModalSheet(
    title: String,
    onDismiss: () -> Unit,
    expanded: Boolean = true,
    showNavigationBar: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = expanded),
        containerColor = JanuaryColors.Paper,
        properties = ModalBottomSheetProperties(isAppearanceLightStatusBars = true, isAppearanceLightNavigationBars = true),
    ) {
        if (showNavigationBar) {
            AppNavigationBar(
                title = title,
                leading = { AppNavigationButton(AppNavigationButtonKind.Close, title = "Close $title", onClick = onDismiss) },
                trailing = trailing,
            )
        }
        content()
    }
}
