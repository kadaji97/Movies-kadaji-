package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.*

/**
 * MediaCard
 *
 * Implements:
 * - High-end Crystal Glassmorphism experience with translucent backdrop, ultra-thin 1dp border,
 *   specular highlight sheen line, and 12dp rounded corners.
 * - Strict accessibility contrast ratios with vertical gradient backing scrims ensuring legible text
 *   over any high-key poster artwork.
 * - Seamless visual alignment across poster grids (dimensions, aspect ratio, elevation, radius).
 */
@Composable
fun MediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: androidx.compose.ui.unit.Dp = 145.dp,
    cardHeight: androidx.compose.ui.unit.Dp = 220.dp,
    showGenres: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1.0f, label = "card_scale")

    val widthModifier = if (cardWidth != androidx.compose.ui.unit.Dp.Unspecified) {
        Modifier.width(cardWidth)
    } else {
        Modifier.fillMaxWidth()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(12.dp)
    val borderBrush = crystalGlassBorderBrush(accentColor = primaryColor, baseOpacity = 0.22f)

    Column(
        modifier = modifier
            .then(widthModifier)
            .scale(scale)
            .testTag("media_card_${mediaItem.id}")
    ) {
        Card(
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .shadow(elevation = 6.dp, shape = cardShape, spotColor = Color.Black)
                .border(BorderStroke(1.dp, borderBrush), cardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Poster Image
                val imageModel: Any = if (mediaItem.drawableResId != null) {
                    mediaItem.drawableResId
                } else {
                    mediaItem.posterUrl
                }

                AsyncImage(
                    model = imageModel,
                    contentDescription = mediaItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top & Bottom Gradient Overlay for Accessibility Contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                // Top Specular Highlight Sheen Line (Crystal Effect)
                CrystalGlassSheenLine(
                    modifier = Modifier.align(Alignment.TopCenter),
                    sheenColor = Color.White.copy(alpha = 0.35f)
                )

                // Top Quality Badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (mediaItem.qualityBadge.contains("4K", ignoreCase = true)) {
                                primaryColor
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = mediaItem.qualityBadge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Top Rating Badge with Frosted Glass backing
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = primaryColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = String.format("%.1f", mediaItem.imdbRating),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Quick Play Icon Hint
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, primaryColor.copy(alpha = 0.40f), RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Bottom Year & Duration Info with high-contrast text
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${mediaItem.releaseYear}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = mediaItem.duration,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title with high contrast
        Text(
            text = mediaItem.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Genres
        if (showGenres) {
            Text(
                text = mediaItem.genres.take(2).joinToString(" • "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
