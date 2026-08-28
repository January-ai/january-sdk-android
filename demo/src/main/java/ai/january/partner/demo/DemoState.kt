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
import ai.january.partner.JanuaryTokenProviderException
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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
    private val partnerSessionToken = BuildConfig.JANUARY_PARTNER_SESSION_TOKEN.trim()
    val authenticationDescription: String
    val client: JanuaryPartnerClient?

    init {
        if (partnerTokenUrl.isNotEmpty() && partnerSessionToken.isEmpty()) {
            authenticationDescription = "Missing january.partnerSessionToken"
            client = null
        } else if (partnerTokenUrl.isNotEmpty()) {
            authenticationDescription = "Partner backend token provider"
            client = JanuaryPartnerClient.withClientTokenProvider(
                provider = { fetchClientToken(partnerTokenUrl, partnerSessionToken) },
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

    private fun fetchClientToken(url: String, sessionToken: String): JanuaryClientToken {
        val userId = endUserId.trim()
        require(userId.isNotEmpty()) { "Set an end user ID before making a January request." }
        val request = Request.Builder()
            .url(url.toHttpUrl())
            .post(ByteArray(0).toRequestBody())
            .header("Authorization", "Bearer $sessionToken")
            .header("x-end-user-id", userId)
            .build()
        val response = try {
            tokenHttpClient.newCall(request).execute()
        } catch (error: java.io.IOException) {
            throw JanuaryTokenProviderException(
                "The partner token endpoint is unavailable.",
                retryable = true,
                cause = error,
            )
        }
        response.use {
            if (!response.isSuccessful) {
                throw JanuaryTokenProviderException(
                    "The partner token endpoint rejected the request.",
                    retryable = response.code == 408 || response.code == 429 || response.code >= 500,
                )
            }
            val token = JanuaryClientToken.fromJson(requireNotNull(response.body).string())
            Log.i("JanuaryDemoAuth", "Fetched a short-lived token valid for ${token.expiresIn} seconds")
            return token
        }
    }
}
