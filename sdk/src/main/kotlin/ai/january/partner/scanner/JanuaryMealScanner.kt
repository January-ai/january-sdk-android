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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import ai.january.partner.JanuaryPartnerClient
import ai.january.partner.JanuaryPartnerUserClient
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
public fun JanuaryFoodScanner(
    userClient: JanuaryPartnerUserClient,
    modifier: Modifier = Modifier,
    configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
    onResult: (JanuaryFoodScannerResult) -> Unit,
    onCancel: () -> Unit,
) {
    JanuaryFoodScanner(
        client = userClient.client,
        modifier = modifier,
        endUserId = userClient.context.endUserId,
        configuration = configuration,
        onResult = onResult,
        onCancel = onCancel,
    )
}

/** Full-screen scanner for integrations that have not adopted a user-scoped client. */
@Composable
public fun JanuaryFoodScanner(
    client: JanuaryPartnerClient,
    modifier: Modifier = Modifier,
    endUserId: PartnerUserId? = null,
    configuration: JanuaryFoodScannerConfiguration = JanuaryFoodScannerConfiguration(),
    onResult: (JanuaryFoodScannerResult) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    ScannerSystemBars()
    val scope = rememberCoroutineScope()
    val controller = remember(client, endUserId, configuration) {
        JanuaryFoodScannerController(client, endUserId, configuration)
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
    var cameraAttempt by remember { mutableStateOf(0) }

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
                active = processingLabel == null && error == null,
                cameraAttempt = cameraAttempt,
                onReady = { capture, boundCamera ->
                    imageCapture = capture
                    camera = boundCamera
                    if (!boundCamera.cameraInfo.hasFlashUnit()) torchEnabled = false
                },
                onStopped = {
                    imageCapture = null
                    camera = null
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
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScannerCircleButton("Close scanner", processingLabel == null, onCancel)
                Spacer(Modifier.weight(1f))
                if (mode == JanuaryFoodScannerMode.PHOTO && hasPermission && camera?.cameraInfo?.hasFlashUnit() == true) {
                    ScannerCircleButton("Toggle torch", processingLabel == null, { torchEnabled = !torchEnabled }, torch = true, torchOn = torchEnabled)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (mode == JanuaryFoodScannerMode.PHOTO) "Fit the whole meal in the frame" else "Hold a food barcode in view",
                    color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                if (configuration.enabledModes.size > 1) {
                    ScannerModeControl(mode, configuration.enabledModes, enabled = processingLabel == null) { selected ->
                        mode = selected
                        if (selected == JanuaryFoodScannerMode.BARCODE) torchEnabled = false
                        error = null
                    }
                }
                Spacer(Modifier.height(24.dp))
                if (mode == JanuaryFoodScannerMode.PHOTO) {
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
                        enabled = imageCapture != null && processingLabel == null && error == null,
                        modifier = Modifier.size(76.dp).semantics { contentDescription = "Take meal photo" },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    ) {}
                } else {
                    Spacer(Modifier.size(76.dp))
                }

            }
        }

        processingLabel?.let { label ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = .72f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(30.dp))
                Text(label, color = Color.White)
            }
        }
        error?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onCancel,
                title = { Text(if (mode == JanuaryFoodScannerMode.PHOTO) "Meal scan failed" else "Barcode lookup failed") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { error = null; cameraAttempt++ }) { Text("Try Again") } },
                dismissButton = { TextButton(onClick = onCancel) { Text("Close") } },
            )
        }

    }
}

