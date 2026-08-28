package ai.january.partner

import ai.january.partner.foods.AutocompleteFoodsRequest
import ai.january.partner.foods.GetFoodRequest
import ai.january.partner.foods.SearchFoodsRequest
import ai.january.partner.foods.portion
import org.junit.Assert.assertNotEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

public class FoodsLiveTest {
    @Test
    public fun searchesDevelopmentThroughPublicSdk(): Unit = runBlocking {
        val apiKey = System.getenv("JANUARY_API_KEY")
        assumeTrue("JANUARY_API_KEY is not configured.", !apiKey.isNullOrBlank())

        val foods = JanuaryPartnerClient(apiKey.orEmpty()).foods
        val endUserId = System.getenv("JANUARY_END_USER_ID")
            ?.takeIf(String::isNotBlank)
            ?.let(::PartnerUserId)
        val suggestions = foods.autocomplete(AutocompleteFoodsRequest("ban", limit = 5, endUserId = endUserId))
        assertTrue("Expected autocomplete to return at least one suggestion.", suggestions.items.isNotEmpty())

        val result = foods.search(
            SearchFoodsRequest(
                query = "banana",
                limit = 3,
                endUserId = endUserId,
            ),
        )

        assertTrue("Expected development food search to return at least one item.", result.items.isNotEmpty())
        val food = foods.get(GetFoodRequest(result.items.first().id, endUserId))
        assertTrue("Expected the full food record to include multiple servings.", food.servings.size >= 2)
        val primary = food.portion()
        val alternate = food.portion(food.servings[1].id, 1.5)
        assertNotEquals(
            "Expected a different serving and quantity to recalculate calories.",
            primary.nutrition.calories?.value,
            alternate.nutrition.calories?.value,
        )
    }
}
