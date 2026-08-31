package ai.january.partner.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object JanuaryColors {
    val Canvas = Color(0xFFEFEBE2)
    val Paper = Color(0xFFFAF8F2)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1D1A14)
    val Body = Color(0xFF3E3A2E)
    val Muted = Color(0xFF55503F)
    val Subdued = Color(0xFF8F887A)
    val Border = Color(0xFFE0DACB)
    val Divider = Color(0xFFF1EDE2)
    val Control = Color(0xFFF3F0E7)
    val ControlStrong = Color(0xFFEBE5D8)
    val Green = Color(0xFF54724F)
    val Gold = Color(0xFFF4C63F)
    val GoldContainer = Color(0xFFFBF0CB)
    val Rust = Color(0xFFA85F3D)
    val TargetBand = Color(0xFFF0F3EA)
    val Error = Color(0xFF9D3B2F)
}

private val LightColors = lightColorScheme(
    primary = JanuaryColors.Ink,
    onPrimary = JanuaryColors.Paper,
    primaryContainer = JanuaryColors.Control,
    onPrimaryContainer = JanuaryColors.Ink,
    secondary = JanuaryColors.Green,
    onSecondary = Color.White,
    secondaryContainer = JanuaryColors.TargetBand,
    onSecondaryContainer = Color(0xFF243620),
    tertiary = JanuaryColors.Rust,
    background = JanuaryColors.Paper,
    onBackground = JanuaryColors.Ink,
    surface = JanuaryColors.Surface,
    onSurface = JanuaryColors.Ink,
    surfaceContainer = JanuaryColors.Paper,
    surfaceContainerLow = JanuaryColors.Paper,
    surfaceContainerHigh = JanuaryColors.Control,
    surfaceContainerHighest = JanuaryColors.Control,
    surfaceVariant = JanuaryColors.Control,
    onSurfaceVariant = JanuaryColors.Body,
    outline = JanuaryColors.Border,
    outlineVariant = JanuaryColors.Border,
    error = JanuaryColors.Error,
)

@Composable
fun JanuaryDemoTheme(
    branding: AppBranding = JanuaryAppBranding,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppBranding provides branding) {
        MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 30.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
            bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
            bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp),
            labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
            content = content,
        )
    }
}
