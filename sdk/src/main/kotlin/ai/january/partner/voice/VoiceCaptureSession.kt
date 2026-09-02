package ai.january.partner.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captures microphone speech and converts it to text with Android speech recognition.
 *
 * Keep one session for each voice-enabled input. Observe [state], [audioLevel],
 * [partialTranscript], [latestResult], and [error] to render app-owned UI. Call
 * [close] when the input leaves composition or its owner is destroyed.
 *
 * The SDK manifest declares `android.permission.RECORD_AUDIO`. The host app must
 * request runtime permission before calling [startListening].
 */
public class VoiceCaptureSession private constructor(
    private val engine: VoiceRecognitionEngine,
    private val hasRecordPermission: () -> Boolean,
    private val elapsedRealtime: () -> Long,
    private val checkThread: () -> Unit,
) : AutoCloseable, VoiceRecognitionListener {
    private val mutableState = MutableStateFlow(VoiceCaptureState.IDLE)
    private val mutableAudioLevel = MutableStateFlow(0f)
    private val mutablePartialTranscript = MutableStateFlow("")
    private val mutableLatestResult = MutableStateFlow<VoiceCaptureResult?>(null)
    private val mutableError = MutableStateFlow<VoiceCaptureException?>(null)
    private var startedAtMillis: Long? = null

    /** Current voice-capture lifecycle state. */
    public val state: StateFlow<VoiceCaptureState> = mutableState.asStateFlow()

    /** Normalized microphone level from `0` (silent) to `1` (maximum). */
    public val audioLevel: StateFlow<Float> = mutableAudioLevel.asStateFlow()

    /** Best partial recognition text while listening. */
    public val partialTranscript: StateFlow<String> = mutablePartialTranscript.asStateFlow()

    /** Most recent completed result. Clear it with [clearResult] after consuming it. */
    public val latestResult: StateFlow<VoiceCaptureResult?> = mutableLatestResult.asStateFlow()

    /** Most recent failure. Clear it with [clearError] after presenting it. */
    public val error: StateFlow<VoiceCaptureException?> = mutableError.asStateFlow()

    /** Creates a session backed by the device's default speech recognizer. */
    public constructor(
        context: Context,
        locale: Locale = Locale.getDefault(),
    ) : this(
        engine = AndroidVoiceRecognitionEngine(context.applicationContext, locale),
        hasRecordPermission = {
            ContextCompat.checkSelfPermission(context.applicationContext, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        },
        elapsedRealtime = SystemClock::elapsedRealtime,
        checkThread = {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "VoiceCaptureSession must be used from the main thread."
            }
        },
    )

    internal constructor(
        engine: VoiceRecognitionEngine,
        hasRecordPermission: () -> Boolean,
        elapsedRealtime: () -> Long,
    ) : this(engine, hasRecordPermission, elapsedRealtime, {})

    init {
        engine.listener = this
    }

    /** Starts listening after the host app has obtained microphone permission. */
    public fun startListening() {
        checkThread()
        if (mutableState.value != VoiceCaptureState.IDLE) {
            throw VoiceCaptureException(
                VoiceCaptureErrorCode.INVALID_STATE,
                "Voice capture is already active.",
            )
        }
        if (!hasRecordPermission()) {
            throw VoiceCaptureException(
                VoiceCaptureErrorCode.PERMISSION_DENIED,
                "Microphone permission is required to capture voice input.",
            )
        }
        if (!engine.isAvailable) {
            throw VoiceCaptureException(
                VoiceCaptureErrorCode.RECOGNIZER_UNAVAILABLE,
                "Speech recognition is not available on this device.",
            )
        }

        mutableLatestResult.value = null
        mutableError.value = null
        mutablePartialTranscript.value = ""
        mutableAudioLevel.value = 0f
        startedAtMillis = elapsedRealtime()
        mutableState.value = VoiceCaptureState.LISTENING

        try {
            engine.startListening()
        } catch (failure: Exception) {
            resetActiveState()
            val error = failure as? VoiceCaptureException ?: VoiceCaptureException(
                VoiceCaptureErrorCode.UNKNOWN,
                "Voice capture could not start.",
                failure,
            )
            mutableError.value = error
            throw error
        }
    }

    /** Stops microphone input and asks the recognizer for its final transcript. */
    public fun stopListening() {
        checkThread()
        if (mutableState.value != VoiceCaptureState.LISTENING) {
            throw VoiceCaptureException(
                VoiceCaptureErrorCode.INVALID_STATE,
                "Voice capture is not listening.",
            )
        }
        mutableState.value = VoiceCaptureState.PROCESSING
        mutableAudioLevel.value = 0f
        try {
            engine.stopListening()
        } catch (failure: Exception) {
            val error = VoiceCaptureException(
                VoiceCaptureErrorCode.UNKNOWN,
                "Voice capture could not stop cleanly.",
                failure,
            )
            completeWithError(error)
            throw error
        }
    }

    /** Cancels listening or result processing without publishing an error. */
    public fun cancel() {
        checkThread()
        if (mutableState.value == VoiceCaptureState.IDLE) return
        engine.cancel()
        resetActiveState()
    }

    /** Clears the last result after the host app consumes it. */
    public fun clearResult() {
        mutableLatestResult.value = null
    }

    /** Clears the last error after the host app presents it. */
    public fun clearError() {
        mutableError.value = null
    }

    /** Current elapsed listening time. */
    public val elapsedDurationMillis: Long
        get() = startedAtMillis?.let { (elapsedRealtime() - it).coerceAtLeast(0) } ?: 0

    override fun onAudioLevel(rmsDecibels: Float) {
        if (mutableState.value == VoiceCaptureState.LISTENING) {
            mutableAudioLevel.value = normalizeRms(rmsDecibels)
        }
    }

    override fun onPartialTranscript(transcript: String) {
        if (mutableState.value == VoiceCaptureState.LISTENING) {
            mutablePartialTranscript.value = transcript.trim()
        }
    }

    override fun onEndOfSpeech() {
        if (mutableState.value == VoiceCaptureState.LISTENING) {
            mutableState.value = VoiceCaptureState.PROCESSING
            mutableAudioLevel.value = 0f
        }
    }

    override fun onFinalTranscript(transcript: String) {
        if (mutableState.value == VoiceCaptureState.IDLE) return
        val normalized = transcript.trim()
        if (normalized.isEmpty()) {
            completeWithError(
                VoiceCaptureException(
                    VoiceCaptureErrorCode.NO_MATCH,
                    "No speech was recognized. Please try again.",
                ),
            )
            return
        }
        mutableLatestResult.value = VoiceCaptureResult(normalized, elapsedDurationMillis)
        resetActiveState()
    }

    override fun onRecognitionError(error: VoiceCaptureException) {
        if (mutableState.value != VoiceCaptureState.IDLE) completeWithError(error)
    }

    private fun completeWithError(error: VoiceCaptureException) {
        mutableError.value = error
        resetActiveState()
    }

    private fun resetActiveState() {
        startedAtMillis = null
        mutableState.value = VoiceCaptureState.IDLE
        mutableAudioLevel.value = 0f
        mutablePartialTranscript.value = ""
    }

    /** Cancels active work and releases the platform recognizer. */
    override fun close() {
        checkThread()
        if (mutableState.value != VoiceCaptureState.IDLE) {
            engine.cancel()
            resetActiveState()
        }
        engine.destroy()
    }

    internal companion object {
        internal fun normalizeRms(rmsDecibels: Float): Float =
            ((rmsDecibels + 2f) / 12f).coerceIn(0f, 1f)
    }
}

internal interface VoiceRecognitionListener {
    fun onAudioLevel(rmsDecibels: Float)
    fun onPartialTranscript(transcript: String)
    fun onEndOfSpeech()
    fun onFinalTranscript(transcript: String)
    fun onRecognitionError(error: VoiceCaptureException)
}

internal interface VoiceRecognitionEngine {
    var listener: VoiceRecognitionListener?
    val isAvailable: Boolean
    fun startListening()
    fun stopListening()
    fun cancel()
    fun destroy()
}
