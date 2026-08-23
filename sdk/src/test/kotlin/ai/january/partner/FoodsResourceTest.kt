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
                    """{"total_count":1,"items":[{"id":84222716,"name":"Banana","brand_name":"One","energy":160,"protein":2,"carbs":15,"net_carbs":13,"fat":10,"fat_total_saturated":5,"fiber":2,"sugars":13,"added_sugars":11,"sodium":20,"potassium":170,"cholesterol":2.5,"gi":34.9,"gl":5.2,"photo_url":null,"servings":[{"id":67943292,"quantity":1,"unit":"bar","scaling_factor":1,"weight_grams":60,"is_primary":true}]}]}""",
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
                endUserId = PartnerUserId("oren-sdk-test"),
            ),
        )

        assertEquals(1, result.totalCount)
        assertEquals(FoodId(84_222_716), result.items.single().id)
        assertEquals(ServingId(67_943_292), result.items.single().servings.single().id)

        val request = server.takeRequest()
        assertEquals("Bearer fixture-api-key", request.getHeader("Authorization"))
        assertEquals("oren-sdk-test", request.getHeader("x-end-user-id"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("JanuaryPartnerSDK-Android/0.1.0"))
        assertEquals("/v1.2/foods/search", request.requestUrl!!.encodedPath)
        assertEquals("banana", request.requestUrl!!.queryParameter("query"))
        assertEquals("branded", request.requestUrl!!.queryParameter("category"))
        assertEquals("10", request.requestUrl!!.queryParameter("limit"))
    }
}
