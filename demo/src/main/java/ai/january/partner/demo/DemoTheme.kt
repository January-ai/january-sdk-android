package ai.january.partner.demo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object JanuaryColors {
    val Paper = Color(0xFFFAF8F2)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1D1A14)
    val Body = Color(0xFF3E3A2E)
    val Muted = Color(0xFF625D4E)
    val Border = Color(0xFFE0DACB)
    val Control = Color(0xFFF3F0E7)
    val Green = Color(0xFF54724F)
    val Gold = Color(0xFFF4C63F)
    val GoldContainer = Color(0xFFFBF0CB)
    val Rust = Color(0xFFA85F3D)
    val Error = Color(0xFF9D3B2F)
}

private val LightColors = lightColorScheme(
    primary = JanuaryColors.Ink,
    onPrimary = JanuaryColors.Paper,
    primaryContainer = JanuaryColors.Control,
    onPrimaryContainer = JanuaryColors.Ink,
    secondary = JanuaryColors.Green,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2EBDD),
    onSecondaryContainer = Color(0xFF243620),
    tertiary = JanuaryColors.Rust,
    background = JanuaryColors.Paper,
    onBackground = JanuaryColors.Ink,
    surface = JanuaryColors.Surface,
    onSurface = JanuaryColors.Ink,
    surfaceVariant = JanuaryColors.Control,
    onSurfaceVariant = JanuaryColors.Body,
    outline = JanuaryColors.Border,
    error = JanuaryColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2EBDD),
    onPrimary = Color(0xFF242018),
    primaryContainer = Color(0xFF3B362B),
    onPrimaryContainer = Color(0xFFF6F0E5),
    secondary = Color(0xFFB8D4B1),
    onSecondary = Color(0xFF243620),
    tertiary = Color(0xFFF1B095),
    background = Color(0xFF171510),
    onBackground = Color(0xFFF3EEE4),
    surface = Color(0xFF211E18),
    onSurface = Color(0xFFF3EEE4),
    surfaceVariant = Color(0xFF302C23),
    onSurfaceVariant = Color(0xFFD8D0C0),
    outline = Color(0xFF5E5749),
    error = Color(0xFFFFB4A8),
)

@Composable
fun JanuaryDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 30.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        ),
        content = content,
    )
}
