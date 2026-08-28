package ai.january.partner.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ai.january.partner.foods.FoodSearchItem
import ai.january.partner.models.CompleteScanNutritionFacts
import ai.january.partner.models.NutrientAmount
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.FoodScan
import ai.january.partner.photos.PhotoScanGlucoseImpact
import ai.january.partner.photos.PhotoScanImage
import ai.january.partner.photos.ScanFoodPhotoRequest
import ai.january.partner.scanner.JanuaryFoodScanner
import ai.january.partner.scanner.JanuaryFoodScannerConfiguration
import ai.january.partner.scanner.JanuaryFoodScannerMode
import ai.january.partner.scanner.JanuaryFoodScannerResult
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private const val SAMPLE_ASSET = "fixtures/photo-scanning/burger-and-fries.png"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageInput by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showUrlSheet by remember { mutableStateOf(false) }
    var showCorrection by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<FoodScan?>(null) }
    var barcodeResult by remember { mutableStateOf<JanuaryFoodScannerResult.Barcode?>(null) }
    var showCameraScanner by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun selectPreparedJpeg(jpeg: ByteArray) {
        imageBytes = jpeg
        imageInput = "data:image/jpeg;base64,${Base64.encodeToString(jpeg, Base64.NO_WRAP)}"
        result = null
        error = null
    }

    fun selectBytes(bytes: ByteArray) {
        runCatching { PhotoScanImage.jpegData(bytes) }
            .onSuccess(::selectPreparedJpeg)
            .onFailure { error = "The selected image could not be prepared for upload." }
    }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { selectBytes(it.readBytes()) }
    }
    fun analyze() {
        if (imageInput.isBlank() || client == null) return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching { client.foodAnalysis.analyzePhoto(ScanFoodPhotoRequest(imageInput, state.partnerUserId)) }
                .onSuccess { result = it }
                .onFailure { error = it.message ?: "The scan failed." }
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppScreenHeader("Scan a meal", settingsAction)
                Text(
                    "Add a clear photo of the whole meal. January will identify foods, servings, nutrition, and an estimated glucose response.",
                    color = JanuaryColors.Muted,
                )
                if (imageInput.isBlank()) {
                    ScanPhotoInstructions()
                } else {
                    MealPreview(imageBytes, imageInput)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DemoSecondaryButton(
                            text = "Change",
                            onClick = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                        )
                        DemoOutlinedButton(
                            text = "Remove",
                            onClick = { imageInput = ""; imageBytes = null; result = null; error = null },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                DemoPrimaryButton(
                    text = "Take photo",
                    onClick = { showCameraScanner = true },
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Outlined.CameraAlt, null) },
                )
                DemoSecondaryButton(
                    text = "Choose from library",
                    onClick = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Outlined.Image, null) },
                )
                SectionLabel("Other ways")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DemoOutlinedButton(
                        text = "Sample meal",
                        onClick = {
                            runCatching { context.assets.open(SAMPLE_ASSET).use { it.readBytes() } }
                                .onSuccess(::selectBytes)
                                .onFailure { error = "The sample meal could not be loaded." }
                        },
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Outlined.Restaurant, null) },
                    )
                    DemoOutlinedButton(
                        text = "Image URL",
                        onClick = { showUrlSheet = true },
                        modifier = Modifier.weight(1f),
                        icon = { Icon(Icons.Outlined.Link, null) },
                    )
                }
                if (imageInput.isNotBlank()) {
                    DemoPrimaryButton(
                        text = if (loading) "Analyzing this meal…" else "Analyze meal",
                        onClick = ::analyze,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = client != null,
                        loading = loading,
                    )
                }
                Text(
                    if (imageInput.isBlank()) "Analyze appears once a photo is added. Barcode scanning is available inside the camera."
                    else if (loading) "Complex meals can take a little longer."
                    else "Your analysis opens as a modal result and can be corrected before you continue.",
                    modifier = Modifier.fillMaxWidth(),
                    color = JanuaryColors.Muted,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                if (client == null) ApiKeyRequiredCard()
                error?.let { ErrorCard(it, ::analyze) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showUrlSheet) {
        ImageUrlSheet(
            initialValue = imageInput.takeIf { it.startsWith("http") }.orEmpty(),
            onDismiss = { showUrlSheet = false },
            onUse = { imageInput = it; imageBytes = null; result = null; error = null; showUrlSheet = false },
        )
    }
    if (showCameraScanner && client != null) {
        Dialog(
            onDismissRequest = { showCameraScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            JanuaryFoodScanner(
                client = client,
                endUserId = state.partnerUserId,
                configuration = JanuaryFoodScannerConfiguration(initialMode = JanuaryFoodScannerMode.PHOTO),
                onResult = { scannerResult ->
                    showCameraScanner = false
                    when (scannerResult) {
                        is JanuaryFoodScannerResult.Photo -> {
                            selectPreparedJpeg(scannerResult.image.jpegData)
                            result = scannerResult.analysis
                        }
                        is JanuaryFoodScannerResult.Barcode -> barcodeResult = scannerResult
                    }
                },
                onCancel = { showCameraScanner = false },
            )
        }
    }
    result?.takeUnless { showCorrection }?.let { analysis ->
        ModalBottomSheet(
            onDismissRequest = { result = null },
            containerColor = JanuaryColors.Paper,
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppModalHeader(title = "Meal analysis", onDismiss = { result = null })
                ScanResult(analysis, imageBytes, imageInput)
                DemoPrimaryButton(
                    "Correct result",
                    { showCorrection = true },
                    Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Outlined.Edit, null) },
                )
            }
        }
    }
    barcodeResult?.let { barcode ->
        BarcodeResultSheet(barcode.value, barcode.food) { barcodeResult = null }
    }
    if (showCorrection && result != null && client != null) {
        CorrectionSheet(
            initial = result!!,
            onDismiss = { showCorrection = false },
            onSubmit = { mealName, instruction, finished ->
                coroutineScope.launch {
                    runCatching {
                        client.foodAnalysis.correct(
                            CorrectPhotoScanRequest(mealName, result!!.detections.orEmpty(), instruction, state.partnerUserId),
                        )
                    }.onSuccess { corrected -> result = corrected; showCorrection = false }
                        .onFailure { correctionError -> finished(correctionError.message ?: "The correction failed.") }
                }
            },
        )
    }
}
