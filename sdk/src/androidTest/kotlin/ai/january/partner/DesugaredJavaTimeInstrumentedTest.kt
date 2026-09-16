package ai.january.partner

import ai.january.partner.foodlogs.FoodLogSummaryGrouping
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import android.os.Build
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Runs the SDK's java.time paths (token expiry, retry delays, date-range and timestamp
 * serialization, response decoding) on the device. Below API 26 these classes exist only
 * through core library desugaring, so this proves the desugared library is wired correctly.
 */
class DesugaredJavaTimeInstrumentedTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    fun tokenLifecycleSummaryAndGlucoseWorkOnThisApiLevel(): Unit = runBlocking {
        val now = Instant.parse("2026-09-16T12:00:00Z")
        var fetches = 0
        val client = JanuaryPartnerClient.testing(
            provider = { fetches += 1; JanuaryClientToken("fixture-client-token-$fetches", 3600) },
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
            refreshLeeway = Duration.ofSeconds(60),
            now = { now },
        )
        val user = client.forUser(PartnerUserId("api-level-${Build.VERSION.SDK_INT}"), "America/Chicago")

        enqueue(SUMMARY)
        val summary = user.foodLogs.getSummary("2026-09-14", "2026-09-14")
        assertEquals(FoodLogSummaryGrouping.DAY, summary.groupBy)
        assertEquals(1853.06, summary.buckets.single().nutrients.calories!!.value, 0.001)

        enqueue(PREDICTION)
        val prediction = user.glucose.predict(
            PredictGlucoseRequest(
                userProfile = GlucosePredictionProfile(35.0, Sex.FEMALE, 65.0, 140.0),
                foods = listOf(FoodSelection("101", ServingSelection("11", 1.0))),
                startTime = OffsetDateTime.parse("2026-09-16T08:30:00-05:00"),
            ),
        )
        assertEquals(5, prediction.prediction.size)

        val summaryRequest = server.takeRequest()
        assertEquals("Bearer fixture-client-token-1", summaryRequest.getHeader("Authorization"))
        assertEquals("2026-09-14", summaryRequest.requestUrl!!.queryParameter("start_date"))
        val predictRequest = server.takeRequest()
        assertTrue(predictRequest.body.readUtf8().contains("2026-09-16T08:30:00-05:00"))
        // One token fetch served both calls: the cached expiry (Instant + Duration) was honoured.
        assertEquals(1, fetches)
    }

    private companion object {
        const val SUMMARY = """{"group_by": "day", "week_start": null, "timezone": "America/Chicago", "start_date": "2026-09-14", "end_date": "2026-09-14", "buckets": [{"start_date": "2026-09-14", "end_date": "2026-09-14", "logs_count": 1, "days_with_logs": 1, "nutrients": {"calories": {"value": 1853.06, "unit": "kcal"}}}], "totals": {"logs_count": 3, "days_with_logs": 2, "nutrients": {"calories": {"value": 3656.48, "unit": "kcal"}}}, "average_per_logged_day": {"nutrients": {"calories": {"value": 1828.24, "unit": "kcal"}}}}"""
        const val PREDICTION = """{"points":[{"minutes":0,"value":90},{"minutes":30,"value":125},{"minutes":60,"value":140},{"minutes":90,"value":115},{"minutes":120,"value":95}],"impact_score":"medium","chart":{"min":70,"max":140}}"""
    }
}
