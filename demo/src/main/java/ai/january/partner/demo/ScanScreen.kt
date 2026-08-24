package ai.january.partner.demo

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.january.partner.photos.PhotoScan
import ai.january.partner.photos.ScanFoodPhotoRequest
import kotlinx.coroutines.launch

private const val SAMPLE_MEAL_URL = "https://friendlysrestaurants.com/assets/live/img/production/detail/menu/lunch-dinner_999-combohs_all-american-burger-fries.jpg"

@Composable
fun ScanScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageInput by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showUrlField by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PhotoScan?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@rememberLauncherForActivityResult
        imageBytes = bytes
        imageInput = "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        result = null
        error = null
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
        DemoTopBar("Scan a meal", settingsAction)
        DemoScreen {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (result == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bitmap = remember(imageBytes) { imageBytes?.let(::decodeImage) }
                        if (bitmap != null) {
                            Image(bitmap.asImageBitmap(), contentDescription = "Selected meal", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(28.dp)) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null)
                                Text("Add a clear photo of the whole meal", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                                Text("January identifies foods, servings, and nutrition from the image.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                        ) { Icon(Icons.Outlined.Image, contentDescription = null); Text(" Choose photo") }
                        OutlinedButton(onClick = { showUrlField = !showUrlField }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Link, contentDescription = null); Text(" Image URL")
                        }
                    }
                    OutlinedButton(
                        onClick = { imageInput = SAMPLE_MEAL_URL; imageBytes = null; result = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Icon(Icons.Outlined.Restaurant, contentDescription = null); Text(" Use sample meal") }
                    if (showUrlField) {
                        OutlinedTextField(
                            value = if (imageInput.startsWith("data:")) "" else imageInput,
                            onValueChange = { imageInput = it; imageBytes = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Public image URL") },
                            singleLine = true,
                        )
                    }
                    Button(
                        onClick = ::analyze,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = imageInput.isNotBlank() && client != null && !loading,
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        else Text("Analyze meal")
                    }
                    if (client == null) ApiKeyRequiredCard()
                    error?.let { ErrorCard(it, ::analyze) }
                } else {
                    ScanResult(result = result!!)
                    Button(onClick = { result = null; imageInput = ""; imageBytes = null }, modifier = Modifier.fillMaxWidth()) { Text("Scan another meal") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ScanResult(result: PhotoScan) {
    Text(result.mealName ?: "Meal scan", style = MaterialTheme.typography.headlineMedium)
    result.totalNutrients?.let { nutrients ->
        DemoCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScanMetric("Calories", nutrients.calories?.value, "cal")
                ScanMetric("Protein", nutrients.protein?.value, "g")
                ScanMetric("Carbs", nutrients.carbohydrates?.value, "g")
                ScanMetric("Fat", nutrients.totalFat?.value, "g")
            }
        }
    }
    SectionLabel("Detected foods")
    result.detections.orEmpty().forEach { detection ->
        DemoCard {
            Text(detection.food.name, style = MaterialTheme.typography.titleMedium)
            detection.confidenceScore?.let { Text("Confidence $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun ScanMetric(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "—", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        Text(unit, style = MaterialTheme.typography.bodySmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun decodeImage(bytes: ByteArray): android.graphics.Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
