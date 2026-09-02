package ai.january.partner

import ai.january.partner.voice.VoiceCaptureErrorCode
import ai.january.partner.voice.VoiceCaptureException
import ai.january.partner.voice.VoiceCaptureSession
import ai.january.partner.voice.VoiceCaptureState
import ai.january.partner.voice.VoiceRecognitionEngine
import ai.january.partner.voice.VoiceRecognitionListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCaptureSessionTest {
    @Test
    fun listeningStopAndFinalResultFollowThePublicLifecycle() {
        val engine = FakeVoiceRecognitionEngine()
        var now = 1_000L
        val session = VoiceCaptureSession(engine, { true }, { now })

        session.startListening()
        assertEquals(VoiceCaptureState.LISTENING, session.state.value)
        assertEquals(1, engine.startCount)

        engine.listener!!.onAudioLevel(4f)
        assertEquals(0.5f, session.audioLevel.value)
        engine.listener!!.onPartialTranscript("greek")
        assertEquals("greek", session.partialTranscript.value)

        now = 3_750L
        session.stopListening()
        assertEquals(VoiceCaptureState.PROCESSING, session.state.value)
        assertEquals(1, engine.stopCount)
        engine.listener!!.onFinalTranscript("  greek yogurt  ")

        assertEquals(VoiceCaptureState.IDLE, session.state.value)
        assertEquals("greek yogurt", session.latestResult.value!!.transcript)
        assertEquals(2_750L, session.latestResult.value!!.durationMillis)
    }

    @Test
    fun automaticEndOfSpeechMovesToProcessing() {
        val engine = FakeVoiceRecognitionEngine()
        val session = VoiceCaptureSession(engine, { true }, { 10L })

        session.startListening()
        engine.listener!!.onEndOfSpeech()

        assertEquals(VoiceCaptureState.PROCESSING, session.state.value)
        assertEquals(0f, session.audioLevel.value)
        engine.listener!!.onPartialTranscript("late transcript")
        assertEquals("", session.partialTranscript.value)
    }

    @Test
    fun stopFailureIsPublishedAndThrown() {
        val engine = FakeVoiceRecognitionEngine().apply {
            stopFailure = IllegalStateException("stop failed")
        }
        val session = VoiceCaptureSession(engine, { true }, { 0L })

        session.startListening()
        val error = assertThrows(VoiceCaptureException::class.java) { session.stopListening() }

        assertEquals(VoiceCaptureErrorCode.UNKNOWN, error.code)
        assertEquals(error, session.error.value)
        assertEquals(VoiceCaptureState.IDLE, session.state.value)
    }

    @Test
    fun permissionAndAvailabilityFailuresAreStable() {
        val denied = VoiceCaptureSession(FakeVoiceRecognitionEngine(), { false }, { 0L })
        val deniedError = assertThrows(VoiceCaptureException::class.java) { denied.startListening() }
        assertEquals(VoiceCaptureErrorCode.PERMISSION_DENIED, deniedError.code)

        val unavailableEngine = FakeVoiceRecognitionEngine().apply { isAvailable = false }
        val unavailable = VoiceCaptureSession(unavailableEngine, { true }, { 0L })
        val unavailableError = assertThrows(VoiceCaptureException::class.java) { unavailable.startListening() }
        assertEquals(VoiceCaptureErrorCode.RECOGNIZER_UNAVAILABLE, unavailableError.code)
    }

    @Test
    fun emptyTranscriptAndRecognizerErrorResetTheSession() {
        val engine = FakeVoiceRecognitionEngine()
        val session = VoiceCaptureSession(engine, { true }, { 0L })

        session.startListening()
        engine.listener!!.onFinalTranscript("   ")
        assertEquals(VoiceCaptureErrorCode.NO_MATCH, session.error.value!!.code)
        assertEquals(VoiceCaptureState.IDLE, session.state.value)

        session.clearError()
        session.startListening()
        engine.listener!!.onRecognitionError(
            VoiceCaptureException(VoiceCaptureErrorCode.NETWORK, "Network unavailable."),
        )
        assertEquals(VoiceCaptureErrorCode.NETWORK, session.error.value!!.code)
        assertEquals(VoiceCaptureState.IDLE, session.state.value)
    }

    @Test
    fun cancelAndCloseReleasePlatformRecognition() {
        val engine = FakeVoiceRecognitionEngine()
        val session = VoiceCaptureSession(engine, { true }, { 0L })

        session.startListening()
        session.cancel()
        assertEquals(1, engine.cancelCount)
        assertEquals(VoiceCaptureState.IDLE, session.state.value)
        assertNull(session.latestResult.value)

        session.close()
        assertEquals(1, engine.destroyCount)
    }

    @Test
    fun rmsNormalizationIsBounded() {
        assertEquals(0f, VoiceCaptureSession.normalizeRms(-20f))
        assertEquals(0.5f, VoiceCaptureSession.normalizeRms(4f))
        assertEquals(1f, VoiceCaptureSession.normalizeRms(20f))
        assertTrue(VoiceCaptureSession.normalizeRms(0f) in 0f..1f)
    }
}

private class FakeVoiceRecognitionEngine : VoiceRecognitionEngine {
    override var listener: VoiceRecognitionListener? = null
    override var isAvailable: Boolean = true
    var startCount = 0
    var stopCount = 0
    var cancelCount = 0
    var destroyCount = 0
    var stopFailure: Exception? = null

    override fun startListening() { startCount += 1 }
    override fun stopListening() {
        stopCount += 1
        stopFailure?.let { throw it }
    }
    override fun cancel() { cancelCount += 1 }
    override fun destroy() { destroyCount += 1 }
}
