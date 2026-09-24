# Voice capture

`VoiceCaptureSession` uses Android's speech recognizer to turn microphone input
into text. It doesn't call January and needs no client or token. To get foods
from the text, send the transcript to
`user.foodAnalysis.analyzeDescription(SearchFoodsByNaturalLanguageRequest(transcript))`
([Food analysis](photo-scanning.md#analyze-a-description)).

## Add voice input to a field

The SDK's manifest declares `RECORD_AUDIO`, but your app must request the
runtime permission. Create one session per voice-enabled input, close it with
the composition, and call every session method on the main thread.
`startListening()` throws a `VoiceCaptureException` right away when the
permission is missing (`PERMISSION_DENIED`), the device has no speech
recognizer (`RECOGNIZER_UNAVAILABLE`, common on emulators), or a capture is
already running (`INVALID_STATE`). Failures after listening starts arrive on
the `error` flow instead.

```kotlin
import ai.january.partner.voice.VoiceCaptureErrorCode
import ai.january.partner.voice.VoiceCaptureException
import ai.january.partner.voice.VoiceCaptureSession
import ai.january.partner.voice.VoiceCaptureState
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun VoiceSearchField() {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var voiceError by remember { mutableStateOf<VoiceCaptureErrorCode?>(null) }

    val voiceCapture = remember { VoiceCaptureSession(context) }
    DisposableEffect(voiceCapture) { onDispose { voiceCapture.close() } }
    val state by voiceCapture.state.collectAsState()
    val result by voiceCapture.latestResult.collectAsState()
    val failure by voiceCapture.error.collectAsState()

    fun start() {
        voiceError = null
        try {
            voiceCapture.startListening()
        } catch (error: VoiceCaptureException) {
            voiceError = error.code
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else voiceError = VoiceCaptureErrorCode.PERMISSION_DENIED
    }

    LaunchedEffect(result) {
        result?.let {
            query = "$query ${it.transcript}".trim()
            voiceCapture.clearResult()
        }
    }
    LaunchedEffect(failure) {
        failure?.let {
            voiceError = it.code
            voiceCapture.clearError()
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        supportingText = { voiceError?.let { Text("Voice input failed: $it") } },
        trailingIcon = {
            IconButton(
                enabled = state != VoiceCaptureState.PROCESSING,
                onClick = {
                    when {
                        state == VoiceCaptureState.LISTENING -> voiceCapture.stopListening()
                        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED -> start()
                        else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            ) { Text(if (state == VoiceCaptureState.LISTENING) "Stop" else "Mic") }
        },
    )
}
```

Ask for the permission only after the user taps the microphone. After a
denial, explain why voice input needs it. If Android no longer shows the
prompt, offer to open the app's settings page
(`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).

## Session state

The lifecycle is `IDLE` → `LISTENING` → `PROCESSING` → `IDLE`. Call
`stopListening()` to end listening and get the final transcript, or `cancel()`
to discard the capture.

* `audioLevel` is a `StateFlow` from 0 to 1 for a level meter.
* `partialTranscript` is a `StateFlow` with live text while listening.
* `elapsedDurationMillis` is a plain property, not a flow; poll it (for example
  every 100 ms while `LISTENING`) to drive a timer.
* `latestResult` holds a `VoiceCaptureResult` with the final `transcript` and
  `durationMillis`. Call `clearResult()` once you've used it.
* `error` holds the latest `VoiceCaptureException`. Its `code` is one of
  `PERMISSION_DENIED`, `RECOGNIZER_UNAVAILABLE`, `RECOGNIZER_BUSY`, `AUDIO`,
  `NETWORK`, `NO_MATCH`, `INVALID_STATE`, or `UNKNOWN`. Call `clearError()` once
  you've shown it.

The constructor also takes `locale` (default `Locale.getDefault()`) and
`endOfSpeechSilence`, how long a pause ends the capture (default two seconds; a
hint that some recognizers ignore):

```kotlin
import java.time.Duration
import java.util.Locale

val voiceCapture = VoiceCaptureSession(
    context,
    locale = Locale.US,
    endOfSpeechSilence = Duration.ofSeconds(3),
)
```

Android's speech recognizer owns the microphone stream. The SDK doesn't keep
or expose recorded audio.
