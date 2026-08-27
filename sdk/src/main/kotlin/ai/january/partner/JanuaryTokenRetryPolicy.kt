package ai.january.partner

import java.time.Duration
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/** Controls bounded retries when the app's token provider fails to fetch a credential. */
public data class JanuaryTokenRetryPolicy(
    /** Total attempts including the initial provider call. */
    public val maximumAttempts: Int = 9,
    public val initialDelay: Duration = Duration.ofSeconds(1),
    public val multiplier: Double = 2.0,
    public val maximumDelay: Duration = Duration.ofSeconds(8),
    /** Random variation from zero to one, where 0.2 means plus or minus 20 percent. */
    public val jitterRatio: Double = 0.2,
) {
    init {
        require(maximumAttempts >= 1) { "maximumAttempts must be at least 1." }
        require(!initialDelay.isNegative) { "initialDelay must be nonnegative." }
        require(multiplier.isFinite() && multiplier >= 1) { "multiplier must be finite and at least 1." }
        require(!maximumDelay.isNegative) { "maximumDelay must be nonnegative." }
        require(jitterRatio.isFinite() && jitterRatio in 0.0..1.0) {
            "jitterRatio must be between 0 and 1."
        }
    }

    internal fun delayAfterFailedAttempt(failedAttempt: Int, unitRandom: Double): Duration {
        val baseMillis = min(
            maximumDelay.toMillis().toDouble(),
            initialDelay.toMillis() * multiplier.pow((failedAttempt - 1).coerceAtLeast(0)),
        )
        val variation = baseMillis * jitterRatio
        val jittered = baseMillis - variation + (2 * variation * unitRandom.coerceIn(0.0, 1.0))
        return Duration.ofMillis(min(maximumDelay.toMillis().toDouble(), jittered).coerceAtLeast(0.0).roundToLong())
    }

    public companion object {
        @JvmField
        public val NONE: JanuaryTokenRetryPolicy = JanuaryTokenRetryPolicy(maximumAttempts = 1)
    }
}
