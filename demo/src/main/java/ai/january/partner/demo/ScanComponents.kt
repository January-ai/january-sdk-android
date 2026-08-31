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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.DownloadForOffline
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

@Composable
internal fun MealPreview(imageBytes: ByteArray?, imageInput: String, square: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxWidth().then(if (square) Modifier.aspectRatio(1f) else Modifier.height(240.dp)).clip(RoundedCornerShape(28.dp))
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
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).background(JanuaryColors.Green.copy(alpha = 0.1f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(20.dp), tint = JanuaryColors.Green)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Photograph the entire meal", style = MaterialTheme.typography.titleLarge)
                Text("January identifies foods, servings, and nutrition — then estimates glucose impact.", color = JanuaryColors.Body)
            }
        }
    }
}

@Composable
internal fun ScanResult(result: FoodScan, imageBytes: ByteArray?, imageInput: String) {
    if (imageBytes != null) MealPreview(imageBytes, imageInput, square = true)
    Text(result.mealName ?: "Meal analysis", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    result.totalNutrients?.let { nutrients ->
        DemoCard { ScanMacroStrip(nutrients) }
        val rows = scanNutritionRows(nutrients)
        if (rows.isNotEmpty()) {
            DemoCard {
                NutritionList(rows.map { (label, value) -> NutritionValue(label, "${formatScanNumber(value.value)} ${value.unit}") })
            }
        }
    }
    if (result.detections.orEmpty().isNotEmpty()) Text("Detected foods", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    result.detections.orEmpty().forEach { detection ->
        DemoCard {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(detection.food.name, style = MaterialTheme.typography.titleMedium)
                    detection.food.brandName?.takeIf(String::isNotBlank)?.let { Text(it, color = JanuaryColors.Muted) }
                }
                detection.confidenceScore?.let {
                    val confidenceColor = when (it.lowercase()) { "high" -> JanuaryColors.Green; "medium" -> JanuaryColors.Gold; else -> JanuaryColors.Rust }
                    Text(
                        "${it.replaceFirstChar(Char::uppercase)} confidence",
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(confidenceColor.copy(alpha = .13f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        color = confidenceColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            ScanMacroStrip(detection.food.nutrients)
        }
    }
    result.glucoseImpact?.takeIf { it.prediction.isNotEmpty() }?.let { impact ->
        Text("Estimated glucose response", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(impact.impactScore.replace('_', ' ').replaceFirstChar(Char::uppercase), color = JanuaryColors.Rust, fontWeight = FontWeight.SemiBold)
        PhotoGlucoseChart(impact)
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
        minimum = null,
        maximum = null,
        showTargetBand = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageUrlSheet(initialValue: String, onDismiss: () -> Unit, onUse: (String) -> Unit) {
    var value by remember { mutableStateOf(initialValue) }
    val valid = runCatching { java.net.URI(value.trim()) }.getOrNull()?.let { it.scheme?.lowercase() in listOf("http", "https") && !it.host.isNullOrBlank() } == true
    AppModalSheet(title = "Use image URL", onDismiss = onDismiss, expanded = false) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding).padding(top = 28.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            DemoCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Link, null, tint = JanuaryColors.Green); Text("Public image", style = MaterialTheme.typography.titleMedium, color = JanuaryColors.Green) }
                Text("Paste a direct HTTPS link to a meal photo.", color = JanuaryColors.Body)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Image address")
                DemoInput(value, { value = it }, "https://example.com/meal.jpg")
                Text("The server must be able to download the image without signing in.", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
            DemoPrimaryButton("Use image URL", { onUse(value.trim()) }, Modifier.fillMaxWidth(), enabled = valid, icon = { Icon(Icons.Outlined.DownloadForOffline, null) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CorrectionSheet(
    initial: FoodScan,
    onDismiss: () -> Unit,
    onSubmit: (String, String, (Throwable) -> Unit) -> Unit,
) {
    var mealName by remember { mutableStateOf(initial.mealName.orEmpty()) }
    var instruction by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    fun submitCorrection() {
        if (submitting || instruction.isBlank()) return
        error = null
        if (initial.detections.orEmpty().isEmpty()) {
            error = IllegalStateException("There are no detections available to correct.")
            return
        }
        submitting = true
        onSubmit(mealName.trim().ifEmpty { "Meal" }, instruction.trim()) { failure ->
            error = failure
            submitting = false
        }
    }
    AppModalSheet(title = "Correct result", onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding).padding(top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Meal")
            DemoInput(mealName, { mealName = it }, "Meal name")
            SectionLabel("Current detections")
            DemoCard {
                initial.detections.orEmpty().forEachIndexed { index, detection ->
                    Text(detection.food.name, Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.titleMedium)
                    if (index < initial.detections.orEmpty().lastIndex) HorizontalDivider(color = JanuaryColors.Divider)
                }
            }
            SectionLabel("What should change?")
            OutlinedTextField(instruction, { instruction = it }, Modifier.fillMaxWidth().height(150.dp), placeholder = { Text("Describe the correction") }, shape = RoundedCornerShape(24.dp))
            Text("For example: The oatmeal was steel-cut, about 2 cups, and there was no honey.", color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
            error?.let { ErrorCard(it, ::submitCorrection) }
            DemoPrimaryButton(
                "Submit correction",
                ::submitCorrection,
                Modifier.fillMaxWidth(),
                enabled = instruction.trim().isNotEmpty(),
                loading = submitting,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarcodeResultSheet(state: DemoState, food: FoodSearchItem, onDismiss: () -> Unit) {
    AppModalSheet(title = "Food details", onDismiss = onDismiss, showNavigationBar = false) {
        FoodDetailScreen(
            state = state,
            food = food,
            onBack = onDismiss,
            isModal = true,
        )
    }
}

internal fun decodeImage(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
internal fun formatScanNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
