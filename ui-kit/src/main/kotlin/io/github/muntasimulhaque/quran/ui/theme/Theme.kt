package io.github.muntasimulhaque.quran.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.resolved

/**
 * The two grounds, in one place, each one fully spoken for.
 *
 * Every role Material draws from is set here rather than left to its default.
 * A default that falls through is a color nobody chose, and on a page whose
 * ink was chosen to the point, an unchosen tone is the one thing that reads
 * as wrong. The names are Material's, so a control that asks for a container
 * gets the tone this app meant by one:
 *
 * - `background` is the reading itself, the page or the reading paper.
 * - `surface` is anything that covers the reading: a sheet, a panel.
 * - `surfaceContainerHigh` is a control that floats *over* the reading with
 *   the page still visible around it: the ayah pill, the playback bar. It
 *   sits a step away from both the page and the sheet, so a floating thing
 *   reads as floating without needing a hard border.
 * - `outline` and `outlineVariant` are the hairlines, never carrying meaning
 *   on their own.
 *
 * `surfaceTint` is transparent in both. Material lifts a raised surface
 * by tinting it toward the primary; on a manuscript that would awake a blue
 * wash under every sheet, so the lift is a chosen tone and a shadow instead.
 *
 * `surfaceContainerHighest` is the ground a switch's own track is filled
 * with, and it is named with the switch in `Switch.kt`: a control that reads
 * as a control is the one thing a scheme role should not be left to answer
 * for by accident.
 */
internal val LightScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = PaperOnPrimary,
    primaryContainer = Color(0xFFD3E4F5),
    onPrimaryContainer = Color(0xFF0B2A45),
    secondary = PaperMuted,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = PaperMuted,
    onTertiary = Color(0xFFFFFFFF),
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = Color(0xFFF4F0E7),
    onSurfaceVariant = PaperMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFCF9),
    surfaceContainer = Color(0xFFF8F5EE),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = PaperControl,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperInk,
    inverseOnSurface = PaperSurface,
    inversePrimary = LapisLight,
    outline = PaperOutline,
    outlineVariant = PaperOutline,
    scrim = Color(0xFF14130F),
)

internal val DarkScheme = darkColorScheme(
    primary = LapisLight,
    onPrimary = NightOnPrimary,
    primaryContainer = Color(0xFF24405C),
    onPrimaryContainer = Color(0xFFD3E4F5),
    secondary = NightMuted,
    onSecondary = Color(0xFF141210),
    tertiary = NightMuted,
    onTertiary = Color(0xFF141210),
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = Color(0xFF2C2925),
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF191715),
    surfaceContainerLow = Color(0xFF201E1C),
    surfaceContainer = Color(0xFF282522),
    surfaceContainerHigh = Color(0xFF2E2B28),
    surfaceContainerHighest = NightControl,
    surfaceTint = Color.Transparent,
    inverseSurface = NightText,
    inverseOnSurface = NightBackground,
    inversePrimary = Lapis,
    outline = NightOutline,
    outlineVariant = NightOutline,
    scrim = Color(0xFF000000),
)

/** The reading colors, which turn over with the theme. */
internal val LightPage = PagePalette(
    ink = PaperInk,
    ornament = Gold,
    highlight = Color(0x381B4D7A),
    tajweed = PaperTajweed,
)

internal val DarkPage = PagePalette(
    ink = NightInk,
    ornament = NightGold,
    highlight = Color(0x4795C0EA),
    tajweed = NightTajweed,
)

val LocalPagePalette = staticCompositionLocalOf { LightPage }

/**
 * The theme is the whole app: the two grounds, and the voice everything
 * is written in. The voice follows the reader's one language choice: English
 * speaks Inter and Literata, Bangla speaks Noto Sans Bengali and Noto Serif
 * Bengali, and the switch is made here so no surface picks a face of its own.
 *
 * The ground may be handed as any of the three values, System among them:
 * System is resolved here against the phone's own day and night, so a caller
 * that passes the reader's raw choice still gets one ground drawn. The shell
 * resolves it too, for the status and navigation bars, and hands Light or
 * Dark, which resolve to themselves.
 */
@Composable
fun QuranTheme(
    theme: AppTheme = AppTheme.Light,
    language: UiLanguage = UiLanguage.English,
    content: @Composable () -> Unit,
) {
    // System becomes the phone's own ground; Light and Dark are already one,
    // so `shown` is only ever Light or Dark and the `else` below is the light
    // ground (owner decision, this session).
    val shown = theme.resolved(isSystemInDarkTheme())
    val colors = when (shown) {
        AppTheme.Dark -> DarkScheme
        else -> LightScheme
    }
    val palette = when (shown) {
        AppTheme.Dark -> DarkPage
        else -> LightPage
    }
    val switches = when (shown) {
        AppTheme.Dark -> DarkSwitch
        else -> LightSwitch
    }
    CompositionLocalProvider(
        LocalPagePalette provides palette,
        LocalSwitchPalette provides switches,
        LocalReadingVoice provides when (language) {
            UiLanguage.Bangla -> ReadingVoice(BengaliReading, 1.706f, { NotoSerifBengali })
            UiLanguage.English -> ReadingVoice(LatinReading, 1.6f, ::literataAt)
        },
    ) {
        val typography = when (language) {
            UiLanguage.Bangla -> BanglaTypography
            UiLanguage.English -> quranTypography()
        }
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
