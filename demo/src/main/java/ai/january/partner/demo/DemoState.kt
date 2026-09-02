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

class DemoState(context: Context, private val clientOverride: JanuaryPartnerClient? = null) {
    private val preferences = context.getSharedPreferences("january_demo", Context.MODE_PRIVATE)
    private val developmentApiKey = BuildConfig.JANUARY_API_KEY.trim()
    private val partnerTokenUrl = BuildConfig.JANUARY_PARTNER_TOKEN_URL.trim()
    private val partnerSessionToken = BuildConfig.JANUARY_PARTNER_SESSION_TOKEN.trim()
    private val hasConfiguredAuthentication =
        partnerTokenUrl.isNotEmpty() && partnerSessionToken.isNotEmpty() ||
            BuildConfig.DEBUG && developmentApiKey.isNotEmpty()
    private val defaultUserId = if (hasConfiguredAuthentication) "your-android-user-id" else ""
    private val endUserIdState = mutableStateOf(preferences.getString("end_user_id", defaultUserId).orEmpty())
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
    val isDevelopmentAuthentication: Boolean get() = clientOverride == null && BuildConfig.DEBUG && developmentApiKey.isNotEmpty() && partnerTokenUrl.isEmpty()
    val authenticationDescription: String
    private var cachedClientUserId: String? = null
    private var cachedClient: JanuaryPartnerClient? = null
    val client: JanuaryPartnerClient?
        get() {
            clientOverride?.let { return it }
            val userId = endUserId.trim()
            if (cachedClientUserId != userId) {
                cachedClient = createClient(userId)
                cachedClientUserId = userId
            }
            return cachedClient
        }

    init {
        if (partnerTokenUrl.isNotEmpty() && partnerSessionToken.isEmpty()) {
            authenticationDescription = "Missing january.partnerSessionToken"
        } else if (partnerTokenUrl.isNotEmpty()) {
            authenticationDescription = "Partner backend token provider"
        } else {
            authenticationDescription = if (developmentApiKey.isEmpty()) {
                "Missing january.apiKey or january.partnerTokenUrl"
            } else if (!BuildConfig.DEBUG) {
                "Development authentication is disabled in Release builds. Configure a partner token endpoint."
            } else {
                "Development API key"
            }
        }
    }

    private fun createClient(userId: String): JanuaryPartnerClient? = when {
        partnerTokenUrl.isNotEmpty() && partnerSessionToken.isNotEmpty() ->
            JanuaryPartnerClient.withClientTokenProvider(
                provider = { fetchClientToken(partnerTokenUrl, partnerSessionToken, userId) },
            )
        partnerTokenUrl.isEmpty() && BuildConfig.DEBUG && developmentApiKey.isNotEmpty() ->
            JanuaryPartnerClient(developmentApiKey)
        else -> null
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

    private fun fetchClientToken(url: String, sessionToken: String, userId: String): JanuaryClientToken {
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
