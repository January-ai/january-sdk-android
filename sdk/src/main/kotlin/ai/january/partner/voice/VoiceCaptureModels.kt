package ai.january.partner.voice

/** Lifecycle state for [VoiceCaptureSession]. */
public enum class VoiceCaptureState {
    /** No recognition is active. */
    IDLE,

    /** The microphone is listening for speech. */
    LISTENING,

    /** Listening has ended and the recognizer is producing a final result. */
    PROCESSING,
}

/** A completed Android voice capture. */
public data class VoiceCaptureResult(
    /** Final text returned by Android speech recognition. */
    public val transcript: String,
    /** Elapsed listening time in milliseconds. */
    public val durationMillis: Long,
)

/** Stable categories for voice-capture failures. */
public enum class VoiceCaptureErrorCode {
    PERMISSION_DENIED,
    RECOGNIZER_UNAVAILABLE,
    RECOGNIZER_BUSY,
    AUDIO,
    NETWORK,
    NO_MATCH,
    INVALID_STATE,
    UNKNOWN,
}

/** An error returned while starting or completing voice capture. */
public class VoiceCaptureException(
    public val code: VoiceCaptureErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
