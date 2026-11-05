package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.playback.PlaybackUiState
import io.github.muntasimulhaque.quran.ui.reader.ReaderTopBar
import io.github.muntasimulhaque.quran.ui.playback.PlaybackBar
import io.github.muntasimulhaque.quran.ui.settings.DailyPage
import io.github.muntasimulhaque.quran.ui.settings.SettingsHub
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test

/**
 * The app at twice the system text size, the size Android's own accessibility
 * setting can reach.
 *
 * The surfaces are composed at the density a 200 percent reader has, not at
 * the app's own largest step: the two are different ladders, and the fixed
 * shapes around the type (the top bar, a settings row, the pill, the time
 * dialog) are where a doubled word can run out of room. The test does not
 * pretend to measure every glyph; it keeps the controls reachable and on the
 * glass, which is the failure that matters, and the capture run can show the
 * type itself (owner decision, this session).
 */
class LargeTextTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theTopBarKeepsItsDoorsAtTwoHundredPercent() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                AtFontScale(2f) {
                    ReaderTopBar(
                        title = "Al-Baqarah",
                        detail = "Juz 3",
                        visible = true,
                        reducedMotion = true,
                        titleAction = "Back to the top of the surah",
                        onTitle = {},
                        onBrowse = {},
                        onSearch = {},
                        onSettings = {},
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Browse the Quran").assertIsDisplayed()
        compose.onNodeWithContentDescription("Search").assertIsDisplayed()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithText("Al-Baqarah").assertExists()
    }

    @Test
    fun theSettingsHubKeepsEveryRowAtTwoHundredPercent() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                AtFontScale(2f) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SettingsHub(
                            settings = AppSettings(),
                            packs = emptyList(),
                            recitations = emptyList(),
                            version = "14.7",
                            notificationsBlocked = false,
                            exactGrantWithheld = false,
                            packSetup = null,
                            onOpen = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Language").assertExists()
        compose.onNodeWithText("About").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun thePlaybackPillKeepsItsTransportAtTwoHundredPercent() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                AtFontScale(2f) {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        PlaybackBar(
                            state = PlaybackUiState(
                                connected = true,
                                isPlaying = true,
                                recitation = "husary",
                                ayahNumber = 262,
                                surah = 2,
                                reference = "2:255",
                                placeName = "Al-Baqarah",
                            ),
                            offer = null,
                            offerTitle = "",
                            reciterName = "Husary",
                            reference = "2:255",
                            placeName = "Al-Baqarah",
                            pendingLabel = null,
                            onToggle = {},
                            onNext = {},
                            onPrevious = {},
                            onReciter = {},
                            onDownload = {},
                            onStop = {},
                            onCancelDownload = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Play or pause").assertIsDisplayed()
        compose.onNodeWithContentDescription("Previous ayah").assertIsDisplayed()
        compose.onNodeWithContentDescription("Next ayah").assertIsDisplayed()
    }

    @Test
    fun theDailyTimeDialogKeepsItsAnswersAtTwoHundredPercent() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                AtFontScale(2f) {
                    Box(Modifier.fillMaxSize()) {
                        DailyPage(
                            settings = AppSettings(dailyAyah = true),
                            notificationsBlocked = false,
                            exactGrantWithheld = false,
                            onDailyAyah = {},
                            onDailyAyahTime = {},
                            onOpenNotificationSettings = {},
                            onAskExactAlarm = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Time").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Set").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Set").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }
}

/** The density a reader who chose 200 percent text actually has. */
@Composable
private fun AtFontScale(scale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, scale),
        content = content,
    )
}
