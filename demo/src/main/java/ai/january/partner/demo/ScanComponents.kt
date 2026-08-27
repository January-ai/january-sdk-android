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
import ai.january.partner.scanner.JanuaryMealScanner
import ai.january.partner.scanner.JanuaryMealScannerConfiguration
import ai.january.partner.scanner.JanuaryMealScannerMode
import ai.january.partner.scanner.JanuaryMealScannerResult
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private const val SAMPLE_ASSET = "fixtures/photo-scanning/burger-and-fries.png"

@Composable
internal fun MealPreview(imageBytes: ByteArray?, imageInput: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(28.dp))
            .background(JanuaryColors.Control),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = remember(imageBytes) { imageBytes?.let(::decodeImage) }
        when {
            bitmap != null -> Image(bitmap.asImageBitmap(), "Selected meal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            imageInput.startsWith("http") -> AsyncImage(imageInput, "Selected meal", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else -> Unit
        }
    }
}

@Composable
internal fun ScanPhotoInstructions() {
    DemoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.CameraAlt, null, tint = JanuaryColors.Green)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Start with the whole meal", style = MaterialTheme.typography.titleMedium)
                Text("Use good light and keep every food visible.", color = JanuaryColors.Muted)
            }
        }
    }
}

@Composable
internal fun ScanResult(result: FoodScan, imageBytes: ByteArray?, imageInput: String) {
    MealPreview(imageBytes, imageInput)
    Text(result.mealName ?: "Meal analysis", style = MaterialTheme.typography.displaySmall)
    result.totalNutrients?.let { nutrients ->
        DemoCard { ScanMacroStrip(nutrients) }
        val rows = scanNutritionRows(nutrients)
        if (rows.isNotEmpty()) {
            DemoCard {
                NutritionList(rows.map { (label, value) -> NutritionValue(label, "${formatScanNumber(value.value)} ${value.unit}") })
            }
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
                        "${it.replaceFirstChar(Char::uppercase)} confidence",
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
internal fun ScanMacroStrip(nutrients: CompleteScanNutritionFacts) {
    MacroGrid(
        listOf(
            MacroValue("Calories", nutrients.calories?.value?.let(::formatScanNumber) ?: "—", "cal"),
            MacroValue("Protein", nutrients.protein?.value?.let(::formatScanNumber) ?: "—", "g"),
            MacroValue("Carbs", nutrients.carbohydrates?.value?.let(::formatScanNumber) ?: "—", "g"),
            MacroValue("Fat", nutrients.totalFat?.value?.let(::formatScanNumber) ?: "—", "g"),
        ),
    )
}

internal fun scanNutritionRows(value: CompleteScanNutritionFacts): List<Pair<String, NutrientAmount>> = listOfNotNull(
    value.netCarbohydrates?.let { "Net carbohydrates" to it },
    value.saturatedFat?.let { "Saturated fat" to it },
    value.fiber?.let { "Fiber" to it },
    value.totalSugars?.let { "Total sugars" to it },
    value.addedSugars?.let { "Added sugars" to it },
    value.sodium?.let { "Sodium" to it },
)

@Composable
internal fun PhotoGlucoseChart(impact: PhotoScanGlucoseImpact) {
    val points = impact.prediction.map { PredictionPoint(it.minutes.toDouble(), it.value) }
    PredictionChart(
        points = points,
        minimum = points.minOfOrNull { it.value } ?: 0.0,
        maximum = points.maxOfOrNull { it.value } ?: 1.0,
        showTargetBand = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageUrlSheet(initialValue: String, onDismiss: () -> Unit, onUse: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JanuaryColors.Paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppModalHeader(
                title = "Image URL",
                onDismiss = onDismiss,
                action = {
                    TextButton(onClick = { onUse(value.trim()) }, enabled = value.trim().startsWith("http")) {
                        Text("Use URL")
                    }
                },
            )
            OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), label = { Text("Public image URL") }, singleLine = true)
            Text("Paste a publicly accessible HTTPS image URL.", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CorrectionSheet(
    initial: FoodScan,
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
            AppModalHeader(title = "Correct result", onDismiss = onDismiss)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarcodeResultSheet(value: String, food: FoodSearchItem, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = JanuaryColors.Paper) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppModalHeader(title = "Barcode match", onDismiss = onDismiss)
            SectionLabel("Barcode $value")
            DemoCard {
                Text(food.name, style = MaterialTheme.typography.titleLarge)
                food.brandName?.let { Text(it, color = JanuaryColors.Muted) }
                Text("${food.servings.size} serving option${if (food.servings.size == 1) "" else "s"}", color = JanuaryColors.Muted)
            }
        }
    }
}

internal fun decodeImage(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
internal fun formatScanNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
