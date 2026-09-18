package ai.january.partner.demo

import ai.january.partner.JanuaryClientToken
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.forJanuaryDevelopment
import android.app.Activity

/**
 * Lets a debug build be launched against the local fixture server that the UI
 * suites use, for example
 * `adb shell am start --es januaryFixtureOrigin http://10.0.2.2:18766 ai.january.partner.demo/.MainActivity`
 * or a Maestro `launchApp` with that argument. The client then uses a stub
 * token and the deterministic fixture data, never production. The release
 * source set replaces this with a function that always returns null.
 */
internal fun fixtureStateFromIntent(activity: Activity): DemoState? {
    val origin = activity.intent?.getStringExtra(FIXTURE_ORIGIN_EXTRA)?.trim().orEmpty()
    if (origin.isEmpty()) return null
    val client = JanuaryPartnerClient.forJanuaryDevelopment(
        provider = { JanuaryClientToken("fixture-client-token", 3600) },
        apiBaseUrl = origin,
    )
    return DemoState(activity.applicationContext, client).apply {
        endUserId = "parity-user"
        timezone = "America/New_York"
    }
}

private const val FIXTURE_ORIGIN_EXTRA = "januaryFixtureOrigin"
