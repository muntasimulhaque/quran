package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ReaderTheme { Paper, Sepia, Night, Black }

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
)

@Composable
fun QuranTheme(theme: ReaderTheme = ReaderTheme.Paper, content: @Composable () -> Unit) {
    val colors = when (theme) {
        ReaderTheme.Paper -> PaperScheme
        ReaderTheme.Sepia -> SepiaScheme
        ReaderTheme.Night -> NightScheme
        ReaderTheme.Black -> BlackScheme
    }
    MaterialTheme(colorScheme = colors, typography = QuranTypography, content = content)
}
