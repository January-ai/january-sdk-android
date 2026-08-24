package ai.january.partner.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
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
import ai.january.partner.models.CompleteScanNutritionFacts
import ai.january.partner.models.NutrientAmount
import ai.january.partner.photos.CorrectPhotoScanRequest
import ai.january.partner.photos.PhotoScan
import ai.january.partner.photos.PhotoScanGlucoseImpact
import ai.january.partner.photos.ScanFoodPhotoRequest
import coil3.compose.AsyncImage
import java.io.ByteArrayOutputStream
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
    var result by remember { mutableStateOf<PhotoScan?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun selectBytes(bytes: ByteArray) {
        imageBytes = bytes
        imageInput = "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        result = null
        error = null
    }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { selectBytes(it.readBytes()) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        selectBytes(ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, stream)
            stream.toByteArray()
        })
    }

    fun analyze() {
        if (imageInput.isBlank() || client == null) return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching { client.photoScanning.scan(ScanFoodPhotoRequest(imageInput, state.partnerUserId)) }
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
                if (result == null) {
                    Text("Scan a meal", style = MaterialTheme.typography.displaySmall)
                    MealPreview(imageBytes, imageInput)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DemoPrimaryButton(
                            text = "Take photo",
                            onClick = { camera.launch(null) },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Outlined.CameraAlt, null) },
                        )
                        DemoSecondaryButton(
                            text = "Choose photo",
                            onClick = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Outlined.Image, null) },
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DemoOutlinedButton(
                            text = "Use sample meal",
                            onClick = {
                                runCatching { context.assets.open(SAMPLE_ASSET).use { it.readBytes() } }
                                    .onSuccess(::selectBytes)
                                    .onFailure { error = "The sample meal could not be loaded." }
                            },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Outlined.Restaurant, null) },
                        )
                        DemoOutlinedButton(
                            text = "Use image URL",
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
                        if (imageInput.isBlank()) "Analyze appears once a photo is added."
                        else if (loading) "Complex meals can take a little longer. You can leave this screen while the request completes."
                        else "January will identify the foods, servings, nutrition, and estimated glucose response.",
                        modifier = Modifier.fillMaxWidth(),
                        color = JanuaryColors.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    if (client == null) ApiKeyRequiredCard()
                    error?.let { ErrorCard(it, ::analyze) }
                } else {
                    ScanResult(result = result!!, imageBytes = imageBytes, imageInput = imageInput)
                    DemoPrimaryButton(
                        "Correct result",
                        { showCorrection = true },
                        Modifier.fillMaxWidth(),
                        icon = { Icon(Icons.Outlined.Edit, null) },
                    )
                    DemoSecondaryButton(
                        "Scan another meal",
                        { result = null; imageInput = ""; imageBytes = null; error = null },
                        Modifier.fillMaxWidth(),
                    )
                }
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
    if (showCorrection && result != null && client != null) {
        CorrectionSheet(
            initial = result!!,
            onDismiss = { showCorrection = false },
            onSubmit = { mealName, instruction, finished ->
                coroutineScope.launch {
                    runCatching {
                        client.photoScanning.correct(
                            CorrectPhotoScanRequest(mealName, result!!.detections.orEmpty(), instruction, state.partnerUserId),
                        )
                    }.onSuccess { corrected -> result = corrected; showCorrection = false }
                        .onFailure { correctionError -> finished(correctionError.message ?: "The correction failed.") }
                }
            },
        )
    }
}

