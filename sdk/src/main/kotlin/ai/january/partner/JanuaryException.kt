package ai.january.partner

public enum class ErrorCategory {
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    RATE_LIMITED,
    TIMEOUT,
    TRANSPORT,
    DECODING,
    SERVER,
}

public class JanuaryException(
    public val category: ErrorCategory,
    message: String,
    public val httpStatus: Int?,
    cause: Throwable?,
    public val code: String?,
    public val requestId: String?,
) : Exception(message, cause) {
    // Retain the original constructor (including Kotlin defaults) for existing SDK consumers.
    public constructor(
        category: ErrorCategory,
        message: String,
        httpStatus: Int? = null,
        cause: Throwable? = null,
    ) : this(category, message, httpStatus, cause, null, null)
}
