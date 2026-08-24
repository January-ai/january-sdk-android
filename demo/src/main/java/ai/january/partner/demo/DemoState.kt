package ai.january.partner.demo

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId

class DemoState(context: Context) {
    private val preferences = context.getSharedPreferences("january_demo", Context.MODE_PRIVATE)
    val client: JanuaryPartnerClient? = BuildConfig.JANUARY_API_KEY
        .trim()
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { JanuaryPartnerClient(it) }.getOrNull() }

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

    val partnerUserId: PartnerUserId?
        get() = endUserId.trim().takeIf(String::isNotEmpty)?.let(::PartnerUserId)
}
