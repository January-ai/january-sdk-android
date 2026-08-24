package ai.january.partner.demo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId

class DemoState {
    val client: JanuaryPartnerClient? = BuildConfig.JANUARY_API_KEY
        .trim()
        .takeIf(String::isNotEmpty)
        ?.let { runCatching { JanuaryPartnerClient(it) }.getOrNull() }

    var endUserId by mutableStateOf("")

    val partnerUserId: PartnerUserId?
        get() = endUserId.trim().takeIf(String::isNotEmpty)?.let(::PartnerUserId)
}
