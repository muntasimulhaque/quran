package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.data.AppTheme

/**
 * The four grounds, in one place, each one fully spoken for.
 *
 * Every role Material draws from is set here rather than left to its default.
 * A default that falls through is a color nobody chose, and on a page whose
 * ink was chosen to the point, an unchosen tone is the one thing that reads
 * as wrong. The names are Material's, so a control that asks for a container
 * gets the tone this app meant by one:
 *
 * - `background` is the reading itself, the page or the study paper.
 * - `surface` is anything that covers the reading: a sheet, a panel.
 * - `surfaceContainerHigh` is a control that floats *over* the reading with
 *   the page still visible around it: the ayah pill, the playback bar. It
 *   sits a step away from both the page and the sheet, so a floating thing
 *   reads as floating without needing a hard border.
 * - `outline` and `outlineVariant` are the hairlines, never carrying meaning
 *   on their own.
 *
 * `surfaceTint` is transparent in all four. Material lifts a raised surface
 * by tinting it toward the primary; on a manuscript that would awake a blue
 * wash under every sheet, so the lift is a chosen tone and a shadow instead.
 */
private val PaperScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = Color(0xFFFDFBF6),
    primaryContainer = Color(0xFFD3E4F5),
    onPrimaryContainer = Color(0xFF0B2A45),
    secondary = PaperMuted,
    onSecondary = Color(0xFFFDFBF6),
    tertiary = PaperMuted,
    onTertiary = Color(0xFFFDFBF6),
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = Color(0xFFF1ECE1),
    onSurfaceVariant = PaperMuted,
    surfaceContainerLowest = Color(0xFFFFFEFA),
    surfaceContainerLow = Color(0xFFFBF8F1),
    surfaceContainer = Color(0xFFF7F3EA),
    surfaceContainerHigh = Color(0xFFFFFEFB),
    surfaceContainerHighest = Color(0xFFFFFFFF),
    surfaceTint = Color.Transparent,
    inverseSurface = PaperInk,
    inverseOnSurface = PaperSurface,
    inversePrimary = LapisLight,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    scrim = Color(0xFF14130F),
)

private val SepiaScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = Color(0xFFFDF8ED),
    primaryContainer = Color(0xFFE7D9BE),
    onPrimaryContainer = Color(0xFF1C3A56),
    secondary = SepiaMuted,
    onSecondary = Color(0xFFFDF8ED),
    tertiary = SepiaMuted,
    onTertiary = Color(0xFFFDF8ED),
    background = SepiaBackground,
    onBackground = SepiaInk,
    surface = SepiaSurface,
    onSurface = SepiaInk,
    surfaceVariant = Color(0xFFEDE1CB),
    onSurfaceVariant = SepiaMuted,
    surfaceContainerLowest = Color(0xFFFDF6EA),
    surfaceContainerLow = Color(0xFFF7EEDE),
    surfaceContainer = Color(0xFFF2E8D5),
    surfaceContainerHigh = Color(0xFFFCF5E8),
    surfaceContainerHighest = Color(0xFFFEF9F0),
    surfaceTint = Color.Transparent,
    inverseSurface = SepiaInk,
    inverseOnSurface = SepiaSurface,
    inversePrimary = LapisLight,
    outline = SepiaOutline,
    outlineVariant = SepiaOutline,
    scrim = Color(0xFF241B0E),
)

private val NightScheme = darkColorScheme(
    primary = LapisLight,
    onPrimary = Color(0xFF08243D),
    primaryContainer = Color(0xFF24405C),
    onPrimaryContainer = Color(0xFFD3E4F5),
    secondary = NightMuted,
    onSecondary = Color(0xFF0B1219),
    tertiary = NightMuted,
    onTertiary = Color(0xFF0B1219),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = Color(0xFF1E2A38),
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF0A0F16),
    surfaceContainerLow = Color(0xFF121B25),
    surfaceContainer = Color(0xFF17222E),
    surfaceContainerHigh = Color(0xFF1E2B39),
    surfaceContainerHighest = Color(0xFF263443),
    surfaceTint = Color.Transparent,
    inverseSurface = NightText,
    inverseOnSurface = NightBackground,
    inversePrimary = Lapis,
    outline = NightOutline,
    outlineVariant = NightOutline,
    scrim = Color(0xFF000000),
)

private val BlackScheme = darkColorScheme(
    primary = LapisLight,
    onPrimary = Color(0xFF061A2C),
    primaryContainer = Color(0xFF1E3448),
    onPrimaryContainer = Color(0xFFD3E4F5),
    secondary = BlackMuted,
    onSecondary = Color(0xFF05080B),
    tertiary = BlackMuted,
    onTertiary = Color(0xFF05080B),
    background = BlackBackground,
    onBackground = BlackText,
    surface = BlackSurface,
    onSurface = BlackText,
    surfaceVariant = Color(0xFF141C26),
    onSurfaceVariant = BlackMuted,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080C11),
    surfaceContainer = Color(0xFF0D131A),
    surfaceContainerHigh = Color(0xFF151E28),
    surfaceContainerHighest = Color(0xFF1D2834),
    surfaceTint = Color.Transparent,
    inverseSurface = BlackText,
    inverseOnSurface = BlackBackground,
    inversePrimary = Lapis,
    outline = BlackOutline,
    outlineVariant = BlackOutline,
    scrim = Color(0xFF000000),
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
