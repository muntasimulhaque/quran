package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.DownloadedSurah
import io.github.muntasimulhaque.quran.data.PackType
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.settings.PackChoiceRow
import io.github.muntasimulhaque.quran.ui.settings.SettingsActions
import io.github.muntasimulhaque.quran.ui.settings.SizeRow
import io.github.muntasimulhaque.quran.ui.settings.WordsPage
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The controls, photographed for review.
 *
 * The five cell row was four 250 dp pills down the page; the rail is one
 * line per kind of text, so the whole ladder has to be seen at the page's
 * width before it is believed. The pack frame is the other half: the two
 * states of one row, so the off ring, the check, and the two action pills can
 * be read side by side at the phone's width. Both draw in the light theme and
 * write to the build's screenshot directory, the same way the type specimen
 * does: they are review frames, not store frames, and they change no setting.
 */
@RunWith(AndroidJUnit4::class)
class ControlsCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTheFourSizeRails() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) { SizeRails() }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "size-rails.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Test
    fun captureTheTwoPackRowStates() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 24.dp),
                ) {
                    PackChoiceRow(
                        pack = absentBayan(),
                        subtitle = "5.4 MB",
                        selected = false,
                        radio = false,
                        setup = null,
                        onActivate = {},
                        onInstall = {},
                        onRemove = {},
                        onCancel = {},
                    )
                    PackChoiceRow(
                        pack = presentSaheeh(),
                        subtitle = "2.8 MB",
                        selected = true,
                        radio = false,
                        setup = null,
                        onActivate = {},
                        onInstall = {},
                        onRemove = {},
                        onCancel = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "pack-rows.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    /**
     * The words page after this session: the feature's name on the switch,
     * the gesture as its lead, the one list as a file row, and the
     * pronunciation behind the reciter page's door with a row per surah.
     */
    @Test
    fun captureTheWordsPage() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 24.dp),
                ) {
                    WordsPage(
                        settings = AppSettings(uiLanguage = "en", wordByWord = true),
                        packs = listOf(wordsPack()),
                        packSetup = null,
                        actions = SettingsActions(),
                        wordVoices = {
                            listOf(
                                DownloadedSurah(surah = 1, name = "Al-Fatihah", bytes = 208_000),
                                DownloadedSurah(surah = 2, name = "Al-Baqarah", bytes = 39_845_888),
                            )
                        },
                    )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Downloaded pronunciation (2)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Downloaded pronunciation (2)").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Al-Baqarah").fetchSemanticsNodes().isNotEmpty()
        }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "words-page.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun wordsPack() = ContentPack(
        id = "words-en",
        type = PackType.Words,
        name = "Word meanings",
        language = "en",
        credit = "test",
        license = "test",
        version = "1",
        shipped = false,
        ayahs = 6236,
        bytes = 6_303_744,
        installed = true,
    )

    private fun absentBayan() = ContentPack(
        id = "translation-al-bayan-bn",
        type = PackType.Translation,
        name = "Al-Bayan Simple Translation",
        language = "bn",
        credit = "test",
        license = "test",
        version = "1.0.1",
        shipped = false,
        ayahs = 6236,
        bytes = 5_652_480,
        installed = false,
    )

    private fun presentSaheeh() = ContentPack(
        id = "translation-saheeh-en",
        type = PackType.Translation,
        name = "Saheeh International",
        language = "en",
        credit = "test",
        license = "test",
        version = "1.1.2+2",
        shipped = false,
        ayahs = 6236,
        bytes = 2_936_013,
        installed = true,
    )

    @Composable
    private fun SizeRails() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp),
        ) {
            TypeRole.entries.forEach { role ->
                SizeRow(role = role, step = TextSize.DEFAULT, onChange = {})
            }
        }
    }
}
