package ai.january.partner

import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.waterlogs.CreateWaterLogRequest
import ai.january.partner.waterlogs.DeleteWaterLogRequest
import ai.january.partner.waterlogs.ListWaterLogsRequest
import ai.january.partner.waterlogs.VolumeUnit
import ai.january.partner.waterlogs.WaterAmount
import ai.january.partner.weightlogs.CreateWeightLogRequest
import ai.january.partner.weightlogs.ListWeightLogsRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Shapes from the shared contract fixtures for the water-log and weight-log operations. */
public class WaterAndWeightLogsResourceTest {
    private lateinit var server: MockWebServer
    private lateinit var client: JanuaryPartnerClient
    private val user = PartnerUserContext(PartnerUserId("oren-sdk-test"), "America/Los_Angeles")

    @Before
    public fun setUp() {
        server = MockWebServer().apply { start() }
        client = JanuaryPartnerClient.testing("fixture-api-key", server.url("/").toString(), OkHttpClient.Builder())
    }

    @After
    public fun tearDown(): Unit = server.shutdown()

    private fun enqueue(body: String, status: Int = 200) {
        val response = MockResponse().setResponseCode(status)
        server.enqueue(if (body.isEmpty()) response else response.setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test
    public fun waterLogCreateSendsTheAmountAndDecodesTheLog(): Unit = runBlocking {
        enqueue(WATER_LOG, 201)

        val log = client.waterLogs.create(CreateWaterLogRequest(WaterAmount(8.0, VolumeUnit.FL_OZ), "2026-09-10T14:30:15Z", user))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1.2/water-logs", request.path)
        assertEquals("oren-sdk-test", request.getHeader("January-End-User-ID"))
        assertEquals("""{"amount":{"value":8.0,"unit":"fl_oz"},"consumed_at":"2026-09-10T14:30:15Z"}""", request.body.readUtf8())
        assertEquals("9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607", log.id)
        assertEquals(WaterAmount(8.0, VolumeUnit.FL_OZ), log.amount)
        assertEquals("2026-09-10T14:30:15.123Z", log.consumedAt)
    }

    @Test
    public fun waterLogCreateOmitsTheTimestampWhenNotGiven(): Unit = runBlocking {
        enqueue(WATER_LOG, 201)

        client.waterLogs.create(CreateWaterLogRequest(WaterAmount(250.0, VolumeUnit.ML), user = user))

        assertEquals("""{"amount":{"value":250.0,"unit":"ml"}}""", server.takeRequest().body.readUtf8())
    }

    @Test
    public fun waterLogListSendsTheRangeAndUnitAndDecodesDailyTotals(): Unit = runBlocking {
        enqueue("""{"items":[{"date":"2026-09-09","total":{"value":48.5,"unit":"fl_oz"}},{"date":"2026-09-10","total":{"value":64,"unit":"fl_oz"}}]}""")

        val days = client.waterLogs.list(ListWaterLogsRequest("2026-09-01", "2026-09-10", VolumeUnit.FL_OZ, user)).items

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1.2/water-logs", url.encodedPath)
        assertEquals("2026-09-01", url.queryParameter("start_date"))
        assertEquals("2026-09-10", url.queryParameter("end_date"))
        assertEquals("America/Los_Angeles", url.queryParameter("timezone"))
        assertEquals("fl_oz", url.queryParameter("unit"))
        assertEquals(listOf("2026-09-09", "2026-09-10"), days.map { it.date })
        assertEquals(48.5, days[0].total.value, 0.0)
        assertEquals(64.0, days[1].total.value, 0.0)
        assertEquals(VolumeUnit.FL_OZ, days[1].total.unit)
    }

    @Test
    public fun waterLogListDefaultsToUtcWithoutATimezoneAndAcceptsAnEmptyList(): Unit = runBlocking {
        enqueue("""{"items":[]}""")

        val days = client.waterLogs.list(ListWaterLogsRequest("2026-09-01", "2026-09-10", VolumeUnit.ML, PartnerUserContext(PartnerUserId("u")))).items

        assertEquals("UTC", server.takeRequest().requestUrl!!.queryParameter("timezone"))
        assertTrue(days.isEmpty())
    }

    @Test
    public fun waterLogDeleteTargetsTheLogAndAcceptsNoContent(): Unit = runBlocking {
        enqueue("", 204)

        client.waterLogs.delete(DeleteWaterLogRequest("9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607", user))

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/v1.2/water-logs/9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607", request.path)
        assertEquals("oren-sdk-test", request.getHeader("January-End-User-ID"))
    }

    @Test
    public fun weightLogCreateSendsTheWeightAndDecodesTheLog(): Unit = runBlocking {
        enqueue(WEIGHT_LOG, 201)

        val log = client.weightLogs.create(CreateWeightLogRequest(Weight(150.0, WeightUnit.POUNDS), "2026-09-10T14:30:15Z", user))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1.2/weight-logs", request.path)
        assertEquals("""{"weight":{"value":150.0,"unit":"lb"},"measured_at":"2026-09-10T14:30:15Z"}""", request.body.readUtf8())
        assertEquals(Weight(150.0, WeightUnit.POUNDS), log.weight)
        assertEquals("2026-09-10T14:30:15.123Z", log.measuredAt)
    }

    @Test
    public fun weightLogListSendsTheRangeAndDecodesDailyWeights(): Unit = runBlocking {
        enqueue("""{"items":[{"date":"2026-09-08","weight":{"value":151.2,"unit":"lb"}},{"date":"2026-09-10","weight":{"value":68.5,"unit":"kg"}}]}""")

        val days = client.weightLogs.list(ListWeightLogsRequest("2026-09-01", "2026-09-10", user)).items

        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1.2/weight-logs", url.encodedPath)
        assertEquals("2026-09-01", url.queryParameter("start_date"))
        assertEquals("2026-09-10", url.queryParameter("end_date"))
        assertEquals("America/Los_Angeles", url.queryParameter("timezone"))
        assertNull(url.queryParameter("unit"))
        assertEquals(Weight(151.2, WeightUnit.POUNDS), days[0].weight)
        assertEquals(Weight(68.5, WeightUnit.KILOGRAMS), days[1].weight)
    }

    @Test
    public fun scopedClientReusesTheContextForWaterAndWeightLogs(): Unit = runBlocking {
        enqueue(WATER_LOG, 201)
        enqueue("""{"items":[]}""")
        enqueue("", 204)
        enqueue(WEIGHT_LOG, 201)
        enqueue("""{"items":[]}""")
        val scoped = client.forUser(PartnerUserId("scoped-user"), "America/New_York")

        val water = scoped.waterLogs.create(WaterAmount(8.0, VolumeUnit.FL_OZ))
        scoped.waterLogs.list("2026-09-01", "2026-09-10", VolumeUnit.FL_OZ)
        scoped.waterLogs.delete(water.id)
        scoped.weightLogs.create(Weight(70.0, WeightUnit.KILOGRAMS))
        scoped.weightLogs.list("2026-09-01", "2026-09-10")

        val requests = List(5) { server.takeRequest() }
        requests.forEach { assertEquals("scoped-user", it.getHeader("January-End-User-ID")) }
        assertEquals("America/New_York", requests[1].requestUrl!!.queryParameter("timezone"))
        assertEquals("America/New_York", requests[4].requestUrl!!.queryParameter("timezone"))
        assertEquals("""{"weight":{"value":70.0,"unit":"kg"}}""", requests[3].body.readUtf8())
    }

    @Test
    public fun dailyCapAndRangeErrorsAreValidationFailuresWithTheirCodes(): Unit = runBlocking {
        enqueue("""{"code":"daily_water_limit_exceeded","message":"This log would take the day past 24 L."}""", 400)
        enqueue("""{"code":"date_range_too_large","message":"start_date is more than 5 years ago."}""", 400)

        val cap = runCatching { client.waterLogs.create(CreateWaterLogRequest(WaterAmount(800.0, VolumeUnit.FL_OZ), user = user)) }
            .exceptionOrNull() as JanuaryException
        val range = runCatching { client.weightLogs.list(ListWeightLogsRequest("2019-01-01", "2026-09-10", user)) }
            .exceptionOrNull() as JanuaryException

        assertEquals(ErrorCategory.VALIDATION, cap.category)
        assertEquals("daily_water_limit_exceeded", cap.code)
        assertEquals(400, cap.httpStatus)
        assertEquals(ErrorCategory.VALIDATION, range.category)
        assertEquals("date_range_too_large", range.code)
        // Neither request is retried: one attempt each.
        assertEquals(2, server.requestCount)
    }

    @Test
    public fun unknownUnitsAreReportedAsDecodingFailures(): Unit = runBlocking {
        enqueue("""{"items":[{"date":"2026-09-09","total":{"value":1,"unit":"cups"}}]}""")

        val failure = runCatching { client.waterLogs.list(ListWaterLogsRequest("2026-09-01", "2026-09-10", VolumeUnit.FL_OZ, user)) }
            .exceptionOrNull() as JanuaryException

        assertEquals(ErrorCategory.DECODING, failure.category)
        assertTrue(failure.message!!.contains("cups"))
    }

    private companion object {
        const val WATER_LOG = """{"id":"9c1f2a3b-4d5e-4f60-8a71-b2c3d4e5f607","amount":{"value":8,"unit":"fl_oz"},"consumed_at":"2026-09-10T14:30:15.123Z"}"""
        const val WEIGHT_LOG = """{"weight":{"value":150,"unit":"lb"},"measured_at":"2026-09-10T14:30:15.123Z"}"""
    }
}
