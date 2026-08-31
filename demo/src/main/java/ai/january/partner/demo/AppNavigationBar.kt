package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

internal enum class AppNavigationTitleStyle { Centered, Leading }

/** Mirrors iOS AppNavigationBar. The host owns system insets; this bar never adds them. */
@Composable
internal fun AppNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    style: AppNavigationTitleStyle = AppNavigationTitleStyle.Centered,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleExpansion: Float = 1f,
) {
    Column(modifier.fillMaxWidth().background(JanuaryColors.Paper).testTag("app-navigation-bar")) {
        val largeTitle = style == AppNavigationTitleStyle.Leading && titleExpansion > 0f
        CompactNavigationRow(if (largeTitle) "" else title, leading, trailing)
        if (largeTitle) {
            Box(Modifier.fillMaxWidth().height(56.dp * titleExpansion).clipToBounds()) {
                Text(
                    title, Modifier.padding(horizontal = DemoScreenPadding).semantics { heading() }.testTag("app-navigation-title"),
                    fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, color = JanuaryColors.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CompactNavigationRow(title: String, leading: (@Composable () -> Unit)?, trailing: (@Composable () -> Unit)?) {
    Layout(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = DemoScreenPadding),
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) { leading?.invoke() }
            Text(
                title, if (title.isEmpty()) Modifier else Modifier.semantics { heading() }.testTag("app-navigation-title"),
                style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) { trailing?.invoke() }
        },
    ) { measurables, constraints ->
        val actionConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val start = measurables[0].measure(actionConstraints)
        val end = measurables[2].measure(actionConstraints)
        // Reserve the larger action width on BOTH sides so asymmetric actions cannot shift the title.
        val side = max(start.width, end.width)
        val gap = if (side > 0) 8.dp.roundToPx() else 0
        val label = measurables[1].measure(actionConstraints.copy(maxWidth = (constraints.maxWidth - 2 * (side + gap)).coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            start.placeRelative(0, (constraints.maxHeight - start.height) / 2)
            label.placeRelative((constraints.maxWidth - label.width) / 2, (constraints.maxHeight - label.height) / 2)
            end.placeRelative(constraints.maxWidth - end.width, (constraints.maxHeight - end.height) / 2)
        }
    }
}

/** Compact navigation stays fixed; root titles collapse as their content scrolls, matching iOS. */
@Composable
internal fun AppScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    style: AppNavigationTitleStyle = AppNavigationTitleStyle.Centered,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val collapseRange = with(LocalDensity.current) { 56.dp.toPx() }
    var collapsed by remember(title) { mutableFloatStateOf(0f) }
    val connection = remember(title, style, collapseRange) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (style != AppNavigationTitleStyle.Leading || available.y >= 0f) return Offset.Zero
                val old = collapsed
                collapsed = (collapsed - available.y).coerceIn(0f, collapseRange)
                return Offset(0f, old - collapsed)
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (style != AppNavigationTitleStyle.Leading || available.y <= 0f) return Offset.Zero
                val old = collapsed
                collapsed = (collapsed - available.y).coerceIn(0f, collapseRange)
                return Offset(0f, old - collapsed)
            }
        }
    }
    Column(modifier.fillMaxSize().background(JanuaryColors.Paper).nestedScroll(connection)) {
        AppNavigationBar(title = title, style = style, leading = leading, trailing = trailing, titleExpansion = 1f - collapsed / collapseRange)
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
    }
}
