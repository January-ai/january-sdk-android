package ai.january.partner

import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
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

public class UserScopedClientTest {
    private lateinit var server: MockWebServer

    @Before
    public fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    public fun tearDown() {
        server.shutdown()
    }

    @Test
    public fun scopedClientReusesIdentityAndPreservesFoodLogRequestShapes(): Unit = runBlocking {
        server.enqueue(jsonResponse(FOOD_LOG))
        server.enqueue(jsonResponse("""{"total_count":0,"items":[]}"""))
        server.enqueue(jsonResponse(FOOD_LOG))
        server.enqueue(jsonResponse("""{"status":"deleted"}"""))
        server.enqueue(
            jsonResponse(
                """{"prediction":[{"minutes":0,"value":100}],"impact_score":"low","chart":{"min":70,"max":140}}""",
            ),
        )
        val client = JanuaryPartnerClient.testing(
            apiKey = "fixture-api-key",
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
        )
        val context = PartnerUserContext(PartnerUserId("scoped-user"), "America/New_York")
        val scoped = client.forUser(context)
        val foods = listOf(
            FoodSelection(42, ServingSelection(7, 1.5)),
            FoodSelection(84, ServingSelection(9, 0.75)),
        )
        val timestamp = "2024-09-13T11:34:56Z"

        val created = scoped.foodLogs.create(foods, timestamp, "Lunch")
        scoped.foodLogs.list("2023-09-12", "2024-09-15")
        scoped.foodLogs.update(created.id, foods, timestamp, "Updated lunch")
        scoped.foodLogs.delete(created.id)
        scoped.glucose.predict(
            PredictGlucoseRequest(
                userProfile = GlucosePredictionProfile(35.0, Sex.FEMALE, 65.0, 140.0),
                foods = foods,
                startTime = OffsetDateTime.parse(timestamp),
                endUserId = PartnerUserId("request-user-is-replaced"),
                timezone = "UTC",
            ),
        )

        assertEquals(context, scoped.context)
        val requests = List(5) { server.takeRequest() }
        requests.forEach { request ->
            assertEquals("scoped-user", request.getHeader("x-end-user-id"))
            assertEquals("America/New_York", request.getHeader("x-end-user-timezone"))
        }

        val createBody = requests[0].body.readUtf8()
        assertTrue(createBody.contains("\"timestamp_utc\":\"$timestamp\""))
        assertEquals(2, "\"serving\"".toRegex().findAll(createBody).count())
        assertEquals("2023-09-12", requests[1].requestUrl!!.queryParameter("start"))
        assertEquals("2024-09-15", requests[1].requestUrl!!.queryParameter("end"))
        val updateBody = requests[2].body.readUtf8()
        assertTrue(updateBody.contains("\"timestamp_utc\":\"$timestamp\""))
        assertEquals(2, "\"serving\"".toRegex().findAll(updateBody).count())
        assertTrue(requests[4].body.readUtf8().contains("\"foods\""))
    }

    private fun jsonResponse(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val FOOD_LOG =
            """{"id":"00000000-0000-0000-0000-000000000001","foods":[],"timestamp_utc":"2024-09-13T11:34:56Z","name":"Lunch"}"""
    }
}
