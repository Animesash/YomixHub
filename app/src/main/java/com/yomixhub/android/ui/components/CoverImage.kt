package com.yomixhub.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yomixhub.android.data.SourceRepository

/**
 * Title cover: remote image when [coverUrl] is available, duotone gradient
 * with a watermark letter as fallback.
 *
 * [sourceId] selects the Referer/User-Agent headers that must accompany the
 * request (LibGroup CDNs refuse "hotlinked" images without a proper Referer).
 */
@Composable
fun CoverImage(
    coverUrl: String?,
    sourceId: String?,
    letter: String,
    from: Color,
    to: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val context = LocalContext.current
    val headers = remember(sourceId) { SourceRepository.byId(sourceId)?.imageHeaders }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(from, to),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            ),
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .apply {
                        headers?.forEach { (name, value) -> addHeader(name, value) }
                    }
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                placeholder = ColorPainter(Color.Transparent),
            )
        } else {
            Text(
                text = letter.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