@Composable
private fun ScannerModeControl(
    selected: JanuaryFoodScannerMode,
    modes: Set<JanuaryFoodScannerMode>,
    enabled: Boolean,
    onSelect: (JanuaryFoodScannerMode) -> Unit,
) {
    Row(
        modifier = Modifier.width(220.dp).height(40.dp).background(Color.Black.copy(alpha = .55f), RoundedCornerShape(10.dp)).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        JanuaryFoodScannerMode.entries.filter { it in modes }.forEach { mode ->
            val active = selected == mode
            Button(
                onClick = { onSelect(mode) },
                enabled = enabled,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Color.White else Color.Transparent,
                    contentColor = if (active) Color.Black else Color.White,
                ),
            ) { Text(if (mode == JanuaryFoodScannerMode.PHOTO) "Photo" else "Barcode", fontSize = 14.sp) }
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
private fun ScannerCircleButton(label: String, enabled: Boolean, onClick: () -> Unit, torch: Boolean = false, torchOn: Boolean = false) {
    androidx.compose.material3.IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape).semantics { contentDescription = label }) {
        androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
            if (!torch) {
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .8f, size.height * .8f), 2.dp.toPx())
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width * .8f, size.height * .2f), androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .8f), 2.dp.toPx())
            } else {
                val bolt = androidx.compose.ui.graphics.Path().apply { moveTo(size.width * .6f, 0f); lineTo(size.width * .15f, size.height * .55f); lineTo(size.width * .48f, size.height * .55f); lineTo(size.width * .4f, size.height); lineTo(size.width * .85f, size.height * .4f); lineTo(size.width * .55f, size.height * .4f); close() }
                drawPath(bolt, Color.White)
                if (!torchOn) drawLine(Color.White, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), 2.dp.toPx())
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraXPreview(
    mode: JanuaryFoodScannerMode,
    active: Boolean,
    cameraAttempt: Int,
    onReady: (ImageCapture, Camera) -> Unit,
    onStopped: () -> Unit,
    onBarcode: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val currentActive by rememberUpdatedState(active)
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnStopped by rememberUpdatedState(onStopped)
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnFailure by rememberUpdatedState(onFailure)

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    // Processing pauses input; it must not unbind an in-flight photo capture.
    DisposableEffect(lifecycleOwner, mode, cameraAttempt, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        var barcodeScanner: BarcodeScanner? = null
        var analysis: ImageAnalysis? = null
        var ownedUseCases = emptyList<androidx.camera.core.UseCase>()
        var disposed = false
        var analysisInFlight = false
        val detected = AtomicBoolean(false)
        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            runCatching {
                provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                val useCases = mutableListOf<androidx.camera.core.UseCase>(preview, capture)
                if (mode == JanuaryFoodScannerMode.BARCODE) {
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
                    val barcodeAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis = barcodeAnalysis
                    barcodeAnalysis.setAnalyzer(executor) { proxy ->
                        val mediaImage = proxy.image
                        if (disposed || !currentActive || mediaImage == null || detected.get()) {
                            proxy.close()
                        } else {
                            analysisInFlight = true
                            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                                    if (!disposed && currentActive && value != null && detected.compareAndSet(false, true)) {
                                        currentOnBarcode(value)
                                    }
                                }
                                .addOnFailureListener {
                                    if (!disposed && currentActive) currentOnFailure(it)
                                }
                                .addOnCompleteListener {
                                    proxy.close()
                                    analysisInFlight = false
                                    if (disposed) scanner.close()
                                }
                        }
                    }
                    useCases += barcodeAnalysis
                }
                ownedUseCases = useCases
                val boundCamera = provider!!.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray(),
                )
                currentOnReady(capture, boundCamera)
            }.onFailure { if (!disposed) currentOnFailure(it) }
        }, executor)

        onDispose {
            disposed = true
            analysis?.clearAnalyzer()
            provider?.unbind(*ownedUseCases.toTypedArray())
            if (!analysisInFlight) barcodeScanner?.close()
            currentOnStopped()
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

/** Keep camera chrome readable, then restore the embedding app's system-bar style. */
@Composable
private fun ScannerSystemBars() {
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(view) {
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        val window = dialogWindow ?: activity?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (lightStatus != null) controller.isAppearanceLightStatusBars = lightStatus
            if (lightNavigation != null) controller.isAppearanceLightNavigationBars = lightNavigation
        }
    }
}
