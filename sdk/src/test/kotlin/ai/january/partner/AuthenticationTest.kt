package ai.january.partner

import ai.january.partner.foods.SearchFoodsRequest
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

public class AuthenticationTest {
    @Suppress("DEPRECATION")
    @Test
    public fun developmentApiKeyWarnsAndBlankKeysFailValidation() {
        val originalError = System.err
        val output = ByteArrayOutputStream()
        System.setErr(PrintStream(output))
        try {
            JanuaryPartnerClient("sk-local-only")
        } finally {
            System.setErr(originalError)
        }

        assertTrue(output.toString().contains("local testing only"))
        assertTrue(output.toString().contains("Do not ship"))
        runCatching { JanuaryPartnerClient("  ") }
            .onSuccess { fail("Expected blank development key validation") }
    }

    @Test
    public fun clientTokenUsesPartnerServerResponseShape() {
        val camelCase = JanuaryClientToken.fromJson("""{"token":"ct-direct","expiresIn":1800}""")
        val snakeCase = JanuaryClientToken.fromJson("""{"token":"ct-direct","expires_in":1800}""")

        assertEquals(JanuaryClientToken("ct-direct", 1800), camelCase)
        assertEquals(camelCase, snakeCase)
    }

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
    public fun fixedClientTokenIsInjected(): Unit = runBlocking {
        server.enqueue(okResponse())
        val client = JanuaryPartnerClient.testingClientToken(
            clientToken = "ct-fixed",
            baseUrl = server.url("/").toString(),
        )

        client.foods.search(SearchFoodsRequest("banana"))

        assertEquals("Bearer ct-fixed", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    public fun providerTokenIsCachedAndRefreshedOnceAfterTokenExpired(): Unit = runBlocking {
        server.enqueue(okResponse())
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"expired","code":"token_expired"}"""),
        )
        server.enqueue(okResponse())
        val calls = AtomicInteger()
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                JanuaryClientToken(
                    token = if (calls.getAndIncrement() == 0) "ct-one" else "ct-two",
                    expiresIn = 1_800,
                )
            },
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
            now = { Instant.parse("2026-08-26T00:00:00Z") },
        )

        client.foods.search(SearchFoodsRequest("banana"))
        client.foods.search(SearchFoodsRequest("apple"))

        assertEquals(2, calls.get())
        assertEquals("Bearer ct-one", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer ct-one", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer ct-two", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    public fun providerFetchRetriesWithBoundedExponentialBackoff(): Unit = runBlocking {
        server.enqueue(okResponse())
        val calls = AtomicInteger()
        val delays = mutableListOf<Duration>()
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                if (calls.incrementAndGet() <= 2) {
                    throw JanuaryTokenProviderException("temporary partner backend failure", retryable = true)
                }
                JanuaryClientToken("ct-recovered", 1_800)
            },
            baseUrl = server.url("/").toString(),
            tokenRetryPolicy = JanuaryTokenRetryPolicy(
                maximumAttempts = 9,
                initialDelay = Duration.ofSeconds(1),
                multiplier = 2.0,
                maximumDelay = Duration.ofSeconds(8),
                jitterRatio = 0.0,
            ),
            sleep = { delays += it },
        )

        client.foods.search(SearchFoodsRequest("banana"))

        assertEquals(3, calls.get())
        assertEquals(listOf(Duration.ofSeconds(1), Duration.ofSeconds(2)), delays)
        assertEquals("Bearer ct-recovered", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    public fun providerFetchStopsAfterMaximumAttempts(): Unit = runBlocking {
        val calls = AtomicInteger()
        val delays = mutableListOf<Duration>()
        val manager = ClientTokenManager(
            provider = JanuaryTokenProvider {
                calls.incrementAndGet()
                throw JanuaryTokenProviderException("partner backend unavailable", retryable = true)
            },
            retryPolicy = JanuaryTokenRetryPolicy(
                maximumAttempts = 9,
                initialDelay = Duration.ofSeconds(1),
                multiplier = 2.0,
                maximumDelay = Duration.ofSeconds(8),
                jitterRatio = 0.0,
            ),
            sleep = { delays += it },
        )

        try {
            manager.token()
            fail("Expected token-provider failure")
        } catch (error: JanuaryException) {
            assertEquals(ErrorCategory.AUTHENTICATION, error.category)
            assertTrue(error.message.orEmpty().contains("after 9 attempts"))
        }

        assertEquals(9, calls.get())
        assertEquals(
            listOf(
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(4),
                Duration.ofSeconds(8),
                Duration.ofSeconds(8),
                Duration.ofSeconds(8),
                Duration.ofSeconds(8),
                Duration.ofSeconds(8),
            ),
            delays,
        )
    }

    @Test
    public fun tokenExpiredRefreshUsesBackoffBeforeSingleReplay(): Unit = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"expired","code":"token_expired"}"""),
        )
        server.enqueue(okResponse())
        val calls = AtomicInteger()
        val delays = mutableListOf<Duration>()
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                when (calls.incrementAndGet()) {
                    1 -> JanuaryClientToken("ct-expired", 1_800)
                    2, 3 -> throw JanuaryTokenProviderException(
                        "temporary partner backend failure",
                        retryable = true,
                    )
                    else -> JanuaryClientToken("ct-refreshed", 1_800)
                }
            },
            baseUrl = server.url("/").toString(),
            tokenRetryPolicy = JanuaryTokenRetryPolicy(
                maximumAttempts = 9,
                initialDelay = Duration.ofSeconds(1),
                multiplier = 2.0,
                maximumDelay = Duration.ofSeconds(8),
                jitterRatio = 0.0,
            ),
            sleep = { delays += it },
        )

        client.foods.search(SearchFoodsRequest("banana"))

        assertEquals(4, calls.get())
        assertEquals(listOf(Duration.ofSeconds(1), Duration.ofSeconds(2)), delays)
        assertEquals("Bearer ct-expired", server.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer ct-refreshed", server.takeRequest().getHeader("Authorization"))
        assertEquals(2, server.requestCount)
    }

    @Test
    public fun concurrentRequestsShareOneProviderRetrySequence(): Unit = runBlocking {
        repeat(8) { server.enqueue(okResponse()) }
        val calls = AtomicInteger()
        val delays = Collections.synchronizedList(mutableListOf<Duration>())
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                val call = calls.incrementAndGet()
                delay(25)
                if (call == 1) {
                    throw JanuaryTokenProviderException("temporary partner backend failure", retryable = true)
                }
                JanuaryClientToken("ct-shared", 1_800)
            },
            baseUrl = server.url("/").toString(),
            tokenRetryPolicy = JanuaryTokenRetryPolicy(
                maximumAttempts = 9,
                initialDelay = Duration.ofSeconds(1),
                jitterRatio = 0.0,
            ),
            sleep = { delays += it },
        )

        coroutineScope {
            (0 until 8).map { index ->
                async { client.foods.search(SearchFoodsRequest("food-$index")) }
            }.awaitAll()
        }

        assertEquals(2, calls.get())
        assertEquals(listOf(Duration.ofSeconds(1)), delays)
        assertEquals(8, server.requestCount)
    }

    @Test
    public fun malformedProviderTokenDoesNotRetry(): Unit = runBlocking {
        val calls = AtomicInteger()
        val delays = mutableListOf<Duration>()
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                calls.incrementAndGet()
                JanuaryClientToken("  ", 1_800)
            },
            baseUrl = server.url("/").toString(),
            sleep = { delays += it },
        )

        runCatching { client.foods.search(SearchFoodsRequest("banana")) }

        assertEquals(1, calls.get())
        assertTrue(delays.isEmpty())
        assertEquals(0, server.requestCount)
    }

    @Test
    public fun ordinaryProviderFailureDoesNotRetry(): Unit = runBlocking {
        val calls = AtomicInteger()
        val manager = ClientTokenManager(
            provider = JanuaryTokenProvider {
                calls.incrementAndGet()
                error("invalid partner session")
            },
            retryPolicy = JanuaryTokenRetryPolicy(maximumAttempts = 9),
            sleep = { fail("A permanent provider error must not sleep or retry") },
        )

        try {
            manager.token()
            fail("Expected provider failure")
        } catch (error: IllegalStateException) {
            assertEquals("invalid partner session", error.message)
        }
        assertEquals(1, calls.get())
    }

    @Test
    public fun retryPolicyAddsBoundedJitter() {
        val policy = JanuaryTokenRetryPolicy(
            maximumAttempts = 9,
            initialDelay = Duration.ofSeconds(1),
            multiplier = 3.0,
            maximumDelay = Duration.ofSeconds(5),
            jitterRatio = 0.2,
        )

        assertEquals(Duration.ofMillis(800), policy.delayAfterFailedAttempt(1, 0.0))
        assertEquals(Duration.ofMillis(1_200), policy.delayAfterFailedAttempt(1, 1.0))
        assertEquals(Duration.ofSeconds(3), policy.delayAfterFailedAttempt(2, 0.5))
        assertEquals(Duration.ofSeconds(5), policy.delayAfterFailedAttempt(3, 1.0))
        assertEquals(1, JanuaryTokenRetryPolicy.NONE.maximumAttempts)
    }

    @Test
    public fun clientTokenOmitsEndUserHeader(): Unit = runBlocking {
        server.enqueue(okResponse())
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider { JanuaryClientToken("ct-user", 1_800) },
            baseUrl = server.url("/").toString(),
        )

        client.foods.search(
            SearchFoodsRequest(
                query = "banana",
                endUserId = PartnerUserId("partner-user"),
            ),
        )

        assertEquals(null, server.takeRequest().getHeader("x-end-user-id"))
    }

    @Test
    public fun tokenInvalidDoesNotRefresh(): Unit = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"")
                .setHeader("Content-Type", "application/json")
                .setBody("""{"message":"invalid","code":"token_invalid"}"""),
        )
        val calls = AtomicInteger()
        val client = JanuaryPartnerClient.testing(
            provider = JanuaryTokenProvider {
                calls.incrementAndGet()
                JanuaryClientToken("ct-invalid", 1_800)
            },
            baseUrl = server.url("/").toString(),
        )

        runCatching { client.foods.search(SearchFoodsRequest("banana")) }

        assertEquals(1, calls.get())
        assertEquals(1, server.requestCount)
    }

    private fun okResponse(): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"total_count":0,"items":[]}""")
}