@Composable
private fun MealPreview(imageBytes: ByteArray?, imageInput: String, height: Int = 340) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(28.dp))
            .background(JanuaryColors.Control),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = remember(imageBytes) { imageBytes?.let(::decodeImage) }
        when {
            bitmap != null -> Image(bitmap.asImageBitmap(), "Selected meal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            imageInput.startsWith("http") -> AsyncImage(imageInput, "Selected meal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> {
                Canvas(Modifier.fillMaxSize()) {
                    val stripe = JanuaryColors.ControlStrong.copy(alpha = .48f)
                    var offset = -size.height
                    while (offset <= size.width) {
                        drawLine(stripe, Offset(offset, size.height), Offset(offset + size.height, 0f), strokeWidth = 9.dp.toPx())
                        offset += 22.dp.toPx()
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(28.dp),
                ) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = JanuaryColors.Green)
                    Text("Add a clear photo of the whole meal", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        "January identifies the foods, servings, and nutrition — then estimates your response.",
                        color = JanuaryColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanResult(result: PhotoScan, imageBytes: ByteArray?, imageInput: String) {
    MealPreview(imageBytes, imageInput, height = 210)
    Text(result.mealName ?: "Meal analysis", style = MaterialTheme.typography.displaySmall)
    result.totalNutrients?.let { nutrients ->
        DemoCard { ScanMacroStrip(nutrients) }
        val rows = scanNutritionRows(nutrients)
        if (rows.isNotEmpty()) {
            DemoCard { rows.forEach { (label, value) -> ScanNutritionRow(label, value) } }
        }
    }
    SectionLabel("Detected foods")
    result.detections.orEmpty().forEach { detection ->
        DemoCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(detection.food.name, style = MaterialTheme.typography.titleMedium)
                    detection.food.brandName?.takeIf(String::isNotBlank)?.let { Text(it, color = JanuaryColors.Muted) }
                }
                detection.confidenceScore?.let {
                    Text(
                        it.replaceFirstChar(Char::uppercase),
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(JanuaryColors.Green.copy(alpha = .13f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = JanuaryColors.Green,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            ScanMacroStrip(detection.food.nutrients)
        }
    }
    result.glucoseImpact?.let { impact ->
        SectionLabel("Estimated glucose response")
        DemoCard {
            Text(
                "${impact.impactScore.replaceFirstChar(Char::uppercase)} impact",
                color = if (impact.impactScore.lowercase() == "low") JanuaryColors.Green else JanuaryColors.Rust,
                fontWeight = FontWeight.Bold,
            )
            PhotoGlucoseChart(impact)
        }
    }
}

@Composable
private fun ScanMacroStrip(nutrients: CompleteScanNutritionFacts) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ScanMetric("Calories", nutrients.calories?.value, "cal")
        ScanMetric("Protein", nutrients.protein?.value, "g")
        ScanMetric("Carbs", nutrients.carbohydrates?.value, "g")
        ScanMetric("Fat", nutrients.totalFat?.value, "g")
    }
}

@Composable
private fun ScanMetric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let(::formatScanNumber) ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        Text(unit, style = MaterialTheme.typography.bodySmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ScanNutritionRow(label: String, value: NutrientAmount) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text("${formatScanNumber(value.value)} ${value.unit}", fontFamily = FontFamily.Monospace)
    }
}

private fun scanNutritionRows(value: CompleteScanNutritionFacts): List<Pair<String, NutrientAmount>> = listOfNotNull(
    value.netCarbohydrates?.let { "Net carbohydrates" to it },
    value.saturatedFat?.let { "Saturated fat" to it },
    value.fiber?.let { "Fiber" to it },
    value.totalSugars?.let { "Total sugars" to it },
    value.addedSugars?.let { "Added sugars" to it },
    value.sodium?.let { "Sodium" to it },
)

@Composable
private fun PhotoGlucoseChart(impact: PhotoScanGlucoseImpact) {
    Canvas(Modifier.fillMaxWidth().height(180.dp).padding(top = 12.dp)) {
        val points = impact.prediction
        if (points.isEmpty()) return@Canvas
        val minX = points.minOf { it.minutes }
        val maxX = points.maxOf { it.minutes }.coerceAtLeast(minX + 1)
        val minY = points.minOf { it.value }
        val maxY = points.maxOf { it.value }.coerceAtLeast(minY + 1)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = ((point.minutes - minX) / (maxX - minX) * size.width).toFloat()
            val y = (size.height - ((point.value - minY) / (maxY - minY) * size.height)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, JanuaryColors.Rust, style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { point ->
            val x = ((point.minutes - minX) / (maxX - minX) * size.width).toFloat()
            val y = (size.height - ((point.value - minY) / (maxY - minY) * size.height)).toFloat()
            drawCircle(JanuaryColors.Rust, 3.dp.toPx(), Offset(x, y))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageUrlSheet(initialValue: String, onDismiss: () -> Unit, onUse: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JanuaryColors.Paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Image URL", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                TextButton(onClick = { onUse(value.trim()) }, enabled = value.trim().startsWith("http")) { Text("Use URL") }
            }
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text("Public image URL") }, singleLine = true)
            Text("Paste a publicly accessible HTTPS image URL.", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CorrectionSheet(
    initial: PhotoScan,
    onDismiss: () -> Unit,
    onSubmit: (String, String, (String) -> Unit) -> Unit,
) {
    var mealName by remember { mutableStateOf(initial.mealName.orEmpty()) }
    var instruction by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = JanuaryColors.Paper) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text("Correct result", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.weight(.2f))
            }
            OutlinedTextField(mealName, { mealName = it }, Modifier.fillMaxWidth(), label = { Text("Meal name") })
            SectionLabel("Current detections")
            initial.detections.orEmpty().forEach { Text("• ${it.food.name}") }
            OutlinedTextField(
                instruction,
                { instruction = it },
                Modifier.fillMaxWidth().height(140.dp),
                label = { Text("What should change?") },
                supportingText = { Text("For example: remove the fries and add a side salad.") },
            )
            error?.let { ErrorCard(it) }
            DemoPrimaryButton(
                "Submit correction",
                {
                    submitting = true
                    onSubmit(mealName.trim().ifEmpty { "Meal" }, instruction.trim()) { message ->
                        error = message
                        submitting = false
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = instruction.isNotBlank(),
                loading = submitting,
            )
        }
    }
}

private fun decodeImage(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
private fun formatScanNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
