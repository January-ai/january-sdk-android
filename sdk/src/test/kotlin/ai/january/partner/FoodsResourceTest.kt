package ai.january.partner

import ai.january.partner.foods.FoodCategory
import ai.january.partner.foods.SearchFoodsRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

public class FoodsResourceTest {
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
    public fun searchUsesCoroutineTransportAndMapsPublicModels(): Unit = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"total_count":1,"items":[{"id":84222716,"name":"Banana","brand_name":"One","nutrients":{"calories":{"value":160,"unit":"kcal"},"protein":{"value":2,"unit":"g"},"carbohydrates":{"value":15,"unit":"g"}},"glycemic_index":34.9,"glycemic_load":5.2,"image_url":null,"servings":[{"id":67943292,"quantity":1,"unit":"bar","scaling_factor":1,"weight_grams":60,"is_primary":true}]}]}""",
                ),
        )
        val client = JanuaryPartnerClient.testing(
            apiKey = "fixture-api-key",
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
        )

        val result = client.foods.search(
            SearchFoodsRequest(
                query = "banana",
                category = FoodCategory.BRANDED,
                limit = 10,
                endUserId = PartnerUserId("test-user-123"),
            ),
        )

        assertEquals(1, result.totalCount)
        assertEquals(FoodId(84_222_716), result.items.single().id)
        assertEquals(ServingId(67_943_292), result.items.single().servings.single().id)

        val request = server.takeRequest()
        assertEquals("Bearer fixture-api-key", request.getHeader("Authorization"))
        assertEquals("test-user-123", request.getHeader("x-end-user-id"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("JanuaryPartnerSDK-Android/0.1.0"))
        assertEquals("/v1.2/foods", request.requestUrl!!.encodedPath)
        assertEquals("banana", request.requestUrl!!.queryParameter("query"))
        assertEquals("branded", request.requestUrl!!.queryParameter("category"))
        assertEquals("10", request.requestUrl!!.queryParameter("limit"))
    }
}
