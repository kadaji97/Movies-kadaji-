package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrystalGlassTopSpecular
import com.example.ui.theme.CrystalGlassWhiteBorder

/**
 * High-performance "Crystal" Glassmorphism Visual Layer for Movieskadaji.
 *
 * Implements:
 * 1. Translucent alpha-channel coloring based on the active Material 3 ColorScheme.
 * 2. Clean, ultra-thin frosted borders (1dp border width with low opacity gradient).
 * 3. Specular highlight line along the top chamfer creating optical glass depth.
 * 4. High-contrast text legibility backing scrims.
 */

/**
 * Creates a two-tone frosted glass gradient border brush (top highlight -> subtle bottom fade).
 */
@Composable
fun crystalGlassBorderBrush(
    accentColor: Color = MaterialTheme.colorScheme.primary,
    baseOpacity: Float = 0.22f
): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = (baseOpacity * 1.35f).coerceAtMost(0.40f)),
            accentColor.copy(alpha = (baseOpacity * 0.9f).coerceAtMost(0.30f)),
            Color.White.copy(alpha = (baseOpacity * 0.45f).coerceAtMost(0.15f))
        )
    )
}

/**
 * Subtle specular highlight reflection strip on the upper rim of crystal cards.
 */
@Composable
fun CrystalGlassSheenLine(
    modifier: Modifier = Modifier,
    sheenColor: Color = CrystalGlassTopSpecular
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        sheenColor.copy(alpha = 0.35f),
                        sheenColor.copy(alpha = 0.65f),
                        sheenColor.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Reusable Crystal Glass Card container.
 */
@Composable
fun CrystalGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    accentBorder: Boolean = true,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderBrush = if (accentBorder) {
        crystalGlassBorderBrush(accentColor = primaryColor, baseOpacity = 0.25f)
    } else {
        Brush.verticalGradient(
            colors = listOf(
                CrystalGlassWhiteBorder,
                CrystalGlassWhiteBorder.copy(alpha = 0.08f)
            )
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = Color.Black)
            .clip(shape)
            .background(containerColor)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
    ) {
        content()
        // Top specular reflection line
        CrystalGlassSheenLine(sheenColor = Color.White.copy(alpha = 0.30f))
    }
}

/**
 * Reusable Crystal Glass Surface for navigation panels, headers, dialogs, and overlays.
 */
@Composable
fun CrystalGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    borderBrush: Brush = crystalGlassBorderBrush(baseOpacity = 0.18f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        content()
    }
}

/**
 * Crystal Glass Badge for media tags (e.g., HD, 4K, TMDb rating, AD).
 */
@Composable
fun CrystalGlassBadge(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
    content: @Composable () -> Unit
) {
    Surface(
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        content()
    }
}
