package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

data class NusuThemePreset(
    val id: String,
    val name: String,
    val collectionName: String,
    val edition: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val glowColor: Color,
    val backgroundColor: Color,
    val cardBackgroundColor: Color,
    val successColor: Color,
    val orbInnerColor: Color,
    val orbOuterColor: Color,
    val typographyStyle: String = "Inter",
    val isGlassMorphic: Boolean = true
)

object ThemeManager {
    private val collections = listOf(
        "Bento Grid Design",
        "NUSU Original OS",
        "Neon AI OS",
        "Cyberpunk AI",
        "Holographic AI",
        "Quantum AI",
        "Jarvis Command Center",
        "Futuristic Military AI",
        "Space Command AI",
        "Galaxy Intelligence OS",
        "Luxury Glass AI",
        "Neural Network Interface",
        "Digital Companion UI",
        "Floating Hologram OS",
        "Minimal Future UI",
        "Ultra Premium Black Edition",
        "Blue Neon Edition",
        "Purple Neural Edition",
        "White Crystal Edition",
        "Matrix Intelligence UI",
        "Zen OS Companion",
        "Experimental Future Concepts"
    )

    private val editions = listOf(
        "Standard Edition",
        "Pro Edition",
        "Sleek Stealth",
        "Supercharged",
        "Alpha Variant"
    )

