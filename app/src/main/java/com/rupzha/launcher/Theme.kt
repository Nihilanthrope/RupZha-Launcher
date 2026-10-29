package com.rupzha.launcher

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

val Ink = Color(0xFF0B1030)
val Navy = Color(0xFF141B3D)
val Cream = Color(0xFFF6EBDD)
val Peach = Color(0xFFFFB59A)
val Coral = Color(0xFFFF8F7A)
val Lavender = Color(0xFFB8A6F2)

/**
 * "Paper" colors are the illustrated cards that sit on the background. Text on top of them uses [onPaper].
 */
@Immutable
class Palette(
    val bg: Color,
    val bgDeep: Color,
    val surface: Color,
    val paperA: Color,
    val paperB: Color,
    val paperC: Color,
    val text: Color,
    val textDim: Color,
    val onPaper: Color,
    val onPaperDim: Color,
    val accent: Color,
    val onAccent: Color,
    val outline: Color,
    val shadow: Color,
    val dark: Boolean,
)

private fun darkPalette(bg: Color, accent: Color) = Palette(
    bg = bg,
    bgDeep = lerp(bg, Color.Black, 0.35f),
    surface = lerp(bg, Color.White, 0.08f),
    paperA = Cream,
    paperB = Color(0xFFCDBFF7),
    paperC = Color(0xFFFFC9B3),
    text = Cream,
    textDim = Color(0xFFB9B4D6),
    onPaper = Navy,
    onPaperDim = Color(0xFF5A5F86),
    accent = accent,
    onAccent = Ink,
    outline = Ink,
    shadow = Color(0x73000000),
    dark = true,
)

private fun lightPalette(bg: Color, accent: Color) = Palette(
    bg = bg,
    bgDeep = lerp(bg, Ink, 0.12f),
    surface = lerp(bg, Color.White, 0.45f),
    paperA = Color(0xFFFFF8EE),
    paperB = Color(0xFFE4DAFF),
    paperC = Color(0xFFFFDCCB),
    text = Navy,
    textDim = Color(0xFF5A5F86),
    onPaper = Navy,
    onPaperDim = Color(0xFF5A5F86),
    accent = accent,
    onAccent = Ink,
    outline = Ink,
    shadow = Color(0x470B1030),
    dark = false,
)

val NavyBg = Color(0xFF121A3A)

@Composable
fun rupzhaPalette(s: Settings): Palette {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    return remember(s.theme, s.accent, dark) {
        when (s.theme) {
            ThemeId.NAVY -> darkPalette(NavyBg, Peach)
            ThemeId.CREAM -> lightPalette(Cream, Coral)
            ThemeId.LAVENDER -> lightPalette(Color(0xFFD9CCF6), Color(0xFF7C5CE0))
            ThemeId.PEACH -> lightPalette(Color(0xFFFFD2BF), Color(0xFFFF7A66))
            ThemeId.CUSTOM -> {
                val a = Color(s.accent)
                darkPalette(lerp(NavyBg, a, 0.14f), a)
            }
            ThemeId.DYNAMIC ->
                if (Build.VERSION.SDK_INT >= 31) {
                    val cs = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
                    Palette(
                        bg = cs.surface, bgDeep = cs.surfaceVariant, surface = cs.surfaceVariant,
                        paperA = cs.primaryContainer, paperB = cs.secondaryContainer, paperC = cs.tertiaryContainer,
                        text = cs.onSurface, textDim = cs.onSurfaceVariant,
                        onPaper = cs.onPrimaryContainer, onPaperDim = cs.onPrimaryContainer.copy(alpha = 0.7f),
                        accent = cs.primary, onAccent = cs.onPrimary,
                        outline = Ink, shadow = if (dark) Color(0x73000000) else Color(0x470B1030), dark = dark,
                    )
                } else darkPalette(NavyBg, Peach)
        }
    }
}

val LocalPalette = staticCompositionLocalOf { darkPalette(NavyBg, Peach) }
val LocalSettings = staticCompositionLocalOf { Settings() }

val AccentChoices = listOf(
    Peach, Coral, Lavender, Color(0xFF9ED36A), Color(0xFF5BB8F5), Color(0xFFFFC95C), Color(0xFFFF8FB1), Color(0xFF5FD3A5),
)
