package ai.january.partner.demo

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryClientToken
import ai.january.partner.PartnerUserId
import ai.january.partner.PartnerUserContext
import ai.january.partner.JanuaryPartnerUserClient
import ai.january.partner.forJanuaryDevelopment
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class DemoState(context: Context) {
    private val preferences = context.getSharedPreferences("january_demo", Context.MODE_PRIVATE)
    private val endUserIdState = mutableStateOf(preferences.getString("end_user_id", "").orEmpty())
    var endUserId: String
        get() = endUserIdState.value
        set(value) {
            endUserIdState.value = value
            preferences.edit().putString("end_user_id", value).apply()
        }

    private val timezoneState = mutableStateOf(preferences.getString("timezone", java.time.ZoneId.systemDefault().id).orEmpty())
    var timezone: String
        get() = timezoneState.value
        set(value) {
            timezoneState.value = value
            preferences.edit().putString("timezone", value).apply()
        }

    private val tokenHttpClient = OkHttpClient()
    private val partnerTokenUrl = BuildConfig.JANUARY_PARTNER_TOKEN_URL.trim()
    private val internalApiBaseUrl = BuildConfig.JANUARY_INTERNAL_API_BASE_URL.trim()
    val authenticationDescription: String
    val client: JanuaryPartnerClient?

    init {
        if (partnerTokenUrl.isNotEmpty() && internalApiBaseUrl.isEmpty()) {
            authenticationDescription = "Missing january.internalApiBaseUrl"
            client = null
        } else if (partnerTokenUrl.isNotEmpty()) {
            authenticationDescription = "Short-lived token provider"
            client = JanuaryPartnerClient.forJanuaryDevelopment(
                provider = { fetchClientToken(partnerTokenUrl) },
                apiBaseUrl = internalApiBaseUrl,
            )
        } else {
            val apiKey = BuildConfig.JANUARY_API_KEY.trim()
            authenticationDescription = if (apiKey.isEmpty()) {
                "Missing january.apiKey or january.partnerTokenUrl"
            } else {
                "Development API key"
            }
            client = apiKey.takeIf(String::isNotEmpty)
                ?.let { runCatching { JanuaryPartnerClient(it) }.getOrNull() }
        }
    }

    val partnerUserId: PartnerUserId?
        get() = endUserId.trim().takeIf(String::isNotEmpty)?.let(::PartnerUserId)

    val partnerContext: PartnerUserContext?
        get() = partnerUserId?.let { PartnerUserContext(it, timezone.trim().takeIf(String::isNotEmpty)) }

    val userClient: JanuaryPartnerUserClient?
        get() = client?.let { sdk -> partnerContext?.let(sdk::forUser) }

    fun clearUser() {
        endUserId = ""
        timezone = java.time.ZoneId.systemDefault().id
    }

    private fun fetchClientToken(url: String): JanuaryClientToken {
        val userId = endUserId.trim()
        require(userId.isNotEmpty()) { "Set an end user ID before making a January request." }
        val request = Request.Builder()
            .url(url.toHttpUrl().newBuilder().addQueryParameter("user", userId).build())
            .get()
            .build()
        tokenHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("The partner token endpoint returned HTTP ${response.code}.")
            }
            val token = JanuaryClientToken.fromJson(requireNotNull(response.body).string())
            Log.i("JanuaryDemoAuth", "Fetched a short-lived token valid for ${token.expiresIn} seconds")
            return token
        }
    }
}
