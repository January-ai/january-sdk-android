# Voice capture

`VoiceCaptureSession` uses Android's speech recognizer to turn microphone input
into text. It does not call January's APIs and does not require a
`JanuaryPartnerClient` or authentication.

## Permission

The SDK manifest declares `android.permission.RECORD_AUDIO`. The host app must
still request that dangerous permission at runtime before calling
`startListening()`:

```kotlin
val permission = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
) { granted ->
    if (granted) voiceCapture.startListening()
}

if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
) {
    voiceCapture.startListening()
} else {
    permission.launch(Manifest.permission.RECORD_AUDIO)
}
```

Request access only after the user taps a microphone control. Explain the value
of voice input before sending users to system settings after a denial.

## Observe the session

Create one session per voice-enabled input and release it with the screen or
composition owner:

```kotlin
val voiceCapture = remember { VoiceCaptureSession(context) }
val state by voiceCapture.state.collectAsState()
val level by voiceCapture.audioLevel.collectAsState()
val partialText by voiceCapture.partialTranscript.collectAsState()
val result by voiceCapture.latestResult.collectAsState()

DisposableEffect(voiceCapture) {
    onDispose { voiceCapture.close() }
}

LaunchedEffect(result) {
    result?.let {
        query += if (query.isBlank()) it.transcript else " ${it.transcript}"
        voiceCapture.clearResult()
    }
}
```

The lifecycle is `IDLE` → `LISTENING` → `PROCESSING` → `IDLE`.
`audioLevel` is normalized from `0..1`, `partialTranscript` supports live text,
and `elapsedDurationMillis` can drive a recording timer. Call `stopListening()`
to finalize the transcript or `cancel()` to discard the active capture.

## Results and errors

`VoiceCaptureResult` contains the final transcript and listening duration.
Android's `SpeechRecognizer` owns its microphone stream and does not expose a
captured audio file through this SDK.

Permission denial, missing recognizer support, recognizer contention, audio,
network, no-match, invalid-state, and unknown failures use stable
`VoiceCaptureErrorCode` values. Observe `error` and call `clearError()` after the
app presents it.
