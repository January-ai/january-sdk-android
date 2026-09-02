package ai.january.partner.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

internal class AndroidVoiceRecognitionEngine(
    private val context: Context,
    private val locale: Locale,
) : VoiceRecognitionEngine, RecognitionListener {
    override var listener: VoiceRecognitionListener? = null
    private var recognizer: SpeechRecognizer? = null

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    override fun startListening() {
        val activeRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(this)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        activeRecognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.stopListening()
    }

    override fun cancel() {
        recognizer?.cancel()
    }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
        listener = null
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) { listener?.onAudioLevel(rmsdB) }
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() { listener?.onEndOfSpeech() }

    override fun onError(error: Int) {
        listener?.onRecognitionError(error.toVoiceCaptureException())
    }

    override fun onResults(results: Bundle?) {
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        listener?.onFinalTranscript(transcript)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val transcript = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        listener?.onPartialTranscript(transcript)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

private fun Int.toVoiceCaptureException(): VoiceCaptureException {
    val code = when (this) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceCaptureErrorCode.PERMISSION_DENIED
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceCaptureErrorCode.RECOGNIZER_BUSY
        SpeechRecognizer.ERROR_AUDIO -> VoiceCaptureErrorCode.AUDIO
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> VoiceCaptureErrorCode.NETWORK
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceCaptureErrorCode.NO_MATCH
        else -> VoiceCaptureErrorCode.UNKNOWN
    }
    val message = when (code) {
        VoiceCaptureErrorCode.PERMISSION_DENIED -> "Microphone permission is required to capture voice input."
        VoiceCaptureErrorCode.RECOGNIZER_BUSY -> "Speech recognition is busy. Please try again."
        VoiceCaptureErrorCode.AUDIO -> "The microphone audio could not be captured."
        VoiceCaptureErrorCode.NETWORK -> "Speech recognition could not connect. Please try again."
        VoiceCaptureErrorCode.NO_MATCH -> "No speech was recognized. Please try again."
        else -> "Speech recognition failed. Please try again."
    }
    return VoiceCaptureException(code, message)
}
