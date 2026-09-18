package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.ui.mushaf.PagePalette

private val PaperScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = PaperSurface,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = PaperSurface,
    onSurfaceVariant = PaperMuted,
    outline = PaperHairline,
    outlineVariant = PaperHairline,
)

private val SepiaScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = SepiaSurface,
    background = SepiaBackground,
    onBackground = SepiaInk,
    surface = SepiaSurface,
    onSurface = SepiaInk,
    surfaceVariant = SepiaSurface,
    onSurfaceVariant = SepiaMuted,
    outline = SepiaHairline,
    outlineVariant = SepiaHairline,
)

private val NightScheme = darkColorScheme(
    primary = LapisLight,
    onPrimary = NightBackground,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightMuted,
    outline = NightHairline,
    outlineVariant = NightHairline,
)

private val BlackScheme = darkColorScheme(
    primary = LapisLight,
    onPrimary = BlackBackground,
    background = BlackBackground,
    onBackground = BlackText,
    surface = BlackSurface,
    onSurface = BlackText,
    surfaceVariant = BlackSurface,
    onSurfaceVariant = BlackMuted,
    outline = BlackHairline,
    outlineVariant = BlackHairline,
)

/** The Mushaf page colors, which turn over with the theme. */
private val PaperPage = PagePalette(PaperBackground, PaperInk, Gold)
private val SepiaPage = PagePalette(SepiaBackground, SepiaInk, SepiaGold)
private val NightPage = PagePalette(NightBackground, NightInk, NightGold)
private val BlackPage = PagePalette(BlackBackground, BlackInk, BlackGold)

/** The wash under a selected ayah, and the one that follows the reciter. */
val MushafSelection = Color(0x1F1B4D7A)
val MushafHighlight = Color(0xFF2E6FA8)

val LocalPagePalette = staticCompositionLocalOf { PaperPage }
val LocalPageThemeName = staticCompositionLocalOf { "paper" }

@Composable
fun QuranTheme(theme: AppTheme = AppTheme.Paper, content: @Composable () -> Unit) {
    val colors = when (theme) {
        AppTheme.Paper -> PaperScheme
        AppTheme.Sepia -> SepiaScheme
        AppTheme.Night -> NightScheme
        AppTheme.Black -> BlackScheme
    }
    val palette = when (theme) {
        AppTheme.Paper -> PaperPage
        AppTheme.Sepia -> SepiaPage
        AppTheme.Night -> NightPage
        AppTheme.Black -> BlackPage
    }
    val name = when (theme) {
        AppTheme.Paper -> "paper"
        AppTheme.Sepia -> "sepia"
        AppTheme.Night -> "night"
        AppTheme.Black -> "black"
    }
    CompositionLocalProvider(
        LocalPagePalette provides palette,
        LocalPageThemeName provides name,
    ) {
        MaterialTheme(colorScheme = colors, typography = QuranTypography, content = content)
    }
}
