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
