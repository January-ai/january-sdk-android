package ai.january.partner.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import ai.january.partner.voice.VoiceCaptureErrorCode
import ai.january.partner.voice.VoiceCaptureException
import ai.january.partner.voice.VoiceCaptureSession
import ai.january.partner.voice.VoiceCaptureState
import kotlinx.coroutines.delay

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    voiceCaptureEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val voiceCapture = remember { VoiceCaptureSession(context) }
    val voiceState by voiceCapture.state.collectAsState()
    val audioLevel by voiceCapture.audioLevel.collectAsState()
    val result by voiceCapture.latestResult.collectAsState()
    val captureError by voiceCapture.error.collectAsState()
    var visibleDuration by remember { mutableLongStateOf(0L) }
    var voiceError by remember { mutableStateOf<VoiceErrorPresentation?>(null) }

    fun startVoiceCapture() {
        runCatching { voiceCapture.startListening() }
            .onFailure { failure ->
                voiceError = VoiceErrorPresentation(
                    message = failure.message ?: "Voice input could not start.",
                    opensSettings = (failure as? VoiceCaptureException)?.code == VoiceCaptureErrorCode.PERMISSION_DENIED,
                )
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceCapture()
        else voiceError = VoiceErrorPresentation(
            message = "Microphone permission is required to use voice input.",
            opensSettings = true,
        )
    }

    DisposableEffect(voiceCapture) {
        onDispose { voiceCapture.close() }
    }
    LaunchedEffect(voiceCaptureEnabled) {
        if (!voiceCaptureEnabled) voiceCapture.cancel()
    }
    LaunchedEffect(voiceState) {
        if (voiceState == VoiceCaptureState.IDLE) {
            visibleDuration = 0L
            return@LaunchedEffect
        }
        while (voiceState == VoiceCaptureState.LISTENING) {
            visibleDuration = voiceCapture.elapsedDurationMillis
            delay(100)
        }
    }
    LaunchedEffect(result) {
        result?.let {
            onValueChange((if (value.isBlank()) "" else "$value ") + it.transcript)
            voiceCapture.clearResult()
        }
    }
    LaunchedEffect(captureError) {
        captureError?.let {
            voiceError = VoiceErrorPresentation(
                message = it.message ?: "Voice input failed.",
                opensSettings = it.code == VoiceCaptureErrorCode.PERMISSION_DENIED,
            )
            voiceCapture.clearError()
        }
    }

    if (voiceState == VoiceCaptureState.IDLE) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if ((onClear != null && value.isNotEmpty()) || voiceCaptureEnabled) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onClear != null && value.isNotEmpty()) {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                        if (voiceCaptureEnabled) {
                            IconButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        startVoiceCapture()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.semantics { contentDescription = "Use voice input" },
                            ) {
                                Icon(Icons.Outlined.Mic, contentDescription = null, tint = JanuaryColors.Green)
                            }
                        }
                    }
                }
            } else null,
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = JanuaryColors.Control,
                unfocusedContainerColor = JanuaryColors.Control,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
    } else {
        VoiceCaptureBar(
            state = voiceState,
            audioLevel = audioLevel,
            durationMillis = visibleDuration,
            onCancel = voiceCapture::cancel,
            onStop = {
                runCatching { voiceCapture.stopListening() }
                    .onFailure { failure ->
                        voiceError = VoiceErrorPresentation(
                            message = failure.message ?: "Voice input could not stop.",
                            opensSettings = (failure as? VoiceCaptureException)?.code == VoiceCaptureErrorCode.PERMISSION_DENIED,
                        )
                    }
            },
            modifier = modifier,
        )
    }

    voiceError?.let { presentation ->
        AlertDialog(
            onDismissRequest = { voiceError = null },
            title = { Text("Voice input unavailable") },
            text = { Text(presentation.message) },
            confirmButton = {
                if (presentation.opensSettings) {
                    TextButton(onClick = {
                        voiceError = null
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    }) { Text("Open settings") }
                } else {
                    TextButton(onClick = { voiceError = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (presentation.opensSettings) {
                    TextButton(onClick = { voiceError = null }) { Text("Cancel") }
                }
            },
        )
    }
}

private data class VoiceErrorPresentation(
    val message: String,
    val opensSettings: Boolean,
)

@Composable
private fun VoiceCaptureBar(
    state: VoiceCaptureState,
    audioLevel: Float,
    durationMillis: Long,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        color = JanuaryColors.Control,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.semantics { contentDescription = "Cancel voice input" }) {
                Icon(Icons.Outlined.Close, contentDescription = null, tint = JanuaryColors.Muted)
            }
            if (state == VoiceCaptureState.LISTENING) {
                VoiceLevelMeter(audioLevel, Modifier.weight(1f))
                Text(
                    "%02d:%02d".format(durationMillis / 60_000, (durationMillis / 1_000) % 60),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                IconButton(onClick = onStop, modifier = Modifier.semantics { contentDescription = "Stop and transcribe" }) {
                    Surface(color = JanuaryColors.Green, shape = androidx.compose.foundation.shape.CircleShape) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Stop, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = JanuaryColors.Green, strokeWidth = 2.dp)
                        Text("Transcribing…", color = JanuaryColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceLevelMeter(level: Float, modifier: Modifier = Modifier) {
    val weights = listOf(0.45f, 0.8f, 0.6f, 1f, 0.7f, 0.9f, 0.5f, 0.75f)
    Row(modifier.height(30.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        weights.forEach { weight ->
            Surface(
                modifier = Modifier.size(width = 3.dp, height = (4 + 24 * level * weight).dp),
                color = JanuaryColors.Green,
                shape = androidx.compose.foundation.shape.CircleShape,
            ) {}
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFieldPreview() {
    JanuaryDemoTheme { SearchField("", {}, "Search foods", {}) }
}
