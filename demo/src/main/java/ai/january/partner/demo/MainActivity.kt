package ai.january.partner.demo

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The demo is always light, even when the device uses dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Debug builds can be launched against the local fixture server used by
        // the UI suites (see FixtureLaunch in the debug source set); release
        // builds always get null here.
        val fixtureState = fixtureStateFromIntent(this)
        setContent {
            JanuaryDemoTheme {
                JanuaryDemoApp(fixtureState)
            }
        }
    }
}
