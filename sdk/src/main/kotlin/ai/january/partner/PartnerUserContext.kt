package ai.january.partner

/**
 * Partner-owned identity and locale information reused across January requests.
 *
 * The SDK does not persist this value. Keep it in the application's authenticated
 * session and create a new scoped client when the active user changes or signs out.
 */
public data class PartnerUserContext(
    public val endUserId: PartnerUserId,
    public val timezone: String? = null,
)
