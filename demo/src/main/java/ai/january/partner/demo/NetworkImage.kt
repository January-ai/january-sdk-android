package ai.january.partner.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun NetworkImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderSize: Dp = 24.dp,
) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(JanuaryColors.Control),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(Icons.Outlined.Restaurant, contentDescription = null, modifier = Modifier.size(placeholderSize), tint = JanuaryColors.Green)
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkImagePreview() {
    JanuaryDemoTheme { NetworkImage(null, null, Modifier.size(88.dp)) }
}
