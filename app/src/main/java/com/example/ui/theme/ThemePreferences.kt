package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App Theme Modes supported by the Dynamic Material 3 Multi-Theme Architecture:
 * 1. Imperial Gold (Premium Cinematic - Default): Rich Liquid Gold (#D4AF37) + Deep Obsidian Black (#08080A)
 * 2. Midnight Theater (Classic Deep Dark): Emerald Green (#10B981) + Deep Void Blue-Black (#0B0F19)
 * 3. Cyberpunk Neon (High Contrast): Popcorn Amber (#FBBF24) + Matte Carbon Black (#0F0F10)
 * 4. Hollywood Rouge (Vintage Cinema): Neon Velvet Red (#EF4444) + Deep Maroon Black (#110606)
 */
enum class AppThemeMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val crystalSurfaceHex: Long,
    val textHex: Long
) {
    IMPERIAL_GOLD(
        id = "imperial_gold",
        title = "Imperial Gold",
        subtitle = "Premium Cinematic • Rich Liquid Gold",
        primaryHex = 0xFFD4AF37,
        secondaryHex = 0xFFAA7C11,
        backgroundHex = 0xFF08080A,
        surfaceHex = 0xFF1C1A14,
        crystalSurfaceHex = 0xB31C1A14, // Translucent Charcoal-Gold Tint (Alpha 70%)
        textHex = 0xFFFDFBF7 // Radiant Champagne White
    ),
    MIDNIGHT_THEATER(
        id = "midnight_theater",
        title = "Midnight Theater",
        subtitle = "Classic Deep Dark • Emerald Green",
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF059669,
        backgroundHex = 0xFF0B0F19,
        surfaceHex = 0xFF1F2937,
        crystalSurfaceHex = 0xBF1F2937, // Translucent Space Gray (Alpha 75%)
        textHex = 0xFFF9FAFB // Crisp White
    ),
    CYBERPUNK_NEON(
        id = "cyberpunk_neon",
        title = "Cyberpunk Neon",
        subtitle = "High Contrast • Popcorn Amber",
        primaryHex = 0xFFFBBF24,
        secondaryHex = 0xFFF59E0B,
        backgroundHex = 0xFF0F0F10,
        surfaceHex = 0xFF1A1A1E,
        crystalSurfaceHex = 0xB31A1A1E, // Translucent Dark Tint (Alpha 70%)
        textHex = 0xFFFFFFFF // Pure White
    ),
    HOLLYWOOD_ROUGE(
        id = "hollywood_rouge",
        title = "Hollywood Rouge",
        subtitle = "Vintage Cinema • Neon Velvet Red",
        primaryHex = 0xFFEF4444,
        secondaryHex = 0xFFDC2626,
        backgroundHex = 0xFF110606,
        surfaceHex = 0xFF220F0F,
        crystalSurfaceHex = 0xA6220F0F, // Translucent Wine Tint (Alpha 65%)
        textHex = 0xFFFFEAEA // Soft Rose White
    );

    companion object {
        val DEFAULT = IMPERIAL_GOLD
        val CYBERPUNK_AMBER get() = CYBERPUNK_NEON
        val PREMIUM_ROUGE get() = HOLLYWOOD_ROUGE

        fun fromId(id: String?): AppThemeMode {
            return when (id) {
                IMPERIAL_GOLD.id -> IMPERIAL_GOLD
                MIDNIGHT_THEATER.id -> MIDNIGHT_THEATER
                CYBERPUNK_NEON.id, "cyberpunk_amber" -> CYBERPUNK_NEON
                HOLLYWOOD_ROUGE.id, "premium_rouge" -> HOLLYWOOD_ROUGE
                else -> DEFAULT
            }
        }
    }
}

/**
 * Local persistent storage manager ensuring theme selections survive app restarts.
 */
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("movieskadaji_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromId(prefs.getString(KEY_THEME_MODE, AppThemeMode.DEFAULT.id))
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.id).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val KEY_THEME_MODE = "selected_app_theme_mode"

        @Volatile
        private var INSTANCE: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
