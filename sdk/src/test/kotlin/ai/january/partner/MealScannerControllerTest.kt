package ai.january.partner

import ai.january.partner.scanner.JanuaryMealScannerController
import ai.january.partner.scanner.JanuaryMealScannerResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MealScannerControllerTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun barcodeWorkflowLooksUpThenFetchesCompleteFood(): Unit = runBlocking {
        val food =
            """{"id":42,"name":"Banana","nutrients":{},"servings":[{"id":7,"quantity":1,"unit":"serving","scaling_factor":1,"weight_grams":100,"is_primary":true}]}"""
        server.enqueue(json("""{"total_count":1,"items":[$food]}"""))
        server.enqueue(json(food))
        val client = JanuaryPartnerClient.testing(
            "fixture-api-key",
            server.url("/").toString(),
            OkHttpClient.Builder(),
        )

        val result = JanuaryMealScannerController(client, PartnerUserId("scanner-user"))
            .lookupBarcode("049000006346")

        assertEquals("049000006346", result.value)
        assertEquals(FoodId(42), result.food.id)
        assertEquals(1, result.food.servings.size)
        val lookup = server.takeRequest()
        val fullFood = server.takeRequest()
        assertEquals("/v1.2/foods/barcode/049000006346", lookup.requestUrl!!.encodedPath)
        assertEquals("/v1.2/foods/42", fullFood.requestUrl!!.encodedPath)
        assertEquals("scanner-user", lookup.getHeader("x-end-user-id"))
        assertEquals("scanner-user", fullFood.getHeader("x-end-user-id"))
    }

    private fun json(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
