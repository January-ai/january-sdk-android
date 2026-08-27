package ai.january.partner.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.PartnerUserId
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

/**
 * Full-screen CameraX scanner with Photo and Barcode modes.
 *
 * CameraX is bound to the current lifecycle, analysis drops stale frames, and
 * capture/lookup work is serialized while [onResult] is pending.
 */
@Composable
public fun JanuaryMealScanner(
    client: JanuaryPartnerClient,
    modifier: Modifier = Modifier,
    endUserId: PartnerUserId? = null,
    configuration: JanuaryMealScannerConfiguration = JanuaryMealScannerConfiguration(),
    onResult: (JanuaryMealScannerResult) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(client, endUserId, configuration) {
        JanuaryMealScannerController(client, endUserId, configuration)
    }
    var mode by rememberSaveable { mutableStateOf(configuration.initialMode) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var processingLabel by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = true
        hasPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraXPreview(
                mode = mode,
                active = processingLabel == null,
                onReady = { capture, boundCamera ->
                    imageCapture = capture
                    camera = boundCamera
                    if (!boundCamera.cameraInfo.hasFlashUnit()) torchEnabled = false
                },
                onBarcode = { barcode ->
                    if (processingLabel != null) return@CameraXPreview
                    processingLabel = "Looking up barcode…"
                    error = null
                    scope.launch {
                        runCatching { controller.lookupBarcode(barcode) }
                            .onSuccess(onResult)
                            .onFailure { error = scannerMessage(it, "Barcode lookup failed.") }
                        processingLabel = null
                    }
                },
                onFailure = { error = scannerMessage(it, "The camera could not start.") },
            )
        } else {
            CameraPermissionCard(
                showSettings = permissionRequested,
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onSettings = { openAppSettings(context) },
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
                Spacer(Modifier.weight(1f))
                if (hasPermission && camera?.cameraInfo?.hasFlashUnit() == true) {
                    TextButton(onClick = { torchEnabled = !torchEnabled }) {
                        Text(if (torchEnabled) "Torch on" else "Torch", color = Color.White)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (configuration.enabledModes.size > 1) {
                    ScannerModeControl(mode, configuration.enabledModes) { selected ->
                        mode = selected
                        error = null
                    }
                }
                Text(
                    if (mode == JanuaryMealScannerMode.PHOTO) "Frame the whole meal" else "Center the food barcode",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (mode == JanuaryMealScannerMode.PHOTO) {
                    Button(
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            if (processingLabel != null) return@Button
                            processingLabel = "Preparing photo…"
                            error = null
                            capturePhoto(context, capture) { result ->
                                result.onSuccess { bytes ->
                                    processingLabel = "Analyzing meal…"
                                    scope.launch {
                                        runCatching { controller.analyzePhoto(bytes) }
                                            .onSuccess(onResult)
                                            .onFailure { error = scannerMessage(it, "Meal scan failed.") }
                                        processingLabel = null
                                    }
                                }.onFailure {
                                    error = scannerMessage(it, "The photo could not be captured.")
                                    processingLabel = null
                                }
                            }
                        },
                        enabled = imageCapture != null && processingLabel == null,
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) { Text("●", fontSize = 30.sp, maxLines = 1) }
                }
                error?.let { ScannerErrorNotice(it) { error = null } }
            }
        }

        processingLabel?.let { label ->
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = .78f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
                Text(label, color = Color.White)
            }
        }
    }
}

@Composable
private fun ScannerModeControl(
    selected: JanuaryMealScannerMode,
    modes: Set<JanuaryMealScannerMode>,
    onSelect: (JanuaryMealScannerMode) -> Unit,
) {
    Row(
        modifier = Modifier.background(Color.Black.copy(alpha = .62f), RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        JanuaryMealScannerMode.entries.filter { it in modes }.forEach { mode ->
            val active = selected == mode
            Button(
                onClick = { onSelect(mode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Color.White else Color.Transparent,
                    contentColor = if (active) Color.Black else Color.White,
                ),
            ) { Text(if (mode == JanuaryMealScannerMode.PHOTO) "Photo" else "Barcode") }
        }
    }
}

@Composable
private fun CameraPermissionCard(
    showSettings: Boolean,
    onRequest: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.background(Color(0xFF242424), RoundedCornerShape(22.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Camera access is required", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text(
            "Allow camera access to photograph a meal or scan a food barcode.",
            color = Color(0xFFD0D0D0),
        )
        if (showSettings) {
            Button(onClick = onSettings) { Text("Open Settings") }
            OutlinedButton(onClick = onRequest) { Text("Try again", color = Color.White) }
        } else {
            Button(onClick = onRequest) { Text("Continue") }
        }
    }
}

@Composable
private fun ScannerErrorNotice(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xDD3B1717), RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(message, color = Color.White)
        TextButton(onClick = onRetry) { Text("Try again", color = Color.White) }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraXPreview(
    mode: JanuaryMealScannerMode,
    active: Boolean,
    onReady: (ImageCapture, Camera) -> Unit,
    onBarcode: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner, mode, active, previewView) {
        if (!active) return@DisposableEffect onDispose { }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        var barcodeScanner: BarcodeScanner? = null
        val detected = AtomicBoolean(false)
        cameraProviderFuture.addListener({
            runCatching {
                provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview, capture)
                if (mode == JanuaryMealScannerMode.BARCODE) {
                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_ITF,
                    ).build()
                    val scanner = BarcodeScanning.getClient(options)
                    barcodeScanner = scanner
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor) { proxy ->
                        val mediaImage = proxy.image
                        if (mediaImage == null || detected.get()) {
                            proxy.close()
                        } else {
                            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                                    if (value != null && detected.compareAndSet(false, true)) onBarcode(value)
                                }
                                .addOnFailureListener(onFailure)
                                .addOnCompleteListener { proxy.close() }
                        }
                    }
                    useCases += analysis
                }
                provider?.unbindAll()
                val boundCamera = provider!!.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray(),
                )
                onReady(capture, boundCamera)
            }.onFailure(onFailure)
        }, executor)

        onDispose {
            barcodeScanner?.close()
            provider?.unbindAll()
        }
    }
}

private fun capturePhoto(context: Context, capture: ImageCapture, completion: (Result<ByteArray>) -> Unit) {
    val file = File.createTempFile("january-meal-", ".jpg", context.cacheDir)
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            completion(runCatching { file.readBytes() })
            file.delete()
        }

        override fun onError(exception: ImageCaptureException) {
            file.delete()
            completion(Result.failure(exception))
        }
    })
}

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun scannerMessage(error: Throwable, fallback: String): String =
    when {
        error.javaClass.name.contains("CameraUnavailable", ignoreCase = true) ||
            error.javaClass.name.contains("InitializationException", ignoreCase = true) ||
            error.message.orEmpty().contains("Available cameras: 0") ->
            "The camera is unavailable on this device. Close another camera app or try again."
        error is NoBarcodeMatchException -> "No January food matched this barcode. Try another angle or enter it manually."
        else -> error.message?.takeIf(String::isNotBlank) ?: fallback
    }
