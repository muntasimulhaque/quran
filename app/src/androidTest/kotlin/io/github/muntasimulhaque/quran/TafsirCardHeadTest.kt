package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.uikit.R as KitR
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * One tafsir is a page, not a list of one.
 *
 * With a single pack the card names the kind once and the pack once, and the
 * passage's own reference heads its prose. There is no door to fold, so the
 * name is a byline and never a control, and the reference belongs to the
 * passage instead of the sheet (owner decision, this session). The two-pack
 * case is the doors' own case, pinned by the tour's card frames on all three
 * form factors; this test pins the common one.
 *
 * The card is opened the way a reader opens it, from the ayah's actions, not
 * by composing it in isolation: the claim is about the surface the app draws.
 */
@RunWith(AndroidJUnit4::class)
class TafsirCardHeadTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            PackStore(context).install(PACK_ID)
            LanguagePreference(context).set("en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    // Al-Fatihah 1:1 is a short ayah at the top of the reading,
                    // so the pill is raised without a scroll.
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(setOf(PACK_ID))
                    setShowTranslation(true)
                    setWordByWord(false)
                    // The first-run long-press note belongs to a new install,
                    // not to the head this test reads.
                    setLongPressHintShown()
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun oneTafsirNamesItselfOnceAndHeadsItsProseWithItsOwnReference() {
        waitForReading()
        openTheCard()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag(PANEL).fetchSemanticsNodes().isNotEmpty()
        }

        val pack = compose.activity.getString(KitR.string.pack_name_ibn_kathir)
        val label = compose.activity.getString(ReadingR.string.card_tafsir_label)
        val reference = compose.activity.getString(
            ReadingR.string.card_tafsir_reference,
            SURAH,
            "1:1",
        )

        // The name is a byline: one node, and it answers no touch.
        compose.onAllNodesWithText(pack).assertCountEquals(1)
        compose.onAllNodes(hasText(pack) and hasClickAction()).assertCountEquals(0)

        // The sheet is named by the kind of text, and the passage by its own
        // reference, both words a screen reader can jump between.
        compose.onNode(hasText(label) and isHeading()).assertExists()
        compose.onNode(hasText(reference) and isHeading()).assertExists()
    }

    private fun waitForReading() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The card, from the ayah's own actions: a tap on the paper, then a press. */
    private fun openTheCard() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.onAllNodesWithContentDescription(page).onFirst().tapThePaper()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText("1:1", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("1:1", substring = true).onFirst()
            .performTouchInput { longClick() }
        val tafsir = compose.activity.getString(R.string.action_tafsir)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithContentDescription(tafsir)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(tafsir).performClick()
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithTag("ayah-card").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val PACK_ID = "tafsir-ibn-kathir-en"
        const val PANEL = "tafsir-panel"
        const val SURAH = "Al-Fatihah"
    }
}
