package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The share sheet's preview holds the whole card, not a scrollable slice.
 *
 * The owner's Al-Hijr 15:22 card stood taller than the old 340 dp viewport, so
 * the preview cut its translation and asked the reader to scroll a picture
 * they were about to send. This test opens that card from the real reading,
 * waits for the two doors, and keeps the frame the change is reviewed from.
 * The preview is one scaled picture above the doors; both doors must still be
 * on the glass at the same time.
 */
@RunWith(AndroidJUnit4::class)
class ShareSheetPreviewTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            PackStore(context).install(TRANSLATION)
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    // Al-Hijr 15:22, global ayah 1824, the card the owner
                    // shared when the preview cut its translation.
                    setAyah(1824)
                    setTranslationPacks(setOf(TRANSLATION))
                    setShowTranslation(true)
                    setWordByWord(false)
                    setTafsirPacks(emptySet())
                }
            }
        }

        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theWholeMediumCardAndBothDoorsAreOnTheGlass() {
        openShareSheet()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.share_preview))
            .assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.share_image))
            .assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.share_text))
            .assertIsDisplayed()

        compose.waitForIdle()
        val frame = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "share-sheet-preview.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun openShareSheet() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(page).onFirst().tapThePaper()
        compose.onNodeWithTag("ayah-15:22").performClick()
        val share = compose.activity.getString(R.string.action_share)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(share).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(share).performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("share-sheet").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TRANSLATION = "translation-saheeh-en"
    }
}
