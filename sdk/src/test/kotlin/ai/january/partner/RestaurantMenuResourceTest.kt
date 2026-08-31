package ai.january.partner

import ai.january.partner.restaurants.SearchRestaurantsRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class RestaurantMenuResourceTest {

    @Test fun menuByIdUsesPaginationAndPreservesNutrition(): Unit = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"total_count":3,"items":[{"type":"menu_item","id":"101","name":"Bowl","restaurant_name":"Cafe","nutrients":{"calories":{"value":220,"unit":"kcal"}},"image_url":"https://example.com/bowl.jpg","servings":[{"id":11,"quantity":1,"unit":"bowl","is_primary":true}]}]}"""))
            val client = JanuaryPartnerClient.testing("fixture-key", server.url("/").toString(), OkHttpClient.Builder())
            val result = client.restaurants.getMenuItems(ai.january.partner.restaurants.GetRestaurantMenuItemsRequest("cafe-123", 2, 1, PartnerUserId("user-1")))
            val request = server.takeRequest()
            assertEquals("/v1.2/restaurants/cafe-123/menu-items?limit=2&offset=1", request.path)
            assertEquals("user-1", request.getHeader("x-end-user-id"))
            assertEquals(3, result.totalCount)
            assertEquals(220.0, result.items.single().calories!!, 0.0)
            assertEquals(1.0, result.items.single().servings.single().scalingFactor, 0.0)
        } finally { server.shutdown() }
    }
    @Test fun menuDetailsPreserveNestedNutritionImagesAndEveryServing(): Unit = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"total_count":1,"items":[{"type":"menu_item","id":"menu-item-42","name":"Oatmeal","restaurant_name":"Cafe",
                "nutrients":{"calories":{"value":200,"unit":"kcal"},"protein":{"value":8,"unit":"g"},"carbohydrates":{"value":35,"unit":"g"},"total_fat":{"value":4,"unit":"g"}},
                "glycemic_index":45,"glycemic_load":12,"image_url":"https://example.com/oatmeal.png",
                "servings":[{"id":11,"quantity":1,"unit":"bowl","scaling_factor":1,"weight_grams":200,"is_primary":true},
                {"id":12,"quantity":1,"unit":"oz","scaling_factor":0.14175,"weight_grams":28.35,"is_primary":false}]}]}
            """.trimIndent()))
            val client = JanuaryPartnerClient.testing("fixture-api-key", server.url("/").toString(), OkHttpClient.Builder())
            val result = client.restaurants.searchMenuItems(SearchRestaurantsRequest("Cafe", 40.0, -74.0))
            assertEquals(1, result.totalCount)
            val item = result.items.single()
            assertEquals("menu-item-42", item.id)
            assertEquals(200.0, item.calories!!, 0.0)
            assertEquals(8.0, item.protein!!, 0.0)
            assertEquals(35.0, item.carbohydrates!!, 0.0)
            assertEquals(4.0, item.totalFat!!, 0.0)
            assertEquals(45.0, item.glycemicIndex!!, 0.0)
            assertEquals("https://example.com/oatmeal.png", item.photoUrl)
            assertEquals(listOf(11L, 12L), item.servings.map { it.id.value })
            assertEquals(0.14175, item.servings.last().scalingFactor, 0.0)
            assertEquals(28.35, item.servings.last().weightGrams!!, 0.0)
            assertTrue(item.servings.first().isPrimary)
            assertFalse(item.servings.last().isPrimary)
        } finally { server.shutdown() }
    }
}
