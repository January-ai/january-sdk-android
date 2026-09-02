package ai.january.partner.demo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.january.partner.foods.ServingOption
import ai.january.partner.glucose.GlucosePrediction
import ai.january.partner.glucose.GlucosePredictionProfile
import ai.january.partner.glucose.Height
import ai.january.partner.glucose.HeightUnit
import ai.january.partner.glucose.MedicalCondition
import ai.january.partner.glucose.PredictGlucoseRequest
import ai.january.partner.glucose.Sex
import ai.january.partner.glucose.Weight
import ai.january.partner.glucose.WeightUnit
import ai.january.partner.models.FoodSelection
import ai.january.partner.models.ServingSelection
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val GoldText = androidx.compose.ui.graphics.Color(0xFF6E5613)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlucoseScreen(state: DemoState, settingsAction: () -> Unit, modifier: Modifier = Modifier) {
    val client = state.client
    val userClient = state.userClient
    val coroutineScope = rememberCoroutineScope()
    var age by remember { mutableStateOf("42") }
    var sex by remember { mutableStateOf(Sex.FEMALE) }
    var heightInches by remember { mutableDoubleStateOf(66.0) }
    var weightPounds by remember { mutableDoubleStateOf(150.0) }
    var conditions by remember { mutableStateOf<Set<MedicalCondition>>(emptySet()) }
    var foods by remember { mutableStateOf<List<DemoSelectedFood>>(emptyList()) }
    var startTime by remember { mutableStateOf(OffsetDateTime.now()) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<GlucosePrediction?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Throwable?>(null) }

    fun predict() {
        val sdk = client ?: return
        if (foods.isEmpty()) return
        loading = true
        error = null
        coroutineScope.launch {
            runCatching {
                sdk.glucose.predict(
                    PredictGlucoseRequest(
                        userProfile = GlucosePredictionProfile(
                            age = age.toDouble(),
                            sex = sex,
                            height = Height(heightInches, HeightUnit.INCHES),
                            weight = Weight(weightPounds, WeightUnit.POUNDS),
                            healthConditions = conditions.toList(),
                        ),
                        foods = foods.map {
                            FoodSelection(it.food.id.value, ServingSelection(requireNotNull(it.serving.id).value, it.quantity))
                        },
                        startTime = startTime,
                    ),
                )
            }.onSuccess { result = it }
                .onFailure { error = it }
            loading = false
        }
    }

    androidx.activity.compose.BackHandler(enabled = result != null) { result = null }
    if (showConditions) {
        ConditionsScreen(conditions, { conditions = it }, { showConditions = false }, modifier)
        return
    }
    if (result == null) {
        AppScreenScaffold(
            title = "Glucose", modifier = modifier, style = AppNavigationTitleStyle.Leading,
            trailing = { AppNavigationButton(AppNavigationButtonKind.Settings, onClick = settingsAction) },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = DemoScreenPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                MealWorkflowGuide(
                    title = "Estimate this meal’s response",
                    message = "Glucose prediction is a simulation. Your profile shapes the estimate, and the foods and servings define the meal. It does not create a food log.",
                    steps = listOf("Review the prediction profile", "Add every food in the meal to simulate", "Estimate the glucose response curve"),
                    icon = Icons.Outlined.MonitorHeart,
                )

                FormSection("Prediction profile", "Age, sex, body measurements, and health conditions influence the estimated response.") {
                    MeasurementRow("Age", age, { age = numericText(it) }, "years")
                    HorizontalDivider(color = JanuaryColors.Divider)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Sex", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        SegmentedControl(
                            options = listOf(Sex.FEMALE, Sex.MALE),
                            selected = sex,
                            label = { if (it == Sex.FEMALE) "Female" else "Male" },
                            onSelect = { sex = it },
                            modifier = Modifier.weight(2f),
                        )
                    }
                    HorizontalDivider(color = JanuaryColors.Divider)
                    HeightInput(heightInches = heightInches, onHeightInchesChange = { heightInches = it })
                    HorizontalDivider(color = JanuaryColors.Divider)
                    WeightInput(weightPounds = weightPounds, onWeightPoundsChange = { weightPounds = it })
                    HorizontalDivider(color = JanuaryColors.Divider)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showConditions = true }.padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Health conditions", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text(if (conditions.isEmpty()) "None" else "${conditions.size} selected", color = JanuaryColors.Muted)
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = JanuaryColors.Subdued)
                    }
                }

                FormSection("Meal to simulate", "Add one or more foods here. This meal is used only for the prediction and is not saved to Food Logs.") {
                    StartTimeRow(startTime = startTime, onChange = { startTime = it })
                    foods.forEachIndexed { index, selected ->
                        HorizontalDivider(color = JanuaryColors.Divider)
                        SelectedFoodRow(
                            selected = selected,
                            onServingChange = { serving ->
                                foods = foods.toMutableList().also { it[index] = selected.copy(serving = serving) }
                            },
                            onQuantityChange = { quantity ->
                                foods = if (quantity < 0.25) {
                                    foods.filterIndexed { itemIndex, _ -> itemIndex != index }
                                } else {
                                    foods.toMutableList().also { it[index] = selected.copy(quantity = quantity) }
                                }
                            },
                            onRemove = { foods = foods.filterIndexed { itemIndex, _ -> itemIndex != index } },
                        )
                    }
                    HorizontalDivider(color = JanuaryColors.Divider)
                    TextButton(
                        onClick = { showFoodPicker = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text("＋  Add food to prediction", color = GoldText, style = MaterialTheme.typography.titleMedium)
                    }
                }

                error?.let { ErrorCard(it, ::predict) }
                DemoPrimaryButton(
                    text = "Estimate glucose response",
                    onClick = ::predict,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = foods.isNotEmpty() && client != null,
                    loading = loading,
                )
                if (client == null) ApiKeyRequiredCard()
                Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        AppScreenScaffold(
            title = "Estimated response", modifier = modifier,
            leading = { AppNavigationButton(AppNavigationButtonKind.Back, title = "Back from Estimated response", onClick = { result = null }) },
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DemoScreenPadding, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                GlucosePredictionResult(result!!, foods)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DemoSecondaryButton("Adjust meal", { result = null }, Modifier.weight(1f))
                    DemoPrimaryButton("Start over", { result = null; foods = emptyList() }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showFoodPicker && client != null) {
        FoodPickerSheet(
            state = state,
            onDismiss = { showFoodPicker = false },
            onSelect = { foods = foods + it },
        )
    }
}