    // Palette combinations for high-tech aesthetics
    private val colorPaletts = listOf(
        // Primary, Secondary, Accent, Glow
        listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFF472B6), Color(0xFF38BDF8)), // bento blue/indigo
        listOf(Color(0xFF00E5FF), Color(0xFF00FFFF), Color(0xFF8B5CF6), Color(0xFF8B5CF6)), // original cyan-purple
        listOf(Color(0xFF00FF9D), Color(0xFF00FFFF), Color(0xFF00E5FF), Color(0xFF00FF9D)), // neon green/cyan
        listOf(Color(0xFFFF007F), Color(0xFFFF9F43), Color(0xFF8B5CF6), Color(0xFFFF007F)), // cyberpunk pink-gold
        listOf(Color(0xFF00E5FF), Color(0xFFEE82EE), Color(0xFF8B5CF6), Color(0xFFEE82EE)), // holographic lavender
        listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF38BDF8), Color(0xFFE2E8F0)), // sleek silver
        listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFF219EBC), Color(0xFFFB8500)), // amber/gold war room
        listOf(Color(0xFFEF233C), Color(0xFFD90429), Color(0xFF2B2D42), Color(0xFFEF233C)), // military red
        listOf(Color(0xFF4361EE), Color(0xFF4CC9F0), Color(0xFF7209B7), Color(0xFF4CC9F0)), // space indigo/cyan
        listOf(Color(0xFF7209B7), Color(0xFFB5179E), Color(0xFF4361EE), Color(0xFFB5179E)), // galaxy magenta-purple
        listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF00FFFF), Color(0xFFFFFFFF)), // luxury glass
        listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF6EE7B7), Color(0xFF10B981))  // matrix emerald
    )

    val defaultTheme = NusuThemePreset(
        id = "bento_grid_default",
        name = "Bento Grid Minimal (Default)",
        collectionName = "Bento Grid Design",
        edition = "M3 Matte Edition",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF818CF8),
        accentColor = Color(0xFFF472B6),
        glowColor = Color(0xFF38BDF8),
        backgroundColor = Color(0xFF0F172A),
        cardBackgroundColor = Color(0xFF1E293B),
        successColor = Color(0xFF34D399),
        orbInnerColor = Color(0xFF38BDF8),
        orbOuterColor = Color(0xFF818CF8),
        typographyStyle = "Space Grotesk",
        isGlassMorphic = true
    )

    private val mutableThemes = mutableListOf<NusuThemePreset>()
    val premiumThemes: List<NusuThemePreset> get() = mutableThemes

    private val _currentTheme = MutableStateFlow(defaultTheme)
    val currentTheme: StateFlow<NusuThemePreset> = _currentTheme

    private val _favoriteThemeIds = MutableStateFlow<Set<String>>(setOf())
    val favoriteThemeIds: StateFlow<Set<String>> = _favoriteThemeIds

    private var previousTheme: NusuThemePreset = defaultTheme

    init {
        generateAllThemes()
    }

    private fun generateAllThemes() {
        mutableThemes.clear()
        // Always include default as the very first
        mutableThemes.add(defaultTheme)

        var themeIndex = 1
        for (colIndex in collections.indices) {
            val collection = collections[colIndex]
            for (editIndex in editions.indices) {
                val edition = editions[editIndex]
                
                // Keep default separate or skip matches to keep ID unique
                if (colIndex == 0 && editIndex == 0) continue

                // Round-robin selection of premium color palettes
                val palette = colorPaletts[(colIndex + editIndex) % colorPaletts.size]
                val primaryColor = palette[0]
                val secondaryColor = palette[1]
                val accentColor = palette[2]
                val glowColor = palette[3]

                // Programmatic high fidelity dark background adjustments
                val bgHue = when (colIndex) {
                    2 -> Color(0xFF0C0712) // cyberpunk magenta-black
                    6 -> Color(0xFF0F0A0A) // military red-black
                    18 -> Color(0xFF020C06) // matrix dark state
                    else -> Color(0xFF050816) // deep space dark
                }

                val cardBg = when (colIndex) {
                    2 -> Color(0xFF160F24)
                    6 -> Color(0xFF1C1313)
                    18 -> Color(0xFF081A0D)
                    else -> Color(0xFF081120)
                }

                mutableThemes.add(
                    NusuThemePreset(
                        id = "theme_${colIndex}_${editIndex}",
                        name = "$collection $edition",
                        collectionName = collection,
                        edition = edition,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        accentColor = accentColor,
                        glowColor = glowColor,
                        backgroundColor = bgHue,
                        cardBackgroundColor = cardBg,
                        successColor = Color(0xFF00FF9D),
                        orbInnerColor = secondaryColor,
                        orbOuterColor = primaryColor,
                        typographyStyle = when (colIndex % 3) {
                            0 -> "Space Grotesk"
                            1 -> "Inter"
                            else -> "JetBrains Mono"
                        }
                    )
                )
                themeIndex++
            }
        }
    }

    // Interactive AI design generative suite
    fun generateAITheme(prompt: String): NusuThemePreset {
        val hash = kotlin.math.abs(prompt.hashCode())
        
        // Map random generator choices based on input prompt seed hashes
        val words = prompt.lowercase(Locale.ROOT).split(" ")
        
        var primary = Color(0xFF00FFFF)
        var secondary = Color(0xFF8B5CF6)
        var accent = Color(0xFFFF007F)
        var collectionTitle = "AI Generated Custom"

        if (words.contains("lava") || words.contains("fire") || words.contains("orange") || words.contains("red")) {
            primary = Color(0xFFFF4500)
            secondary = Color(0xFFFF8C00)
            accent = Color(0xFFEF233C)
            collectionTitle = "Lava Core Interface"
        } else if (words.contains("forest") || words.contains("green") || words.contains("nature") || words.contains("matrix")) {
            primary = Color(0xFF00FF87)
            secondary = Color(0xFF60EFFF)
            accent = Color(0xFF10B981)
            collectionTitle = "Emerald Biosphere OS"
        } else if (words.contains("gold") || words.contains("luxury") || words.contains("royal")) {
            primary = Color(0xFFFFD700)
            secondary = Color(0xFFFFA500)
            accent = Color(0xFFF8FAFC)
            collectionTitle = "Royal Zenith Gold"
        } else if (words.contains("dark") || words.contains("mono") || words.contains("stealth")) {
            primary = Color(0xFF94A3B8)
            secondary = Color(0xFF475569)
            accent = Color(0xFF0F172A)
            collectionTitle = "Stealth Obsidian Concept"
        }

        val generated = NusuThemePreset(
            id = "generated_${System.currentTimeMillis()}",
            name = "$collectionTitle Concept",
            collectionName = "AI Studio Generative OS",
            edition = "Personal AI Synthesis",
            primaryColor = primary,
            secondaryColor = secondary,
            accentColor = accent,
            glowColor = primary,
            backgroundColor = Color(0xFF050816),
            cardBackgroundColor = Color(0xFF081120),
            successColor = Color(0xFF00FF9D),
            orbInnerColor = secondary,
            orbOuterColor = primary,
            typographyStyle = "Space Grotesk"
        )

        mutableThemes.add(0, generated) // Prepended to customization center
        return generated
    }

    fun applyTheme(theme: NusuThemePreset) {
        previousTheme = _currentTheme.value
        _currentTheme.value = theme
    }

    fun restorePreviousTheme() {
        val temp = _currentTheme.value
        applyTheme(previousTheme)
        previousTheme = temp
    }

    fun toggleFavorite(themeId: String) {
        val currentFavorites = _favoriteThemeIds.value.toMutableSet()
        if (currentFavorites.contains(themeId)) {
            currentFavorites.remove(themeId)
        } else {
            currentFavorites.add(themeId)
        }
        _favoriteThemeIds.value = currentFavorites
    }
}

val LocalNusuTheme = staticCompositionLocalOf { ThemeManager.defaultTheme }
