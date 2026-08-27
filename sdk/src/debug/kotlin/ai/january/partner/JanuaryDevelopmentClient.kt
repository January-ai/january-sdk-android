package ai.january.partner

/**
 * Debug-variant factory for January-owned demos and smoke tests.
 * This symbol is excluded from the published release AAR.
 */
public fun JanuaryPartnerClient.Companion.forJanuaryDevelopment(
    provider: JanuaryTokenProvider,
    apiBaseUrl: String,
): JanuaryPartnerClient {
    require(apiBaseUrl.isNotBlank()) { "An explicit January development API URL is required." }
    return testing(provider = provider, baseUrl = apiBaseUrl)
}
