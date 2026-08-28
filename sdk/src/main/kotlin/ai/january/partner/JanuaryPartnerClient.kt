package ai.january.partner

import ai.january.partner.foods.FoodsResource
import ai.january.partner.foodlogs.FoodLogsResource
import ai.january.partner.glucose.GlucoseResource
import ai.january.partner.photos.FoodAnalysisResource
import ai.january.partner.restaurants.RestaurantsResource
import ai.january.partner.transport.apis.FoodLogsApi
import ai.january.partner.transport.apis.FoodsApi
import ai.january.partner.transport.apis.GlucoseApi
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.apis.RestaurantsApi
import ai.january.partner.transport.infrastructure.ApiClient
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

public class JanuaryPartnerClient private constructor(
    authentication: Authentication,
    baseUrl: String,
    clientBuilder: OkHttpClient.Builder,
) {
    public val foods: FoodsResource
    public val restaurants: RestaurantsResource
    public val foodAnalysis: FoodAnalysisResource
    public val foodLogs: FoodLogsResource
    public val glucose: GlucoseResource

    @Deprecated(
        message = "Local development only. Use withClientTokenProvider in production.",
    )
    public constructor(developmentApiKey: String) : this(
        authentication = Authentication.DevelopmentApiKey(developmentApiKey),
        baseUrl = PRODUCTION_BASE_URL,
        clientBuilder = OkHttpClient.Builder(),
    ) {
        System.err.println(
            "WARNING: January development API-key authentication is for local testing only. " +
                "Do not ship this key; use JanuaryTokenProvider in production.",
        )
    }

    init {
        clientBuilder.addInterceptor(authentication.interceptor())
        clientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "JanuaryPartnerSDK-Android/$SDK_VERSION Kotlin/2.4 Android")
                .build()
            chain.proceed(request)
        }
        val apiClient = ApiClient(
            baseUrl = baseUrl,
            okHttpClientBuilder = clientBuilder,
        )
        val foodAnalysisApi = apiClient.createService(PhotoScanningApi::class.java)
        foods = FoodsResource(apiClient.createService(FoodsApi::class.java))
        restaurants = RestaurantsResource(apiClient.createService(RestaurantsApi::class.java))
        foodAnalysis = FoodAnalysisResource(foodAnalysisApi)
        foodLogs = FoodLogsResource(apiClient.createService(FoodLogsApi::class.java))
        glucose = GlucoseResource(apiClient.createService(GlucoseApi::class.java))
    }

    /** Returns a lightweight client that reuses the supplied partner-owned identity. */
    public fun forUser(context: PartnerUserContext): JanuaryPartnerUserClient =
        JanuaryPartnerUserClient(this, context)

    /** Returns a lightweight client scoped to an end user and optional IANA timezone. */
    public fun forUser(
        endUserId: PartnerUserId,
        timezone: String? = null,
    ): JanuaryPartnerUserClient = forUser(PartnerUserContext(endUserId, timezone))

    public companion object {
        internal const val SDK_VERSION = "0.1.0"
        private const val PRODUCTION_BASE_URL = "https://partners.january.ai"

        /** Creates a client with a short-lived token managed by the integrating app. */
        @JvmStatic
        public fun withClientToken(clientToken: String): JanuaryPartnerClient {
            require(clientToken.isNotBlank()) { "A client token is required." }
            return JanuaryPartnerClient(
                Authentication.FixedClientToken(clientToken.trim()),
                PRODUCTION_BASE_URL,
                OkHttpClient.Builder(),
            )
        }

        /**
         * Creates a client that refreshes short-lived credentials through [provider].
         * Tokens are cached only in memory and refreshed shortly before expiration.
         */
        @JvmStatic
        public fun withClientTokenProvider(
            provider: JanuaryTokenProvider,
            tokenRetryPolicy: JanuaryTokenRetryPolicy = JanuaryTokenRetryPolicy(),
        ): JanuaryPartnerClient = JanuaryPartnerClient(
            Authentication.RefreshingClientToken(
                ClientTokenManager(provider, retryPolicy = tokenRetryPolicy),
            ),
            PRODUCTION_BASE_URL,
            OkHttpClient.Builder(),
        )

        internal fun testing(
            apiKey: String,
            baseUrl: String,
            clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
        ): JanuaryPartnerClient = JanuaryPartnerClient(
            Authentication.DevelopmentApiKey(apiKey),
            baseUrl,
            clientBuilder,
        )

        internal fun testing(
            provider: JanuaryTokenProvider,
            baseUrl: String,
            clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
            refreshLeeway: Duration = Duration.ofSeconds(60),
            tokenRetryPolicy: JanuaryTokenRetryPolicy = JanuaryTokenRetryPolicy(),
            now: () -> Instant = Instant::now,
            sleep: suspend (Duration) -> Unit = { kotlinx.coroutines.delay(it.toMillis()) },
            unitRandom: () -> Double = { kotlin.random.Random.nextDouble() },
        ): JanuaryPartnerClient = JanuaryPartnerClient(
            Authentication.RefreshingClientToken(
                ClientTokenManager(
                    provider = provider,
                    refreshLeeway = refreshLeeway,
                    retryPolicy = tokenRetryPolicy,
                    now = now,
                    sleep = sleep,
                    unitRandom = unitRandom,
                ),
            ),
            baseUrl,
            clientBuilder,
        )

        internal fun testingClientToken(
            clientToken: String,
            baseUrl: String,
            clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
        ): JanuaryPartnerClient = JanuaryPartnerClient(
            Authentication.FixedClientToken(clientToken),
            baseUrl,
            clientBuilder,
        )
    }

    private sealed interface Authentication {
        fun interceptor(): Interceptor

        data class DevelopmentApiKey(val value: String) : Authentication {
            init { require(value.isNotBlank()) { "A development API key is required." } }
            override fun interceptor(): Interceptor = bearerInterceptor(value.trim())
        }

        data class FixedClientToken(val value: String) : Authentication {
            override fun interceptor(): Interceptor = bearerInterceptor(value, omitEndUserId = true)
        }

        data class RefreshingClientToken(val manager: ClientTokenManager) : Authentication {
            override fun interceptor(): Interceptor = Interceptor { chain ->
                val token = runBlocking { manager.token() }
                val request = chain.request().newBuilder()
                    .removeHeader("x-end-user-id")
                    .header("Authorization", "Bearer ${token.token}")
                    .build()
                val response = chain.proceed(request)
                val canReplay = request.body?.isOneShot() != true
                if (
                    response.code != 401 ||
                    responseTokenCode(response) != "token_expired" ||
                    !canReplay
                ) {
                    response
                } else {
                    response.close()
                    runBlocking { manager.invalidateIfMatching(token.token) }
                    val refreshed = runBlocking { manager.token() }
                    chain.proceed(
                        request.newBuilder()
                            .header("Authorization", "Bearer ${refreshed.token}")
                            .build(),
                    )
                }
            }
        }

        companion object {
            private fun bearerInterceptor(
                value: String,
                omitEndUserId: Boolean = false,
            ): Interceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply { if (omitEndUserId) removeHeader("x-end-user-id") }
                    .header("Authorization", "Bearer $value")
                    .build()
                chain.proceed(
                    request,
                )
            }

            private data class TokenErrorEnvelope(val code: String?)

            private val tokenErrorAdapter = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(TokenErrorEnvelope::class.java)

            private fun responseTokenCode(response: okhttp3.Response): String? = runCatching {
                tokenErrorAdapter.fromJson(response.peekBody(64L * 1_024L).string())?.code
            }.getOrNull()
        }
    }
}
