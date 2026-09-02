package ai.january.partner

import ai.january.partner.scanner.JanuaryFoodScannerController
import ai.january.partner.scanner.JanuaryFoodScannerResult
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
            """{"id":"42","type":"generic","name":"Banana","brand_name":null,"nutrients":{},"glycemic_index":null,"glycemic_load":null,"image_url":null,"barcode":"049000006346","servings":[{"id":"7","quantity":1,"unit":"serving","scaling_factor":1,"weight_grams":100,"is_primary":true}]}"""
        server.enqueue(json(food))
        server.enqueue(json(food))
        val client = JanuaryPartnerClient.testing(
            "fixture-api-key",
            server.url("/").toString(),
            OkHttpClient.Builder(),
        )

        val result = JanuaryFoodScannerController(client, PartnerUserId("scanner-user"))
            .lookupBarcode("049000006346")

        assertEquals("049000006346", result.value)
        assertEquals(FoodId(42), result.food.id)
        assertEquals(1, result.food.servings.size)
        val lookup = server.takeRequest()
        val fullFood = server.takeRequest()
        assertEquals("/v1.2/foods/barcode/049000006346", lookup.requestUrl!!.encodedPath)
        assertEquals("/v1.2/foods/42", fullFood.requestUrl!!.encodedPath)
        assertEquals(null, lookup.getHeader("January-End-User-ID"))
        assertEquals(null, fullFood.getHeader("January-End-User-ID"))
    }

    private fun json(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
