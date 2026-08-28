package ai.january.partner

import ai.january.partner.photos.ScanFoodPhotoRequest
import com.squareup.moshi.JsonClass
import ai.january.partner.transport.infrastructure.Serializer
import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

public class FoodAnalysisResourceTest {
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
    public fun scanSendsPublicUrlAndPngDataUriThroughPublicClient(): Unit = runBlocking {
        repeat(2) {
            server.enqueue(
                MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                    .setBody("""{"meal_name":"Burger and fries","detections":[]}"""),
            )
        }
        val fixture = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("fixtures/photo-scanning/burger-and-fries.png"),
        ).use { it.readBytes() }
        val dataUri = "data:image/png;base64,${Base64.getEncoder().encodeToString(fixture)}"
        val client = JanuaryPartnerClient.testing(
            apiKey = "fixture-api-key",
            baseUrl = server.url("/").toString(),
            clientBuilder = OkHttpClient.Builder(),
        )

        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest(BURGER_IMAGE_URL))
        client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest(dataUri))

        val adapter = Serializer.moshiBuilder.build().adapter(ScanPayload::class.java)
        val urlRequest = server.takeRequest()
        val dataRequest = server.takeRequest()
        assertEquals("POST", urlRequest.method)
        assertEquals("/v1.2/food-scans/photo", urlRequest.requestUrl!!.encodedPath)
        assertEquals(BURGER_IMAGE_URL, adapter.fromJson(urlRequest.body.readUtf8())!!.image)
        val encodedImage = adapter.fromJson(dataRequest.body.readUtf8())!!.image
        assertTrue(encodedImage.startsWith("data:image/png;base64,"))
        assertArrayEquals(fixture, Base64.getDecoder().decode(encodedImage.substringAfter(',')))
    }

    @JsonClass(generateAdapter = false)
    private data class ScanPayload(val image: String)

    private companion object {
        const val BURGER_IMAGE_URL = "https://friendlysrestaurants.com/assets/live/img/production/detail/menu/lunch-dinner_999-combohs_all-american-burger-fries.jpg"
    }
}
