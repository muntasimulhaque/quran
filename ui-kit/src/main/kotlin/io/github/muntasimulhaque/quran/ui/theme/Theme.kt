package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.data.AppTheme

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
private val PaperPage = PagePalette(
    paper = PaperBackground,
    ink = PaperInk,
    ornament = Gold,
    selection = Color(0x1F1B4D7A),
    highlight = Color(0x381B4D7A),
)

private val SepiaPage = PagePalette(
    paper = SepiaBackground,
    ink = SepiaInk,
    ornament = SepiaGold,
    selection = Color(0x221B4D7A),
    highlight = Color(0x3D1B4D7A),
)

private val NightPage = PagePalette(
    paper = NightBackground,
    ink = NightInk,
    ornament = NightGold,
    selection = Color(0x2E7FB2E5),
    highlight = Color(0x477FB2E5),
)

private val BlackPage = PagePalette(
    paper = BlackBackground,
    ink = BlackInk,
    ornament = BlackGold,
    selection = Color(0x337FB2E5),
    highlight = Color(0x4D7FB2E5),
)

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
