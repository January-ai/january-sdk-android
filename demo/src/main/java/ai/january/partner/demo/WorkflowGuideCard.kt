package ai.january.partner.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WorkflowStep(val title: String, val description: String)

@Composable
fun WorkflowGuideCard(
    title: String,
    steps: List<WorkflowStep>,
    modifier: Modifier = Modifier,
) {
    DemoCard(modifier) {
        SectionLabel(title)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = JanuaryColors.ControlStrong,
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(step.title, fontWeight = FontWeight.Bold)
                        Text(step.description, color = JanuaryColors.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkflowGuideCardPreview() {
    JanuaryDemoTheme { WorkflowGuideCard("How it works", listOf(WorkflowStep("Choose a food", "Serving and quantity shape the estimate."))) }
}

/** The meal workflow guide shared by the iOS food-log screen and editor. */
@Composable
internal fun MealWorkflowGuide(title: String, message: String, steps: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FoodLogCard {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Surface(Modifier.size(44.dp), shape = CircleShape, color = JanuaryColors.TargetBand) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(icon, null, Modifier.size(20.dp), tint = JanuaryColors.Green)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(message, fontSize = 15.sp, lineHeight = 20.sp, color = JanuaryColors.Body)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Surface(Modifier.size(24.dp), shape = CircleShape, color = JanuaryColors.TargetBand) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF3E5A3A))
                            }
                        }
                        Text(step, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
