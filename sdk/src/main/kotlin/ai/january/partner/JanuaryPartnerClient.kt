package ai.january.partner

import ai.january.partner.foods.FoodsResource
import ai.january.partner.foodlogs.FoodLogsResource
import ai.january.partner.glucose.GlucoseResource
import ai.january.partner.photos.PhotoScanningResource
import ai.january.partner.restaurants.RestaurantsResource
import ai.january.partner.transport.apis.FoodLogsApi
import ai.january.partner.transport.apis.FoodsApi
import ai.january.partner.transport.apis.GlucoseApi
import ai.january.partner.transport.apis.PhotoScanningApi
import ai.january.partner.transport.apis.RestaurantsApi
import ai.january.partner.transport.infrastructure.ApiClient
import okhttp3.OkHttpClient

public class JanuaryPartnerClient private constructor(
    developmentApiKey: String,
    baseUrl: String,
    clientBuilder: OkHttpClient.Builder,
) {
    public val foods: FoodsResource
    public val restaurants: RestaurantsResource
    public val photoScanning: PhotoScanningResource
    public val foodLogs: FoodLogsResource
    public val glucose: GlucoseResource

    public constructor(developmentApiKey: String) : this(
        developmentApiKey = developmentApiKey,
        baseUrl = DEVELOPMENT_BASE_URL,
        clientBuilder = OkHttpClient.Builder(),
    )

    init {
        require(developmentApiKey.isNotBlank()) { "A development API key is required." }
        clientBuilder.addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "JanuaryPartnerSDK-Android/$SDK_VERSION Kotlin/2.4 Android")
                .build()
            chain.proceed(request)
        }
        val apiClient = ApiClient(
            baseUrl = baseUrl,
            okHttpClientBuilder = clientBuilder,
            authName = "bearerAuth",
            bearerToken = developmentApiKey,
        )
        foods = FoodsResource(apiClient.createService(FoodsApi::class.java))
        restaurants = RestaurantsResource(apiClient.createService(RestaurantsApi::class.java))
        photoScanning = PhotoScanningResource(apiClient.createService(PhotoScanningApi::class.java))
        foodLogs = FoodLogsResource(apiClient.createService(FoodLogsApi::class.java))
        glucose = GlucoseResource(apiClient.createService(GlucoseApi::class.java))
    }

    internal companion object {
        internal const val SDK_VERSION = "0.1.0"
        internal const val DEVELOPMENT_BASE_URL = "https://partners.dev.january.ai"

        internal fun testing(
            apiKey: String,
            baseUrl: String,
            clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
        ): JanuaryPartnerClient = JanuaryPartnerClient(apiKey, baseUrl, clientBuilder)
    }
}
