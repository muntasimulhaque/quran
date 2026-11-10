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
import io.github.muntasimulhaque.quran.data.TextSize
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
 * - `surface` is a sheet or a panel that covers the reading.
 * - `surfaceContainerHigh` is a control or a menu that floats *over* the
 *   reading with the page still visible around it: the playback pill, the
 *   offer, the pack row, the snackbar, and the popup they open. It sits a
 *   step away from the page, so a floating thing reads as floating without
 *   needing a hard border.
 *
 * The floating control wears [PaperFloating] on the day ground and
 * [NightControl] on the night ground (owner decision, this session). The day
 * ground used to borrow [PaperControl], the switch's track, and that tone is
 * measured against the white sheet: the day page is only 2.8 L* under it, so a
 * 64 dp capsule stood 1.07:1 from the paper and all but disappeared (owner
 * report). The switch's track keeps [PaperControl] in `Switch.kt`, because a
 * track is a groove on the sheet, not a bar over the page.
 *
 * **The day's step is warmth.** The page stands at L* 98.0 with two points of
 * headroom to white, and the block a menu is raised over, `Wash.Selected` at 8
 * percent of the accent, stands at L* 92.9, so the lightness has no room to
 * give and every tone that asked it to do the whole job read wrong: 17.2's
 * darkened sepia as an overlay, 17.3's neutral as a smudge (owner report). The
 * floating paper is the page's own paper now, 4.4 L* down and warmed to three
 * times the page's chroma, so a menu meets that wash as ivory over a cool gray
 * and the edge is a hue at one lightness. `ContrastTest` pins the warmth, the
 * band the step may take, and the distance from that wash, so a later edit
 * cannot bring either fault back. Everything that floats over the reading
 * takes the high role and this tone; a sheet keeps `surface`.
 * - `outline` and `outlineVariant` are the hairlines, never carrying meaning
 *   on their own.
 *
 * `surfaceTint` is transparent in both. Material lifts a raised surface
 * by tinting it toward the primary; on a manuscript that would awake a blue
 * wash under every sheet, so the lift is a chosen tone and a shadow instead.
 *
 * `surfaceContainerHighest` is the floating tone as well, so a Material
 * component that asks for either high container gets the paper the app draws.
 * The switch's track is named in `Switch.kt` and is not read from a scheme
 * role: a control that reads as a control is the one thing a scheme role
 * should not be left to answer for by accident.
 *
 * The error roles are the app's quiet voice in both grounds, because this app
 * has no red. The Quran's own qalaqah is the page's only red, and a red the
 * interface wore would borrow a color the reading uses for a rule of
 * recitation. A failure here is a sentence, the pill's line or the toast,
 * never a hue, so a Material control that asks for an error tone paints the
 * ink it already reads in, and a stock red can never reach the glass (owner
 * decision, this session).
 */
internal val LightScheme = lightColorScheme(
    primary = Lapis,
    onPrimary = PaperOnPrimary,
    primaryContainer = Color(0xFFD3E4F5),
    onPrimaryContainer = Color(0xFF0B2A45),
    secondary = PaperMuted,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE8DD),
    onSecondaryContainer = PaperInk,
    tertiary = PaperMuted,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2E7CE),
    onTertiaryContainer = PaperInk,
    background = PaperBackground,
    onBackground = PaperInk,
    surface = PaperSurface,
    onSurface = PaperInk,
    surfaceVariant = Color(0xFFF4F0E7),
    onSurfaceVariant = PaperMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFCF9),
    surfaceContainer = Color(0xFFF8F5EE),
    surfaceContainerHigh = PaperFloating,
    surfaceContainerHighest = PaperFloating,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperInk,
    inverseOnSurface = PaperSurface,
    inversePrimary = LapisLight,
    error = PaperInk,
    onError = PaperSurface,
    errorContainer = PaperControl,
    onErrorContainer = PaperInk,
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
    secondaryContainer = Color(0xFF3A352E),
    onSecondaryContainer = NightText,
    tertiary = NightMuted,
    onTertiary = Color(0xFF141210),
    tertiaryContainer = Color(0xFF3D3424),
    onTertiaryContainer = NightText,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = Color(0xFF2C2925),
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = Color(0xFF191715),
    surfaceContainerLow = Color(0xFF201E1C),
    surfaceContainer = Color(0xFF282522),
    surfaceContainerHigh = NightControl,
    surfaceContainerHighest = NightControl,
    surfaceTint = Color.Transparent,
    inverseSurface = NightText,
    inverseOnSurface = NightBackground,
    inversePrimary = Lapis,
    error = NightText,
    onError = NightBackground,
    errorContainer = NightControl,
    onErrorContainer = NightText,
    outline = NightOutline,
    outlineVariant = NightOutline,
    scrim = Color(0xFF000000),
)

/** The reading colors, which turn over with the theme. */
internal val LightPage = PagePalette(
    ink = PaperReadingInk,
    ornament = Gold,
    // 0.13 of the lapis: the strongest tint the seven paper families keep
    // their 4.5:1 on. The reciter's word and a search arrival both wear it.
    highlight = Color(0x211B4D7A),
    tajweed = PaperTajweed,
    introInk = PaperInk.copy(alpha = 0.78f),
    mastheadInk = PaperInk.copy(alpha = 0.9f),
)

internal val DarkPage = PagePalette(
    ink = NightInk,
    ornament = NightGold,
    // 0.15 of the night lapis: the same ceiling for the night families.
    highlight = Color(0x2695C0EA),
    tajweed = NightTajweed,
    introInk = NightInk.copy(alpha = 0.78f),
    mastheadInk = NightText.copy(alpha = 0.9f),
)

val LocalPagePalette = staticCompositionLocalOf { LightPage }

/**
 * The theme is the whole app: the two grounds, and the voice everything
 * is written in. The voice follows the reader's one language choice: English
 * reads Literata and works in Inter, Bangla reads Noto Serif Bengali and
 * works in Noto Sans Bengali, and the switch is made here so no surface picks
 * a face of its own. The reading voice carries the language's own locale as
 * well, so the hyphenation dictionary and the script's local forms follow the
 * content and never the phone (owner decision, this session).
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
            UiLanguage.Bangla -> ReadingVoice(
                BengaliReading,
                TextSize.BENGALI_PROSE_RATIO,
                1.594f,
                // Bengali's title box is the script's own: its matras and
                // conjuncts need 1.6, where the Latin title can sit at 1.3.
                1.6f,
                { NotoSerifBengali },
                BanglaReadingLocale,
            )
            UiLanguage.English -> ReadingVoice(
                LatinReading,
                TextSize.LATIN_PROSE_RATIO,
                1.485f,
                TypeLadder.TITLE_LINE,
                ::literataAt,
                EnglishReadingLocale,
            )
        },
    ) {
        val typography = when (language) {
            UiLanguage.Bangla -> BanglaTypography
            UiLanguage.English -> quranTypography()
        }
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
