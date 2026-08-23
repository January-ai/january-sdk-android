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
    public val httpStatus: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
