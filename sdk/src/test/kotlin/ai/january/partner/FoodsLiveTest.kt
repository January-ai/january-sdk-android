package ai.january.partner

import ai.january.partner.foods.SearchFoodsRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

public class FoodsLiveTest {
    @Test
    public fun searchesDevelopmentThroughPublicSdk(): Unit = runBlocking {
        val apiKey = System.getenv("JANUARY_API_KEY")
        assumeTrue("JANUARY_API_KEY is not configured.", !apiKey.isNullOrBlank())

        val result = JanuaryPartnerClient(apiKey.orEmpty()).foods.search(
            SearchFoodsRequest(
                query = "banana",
                limit = 3,
                endUserId = System.getenv("JANUARY_END_USER_ID")
                    ?.takeIf(String::isNotBlank)
                    ?.let(::PartnerUserId),
            ),
        )

        assertTrue("Expected development food search to return at least one item.", result.items.isNotEmpty())
    }
}
