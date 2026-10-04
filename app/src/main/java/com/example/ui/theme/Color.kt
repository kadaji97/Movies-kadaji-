package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// 1. IMPERIAL GOLD (Premium Cinematic - Default)
// =========================================================================
val ImperialLiquidGold = Color(0xFFD4AF37)
val ImperialBurnishedGold = Color(0xFFAA7C11)
val ImperialObsidianBackground = Color(0xFF08080A)
val ImperialCharcoalGoldTint = Color(0xFF1C1A14)
val ImperialCrystalSurface = Color(0xB31C1A14) // Alpha 70%
val ImperialCrystalElevated = Color(0xCC2A261C)
val ImperialRadiantChampagneWhite = Color(0xFFFDFBF7)
val ImperialTextSecondary = Color(0xFFC7BAA7)
val ImperialOutline = Color(0x40D4AF37)
val ImperialGlow = Color(0x40D4AF37)

// =========================================================================
// 2. MIDNIGHT THEATER (Classic Deep Dark)
// =========================================================================
val MidnightEmerald = Color(0xFF10B981)
val MidnightSecondary = Color(0xFF059669)
val MidnightVoidBackground = Color(0xFF0B0F19)
val MidnightSpaceGray = Color(0xFF1F2937)
val MidnightCrystalSurface = Color(0xBF1F2937) // Alpha 75%
val MidnightCrystalElevated = Color(0xD0283548)
val MidnightTextOnSurface = Color(0xFFF9FAFB)
val MidnightTextSecondary = Color(0xFF9CA3AF)
val MidnightOutline = Color(0xFF374151)
val MidnightGlow = Color(0x3310B981)

// =========================================================================
// 3. CYBERPUNK NEON (High Contrast)
// =========================================================================
val CyberpunkPopcornAmber = Color(0xFFFBBF24)
val CyberpunkSecondary = Color(0xFFF59E0B)
val CyberpunkMatteBackground = Color(0xFF0F0F10)
val CyberpunkDarkTint = Color(0xFF1A1A1E)
val CyberpunkCrystalSurface = Color(0xB31A1A1E) // Alpha 70%
val CyberpunkCrystalElevated = Color(0xCC27272C)
val CyberpunkTextOnSurface = Color(0xFFFFFFFF)
val CyberpunkTextSecondary = Color(0xFFA1A1AA)
val CyberpunkOutline = Color(0xFF3F3F46)
val CyberpunkGlow = Color(0x33FBBF24)

// =========================================================================
// 4. HOLLYWOOD ROUGE (Vintage Cinema)
// =========================================================================
val PremiumRougeNeonRed = Color(0xFFEF4444)
val PremiumRougeSecondary = Color(0xFFDC2626)
val PremiumRougeMaroonBackground = Color(0xFF110606)
val PremiumRougeWineTint = Color(0xFF220F0F)
val PremiumRougeCrystalSurface = Color(0xA6220F0F) // Alpha 65%
val PremiumRougeCrystalElevated = Color(0xC0341919)
val PremiumRougeTextOnSurface = Color(0xFFFFEAEA)
val PremiumRougeTextSecondary = Color(0xFFD4A5A5)
val PremiumRougeOutline = Color(0xFF522121)
val PremiumRougeGlow = Color(0x33EF4444)

// =========================================================================
// Crystal Glassmorphism Core Visual Tokens
// =========================================================================
val CrystalGlassWhiteBorder = Color(0x33FFFFFF)       // Ultra-thin 1dp frosted border (20% opacity)
val CrystalGlassSubtleBorder = Color(0x1AFFFFFF)      // Faint border (10% opacity)
val CrystalGlassTopSpecular = Color(0x4DFFFFFF)       // Subtle top specular edge sheen (30% opacity)
val CrystalGlassBottomShadow = Color(0x80000000)      // Deep frosted shadow
val CrystalGlassScrimDark = Color(0xE608080A)         // 90% opacity high-contrast text backing scrim

// =========================================================================
// Legacy/Global Tokens (Referenced throughout components for backwards compatibility)
// =========================================================================
val DarkBackground = ImperialObsidianBackground
val DarkSurface = ImperialObsidianBackground
val DarkSurfaceElevated = ImperialCharcoalGoldTint
val DarkSurfaceHighlight = ImperialCrystalElevated
val DarkOutline = ImperialOutline
val DarkOutlineVariant = Color(0x33D4AF37)

val CyberCyan = ImperialLiquidGold
val CyberCyanMuted = ImperialBurnishedGold
val CyberCyanGlow = ImperialGlow
val NeonAmber = CyberpunkPopcornAmber
val NeonAmberGlow = CyberpunkGlow
val NeonGreen = MidnightEmerald
val NeonRed = PremiumRougeNeonRed

val TextPrimary = ImperialRadiantChampagneWhite
val TextSecondary = ImperialTextSecondary
val TextTertiary = Color(0xFF8A8275)
