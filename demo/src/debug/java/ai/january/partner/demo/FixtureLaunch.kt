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
 *
 * With `januaryFixtureClientTokens` set to `true` as well, the demo keeps its
 * own per-user client-token path instead of the stub: it asks the fixture
 * server's token route for a token for each end user, as it would a token
 * relay, so a suite can check which user each token was minted for.
 *
 * `januaryFixtureTimezone` sets the end user's timezone (America/New_York
 * otherwise), so a suite can put the user's calendar day away from the
 * device's.
 */
internal fun fixtureStateFromIntent(activity: Activity): DemoState? {
    val origin = activity.intent?.getStringExtra(FIXTURE_ORIGIN_EXTRA)?.trim().orEmpty()
    if (origin.isEmpty()) return null
    val state = if (activity.intent?.getStringExtra(FIXTURE_CLIENT_TOKENS_EXTRA) == "true") {
        DemoState(
            activity.applicationContext,
            tokenUrlOverride = "${origin.trimEnd('/')}/api/january/client-token",
            clientFactory = { provider -> JanuaryPartnerClient.forJanuaryDevelopment(provider = provider, apiBaseUrl = origin) },
        )
    } else {
        val client = JanuaryPartnerClient.forJanuaryDevelopment(
            provider = { JanuaryClientToken("fixture-client-token", 3600) },
            apiBaseUrl = origin,
        )
        DemoState(activity.applicationContext, client)
    }
    val timezone = activity.intent?.getStringExtra(FIXTURE_TIMEZONE_EXTRA)?.trim().orEmpty().ifEmpty { "America/New_York" }
    return state.apply {
        endUserId = "parity-user"
        this.timezone = timezone
    }
}

private const val FIXTURE_ORIGIN_EXTRA = "januaryFixtureOrigin"
private const val FIXTURE_CLIENT_TOKENS_EXTRA = "januaryFixtureClientTokens"
private const val FIXTURE_TIMEZONE_EXTRA = "januaryFixtureTimezone"
