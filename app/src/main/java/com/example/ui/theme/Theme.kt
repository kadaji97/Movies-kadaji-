package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Builds a dynamic Material 3 ColorScheme for each specified palette:
 * 1. Imperial Gold (Premium Cinematic - Default): Rich Liquid Gold (#D4AF37) + Deep Obsidian Black (#08080A)
 * 2. Midnight Theater (Classic Deep Dark): Emerald Green (#10B981) + Deep Void Blue-Black (#0B0F19)
 * 3. Cyberpunk Neon (High Contrast): Popcorn Amber (#FBBF24) + Matte Carbon Black (#0F0F10)
 * 4. Hollywood Rouge (Vintage Cinema): Neon Velvet Red (#EF4444) + Deep Maroon Black (#110606)
 */
fun getAppColorScheme(themeMode: AppThemeMode): ColorScheme = when (themeMode) {
    AppThemeMode.IMPERIAL_GOLD -> darkColorScheme(
        primary = ImperialLiquidGold,
        onPrimary = Color(0xFF1E1700),
        primaryContainer = Color(0xFF4A3C08),
        onPrimaryContainer = ImperialLiquidGold,

        secondary = ImperialBurnishedGold,
        onSecondary = Color(0xFF1E1700),
        secondaryContainer = Color(0xFF382902),
        onSecondaryContainer = Color(0xFFFBE49D),

        tertiary = Color(0xFFF3E5AB),
        onTertiary = Color(0xFF1E1700),

        background = ImperialObsidianBackground,
        onBackground = ImperialRadiantChampagneWhite,

        surface = ImperialObsidianBackground,
        onSurface = ImperialRadiantChampagneWhite,
        surfaceVariant = ImperialCrystalSurface,
        onSurfaceVariant = ImperialTextSecondary,

        outline = ImperialOutline,
        outlineVariant = Color(0x33FFFFFF),

        error = PremiumRougeNeonRed,
        onError = Color.White
    )

    AppThemeMode.MIDNIGHT_THEATER -> darkColorScheme(
        primary = MidnightEmerald,
        onPrimary = Color(0xFF022C22),
        primaryContainer = Color(0xFF064E3B),
        onPrimaryContainer = MidnightEmerald,

        secondary = MidnightSecondary,
        onSecondary = Color(0xFF064E3B),
        secondaryContainer = Color(0xFF065F46),
        onSecondaryContainer = Color(0xFFA7F3D0),

        tertiary = Color(0xFF6EE7B7),
        onTertiary = Color(0xFF022C22),

        background = MidnightVoidBackground,
        onBackground = MidnightTextOnSurface,

        surface = MidnightVoidBackground,
        onSurface = MidnightTextOnSurface,
        surfaceVariant = MidnightCrystalSurface,
        onSurfaceVariant = MidnightTextSecondary,

        outline = MidnightOutline,
        outlineVariant = Color(0x2EFFFFFF),

        error = PremiumRougeNeonRed,
        onError = Color.White
    )

    AppThemeMode.CYBERPUNK_NEON -> darkColorScheme(
        primary = CyberpunkPopcornAmber,
        onPrimary = Color(0xFF451A03),
        primaryContainer = Color(0xFF78350F),
        onPrimaryContainer = CyberpunkPopcornAmber,

        secondary = CyberpunkSecondary,
        onSecondary = Color(0xFF78350F),
        secondaryContainer = Color(0xFF92400E),
        onSecondaryContainer = Color(0xFFFEF3C7),

        tertiary = Color(0xFFFDE68A),
        onTertiary = Color(0xFF451A03),

        background = CyberpunkMatteBackground,
        onBackground = CyberpunkTextOnSurface,

        surface = CyberpunkMatteBackground,
        onSurface = CyberpunkTextOnSurface,
        surfaceVariant = CyberpunkCrystalSurface,
        onSurfaceVariant = CyberpunkTextSecondary,

        outline = CyberpunkOutline,
        outlineVariant = Color(0x33FFFFFF),

        error = PremiumRougeNeonRed,
        onError = Color.White
    )

    AppThemeMode.HOLLYWOOD_ROUGE -> darkColorScheme(
        primary = PremiumRougeNeonRed,
        onPrimary = Color(0xFF450A0A),
        primaryContainer = Color(0xFF7F1D1D),
        onPrimaryContainer = PremiumRougeNeonRed,

        secondary = PremiumRougeSecondary,
        onSecondary = Color(0xFF7F1D1D),
        secondaryContainer = Color(0xFF991B1B),
        onSecondaryContainer = Color(0xFFFEE2E2),

        tertiary = Color(0xFFFCA5A5),
        onTertiary = Color(0xFF450A0A),

        background = PremiumRougeMaroonBackground,
        onBackground = PremiumRougeTextOnSurface,

        surface = PremiumRougeMaroonBackground,
        onSurface = PremiumRougeTextOnSurface,
        surfaceVariant = PremiumRougeCrystalSurface,
        onSurfaceVariant = PremiumRougeTextSecondary,

        outline = PremiumRougeOutline,
        outlineVariant = Color(0x2EFFFFFF),

        error = Color(0xFFB91C1C),
        onError = Color.White
    )
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = getAppColorScheme(themeMode)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
