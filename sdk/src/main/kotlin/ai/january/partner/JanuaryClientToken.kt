package ai.january.partner

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class ClientTokenEnvelope(
    val token: String?,
    val expiresIn: Long?,
    @param:Json(name = "expires_in") val snakeExpiresIn: Long?,
)

private val clientTokenAdapter = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
    .adapter(ClientTokenEnvelope::class.java)

/** A short-lived bearer credential returned by a partner-controlled backend. */
public data class JanuaryClientToken(
    /** Opaque bearer token returned by the partner backend. */
    public val token: String,
    /** Lifetime in seconds, measured from when the provider receives the token. */
    public val expiresIn: Long,
) {
    @Deprecated("Use token", ReplaceWith("token"))
    public val accessToken: String get() = token

    @Deprecated("Use token", ReplaceWith("token"))
    public val value: String get() = token

    public companion object {
        /** Decodes either `expiresIn` or January's snake-case `expires_in`. */
        @JvmStatic
        public fun fromJson(json: String): JanuaryClientToken {
            val body = requireNotNull(clientTokenAdapter.fromJson(json)) {
                "The token endpoint returned an empty response."
            }
            return JanuaryClientToken(
                token = requireNotNull(body.token) { "The token response is missing token." },
                expiresIn = requireNotNull(body.expiresIn ?: body.snakeExpiresIn) {
                    "The token response is missing expiresIn."
                },
            )
        }
    }
}

/** Obtains short-lived January credentials using the app's authenticated backend. */
public fun interface JanuaryTokenProvider {
    public suspend fun fetchClientToken(): JanuaryClientToken
}

@Deprecated("Use JanuaryTokenProvider", ReplaceWith("JanuaryTokenProvider"))
public typealias JanuaryClientTokenProvider = JanuaryTokenProvider

internal class ClientTokenManager(
    private val provider: JanuaryTokenProvider,
    private val refreshLeeway: Duration = Duration.ofSeconds(60),
    private val retryPolicy: JanuaryTokenRetryPolicy = JanuaryTokenRetryPolicy(),
    private val now: () -> Instant = Instant::now,
    private val sleep: suspend (Duration) -> Unit = { delay(it.toMillis()) },
    private val unitRandom: () -> Double = { Random.nextDouble() },
) {
    @Volatile
    private var cachedToken: JanuaryClientToken? = null
    private var cachedExpiresAt: Instant? = null
    private val refreshMutex = Mutex()

    public suspend fun token(): JanuaryClientToken {
        cachedToken?.takeIf { isUsable() }?.let { return it }
        return refreshMutex.withLock {
            cachedToken?.takeIf { isUsable() }?.let { return@withLock it }
            val token = fetchWithRetry()
            val normalized = token.token.trim()
            if (normalized.isEmpty()) {
                throw JanuaryException(
                    ErrorCategory.AUTHENTICATION,
                    "The client token provider returned an empty token.",
                )
            }
            if (token.expiresIn <= refreshLeeway.seconds) {
                throw JanuaryException(
                    ErrorCategory.AUTHENTICATION,
                    "The client token provider returned an expired or nearly expired token.",
                )
            }
            JanuaryClientToken(normalized, token.expiresIn).also {
                cachedToken = it
                cachedExpiresAt = now().plusSeconds(token.expiresIn)
            }
        }
    }

    public suspend fun invalidateIfMatching(value: String) {
        refreshMutex.withLock {
            if (cachedToken?.token == value) {
                cachedToken = null
                cachedExpiresAt = null
            }
        }
    }

    private fun isUsable(): Boolean =
        cachedExpiresAt?.isAfter(now().plus(refreshLeeway)) == true

    private suspend fun fetchWithRetry(): JanuaryClientToken {
        var attempt = 1
        while (true) {
            try {
                return provider.fetchClientToken()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (attempt >= retryPolicy.maximumAttempts) {
                    throw JanuaryException(
                        ErrorCategory.AUTHENTICATION,
                        "The app could not obtain a January client token after $attempt attempts.",
                        cause = error,
                    )
                }
                val retryDelay = retryPolicy.delayAfterFailedAttempt(attempt, unitRandom())
                attempt += 1
                if (!retryDelay.isZero) sleep(retryDelay)
            }
        }
    }
}
