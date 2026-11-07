package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.uikit.R as KitR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * A source's passage ends on the card's block break.
 *
 * A closed door keeps 12 dp above and below its head, and the sheet row keeps
 * its own 2 dp card gap outside that, so two closed doors' heads stand about
 * 28 dp apart, the room the card gives two things of one kind. An open passage
 * had no foot, so its last line and the next source's language label stood
 * only the next row's head apart, closer than two closed doors and close
 * enough to read the next language as a caption to the passage. The passage
 * now leaves one more `Space.Row` before the next door, so the card's block
 * break separates the end of a source from the head of the next (owner
 * decision, this session). The gap is measured between the last line's box
 * and the next language's box, not between the row's invisible padding.
 */
@RunWith(AndroidJUnit4::class)
class TafsirSourcesTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            val store = PackStore(context)
            store.install(ENGLISH)
            store.install(BANGLA)
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    // Al-Fatihah 1:1 is a short ayah at the top of the reading,
                    // so the pill is raised without a scroll.
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(setOf(ENGLISH, BANGLA))
                    setShowTranslation(true)
                    setWordByWord(false)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun thePassageLeavesTheBlockBreakBeforeTheNextSource() {
        waitForReading()
        openTheCard()
        val name = compose.activity.getString(KitR.string.pack_name_ibn_kathir)
        // The reader's language leads, so the first door is the English pack
        // and the second is the Bangla one.
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(name, substring = true).fetchSemanticsNodes().size == 2
        }
        compose.onAllNodesWithText(name, substring = true).onFirst().performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag(PANEL).fetchSemanticsNodes().isNotEmpty()
        }

        // The panel's bottom is the last line of the open passage; the next
        // language's top is the head of the source below it. The card's own
        // Space.Row before the next door plus that door's own 14 dp head (its
        // 12 dp row padding and the 2 dp card gap) puts the next source about
        // 26 dp under the passage; the text's own line box adds a fraction.
        val panel = compose.onNodeWithTag(PANEL).getUnclippedBoundsInRoot()
        val next = compose.onAllNodesWithTag("pack-language", useUnmergedTree = true)
            .onLast()
            .getUnclippedBoundsInRoot()
        assertEquals(
            "the passage's end and the next source stand on the block break",
            26f,
            (next.top - panel.bottom).value,
            2f,
        )
    }

    private fun waitForReading() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The card, from the ayah's own door: a tap on the number at its head. */
    private fun openTheCard() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onAllNodesWithContentDescription(page).onFirst().tapThePaper()
        compose.onNodeWithTag("door-1:1").performClick()
        val tafsir = compose.activity.getString(R.string.action_tafsir)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(tafsir).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(tafsir).performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("ayah-card").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ENGLISH = "tafsir-ibn-kathir-en"
        const val BANGLA = "tafsir-ibn-kathir-bn"
        const val PANEL = "tafsir-panel"
    }
}
