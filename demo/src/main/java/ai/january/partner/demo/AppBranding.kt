package ai.january.partner.demo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class AppBranding(
    val name: String,
    val monogram: String,
    val demoLabel: String,
    val foodDatabaseLabel: String,
)

val JanuaryAppBranding = AppBranding(
    name = "January",
    monogram = "J",
    demoLabel = "Partner Demo",
    foodDatabaseLabel = "January food database",
)

val LocalAppBranding = staticCompositionLocalOf { JanuaryAppBranding }
