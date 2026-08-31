package ai.january.partner

import ai.january.partner.foods.SearchFoodsRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import com.squareup.moshi.JsonDataException
import retrofit2.Response

class TransportErrorParityTest {
    @Test fun searchPreservesErrorDetailsAndUsesSharedStatusCategories(): Unit = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            val client = JanuaryPartnerClient.testing("fixture-key", server.url("/").toString(), OkHttpClient.Builder())
            for ((status, category) in listOf(422 to ErrorCategory.VALIDATION, 504 to ErrorCategory.TIMEOUT, 429 to ErrorCategory.RATE_LIMITED)) {
                server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody("""{"message":"Fixture error","code":"fixture_error","request_id":"parity-request"}"""))
                val failure = runCatching { client.foods.search(SearchFoodsRequest("oatmeal")) }.exceptionOrNull() as JanuaryException
                assertEquals(category, failure.category)
                assertEquals(status, failure.httpStatus)
                assertEquals("Fixture error", failure.message)
                assertEquals("fixture_error", failure.code)
                assertEquals("parity-request", failure.requestId)
            }
        } finally { server.shutdown() }
    }
    @Test fun malformedSuccessfulResponseProducesRecoverableDecodingError(): Unit = runBlocking {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"items":"not an array"}"""))
            val client = JanuaryPartnerClient.testing("fixture-key", server.url("/").toString(), OkHttpClient.Builder())
            val failure = runCatching { client.foods.search(SearchFoodsRequest("oatmeal")) }.exceptionOrNull() as JanuaryException
            assertEquals(ErrorCategory.DECODING, failure.category)
        } finally { server.shutdown() }
    }
    @Test fun socketTimeoutHasDistinctRecoveryMessage(): Unit = runBlocking {
        val failure = runCatching {
            executeApiCall<String,String>(operation = { throw SocketTimeoutException() }, transform = { it })
        }.exceptionOrNull() as JanuaryException
        assertEquals(ErrorCategory.TIMEOUT, failure.category)
        assertEquals("The request to the January API timed out.", failure.message)
    }
}
