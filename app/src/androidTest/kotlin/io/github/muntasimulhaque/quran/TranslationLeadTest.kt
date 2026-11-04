package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The reading and the Font size sample lead with the reader's own language.
 *
 * The catalog sorts translations by pack name, so Al-Bayan stands before
 * Saheeh. Before this rule the sample drew Al-Bayan under an English
 * interface and the reading drew it first: one Bangla column with no name on
 * the sample, and the Bangla line at the head of the reading (owner report,
 * this session).
 */
@RunWith(AndroidJUnit4::class)
class TranslationLeadTest {

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
                    setAyah(1)
                    setTranslationPacks(setOf(ENGLISH, BANGLA))
                    setShowTranslation(true)
                    setWordByWord(false)
                }
            }
        }

        override fun after() {
            // The settings file is shared by the classes in a run: leave the
            // packs off and the reading showing its translation again, so a
            // later leg does not read this one's choice (AGENTS, the
            // instrumented classes share one process).
            runBlocking {
                SettingsStore(context).apply {
                    setTranslationPacks(emptySet())
                    setShowTranslation(true)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theSampleLeadsWithTheReadersLanguage() {
        openSettings()
        compose.onNodeWithTag("row-text").performClick()
        // The sample's name is the one under the steps; the reading behind
        // the sheet has a name of its own, so the tag scopes this to the
        // sample and not to every window on the glass.
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("size-sample-name", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("size-sample-name", useUnmergedTree = true)
            .assertTextEquals(SAHEEH)
    }

    @Test
    fun theReadingLeadsWithTheReadersLanguage() {
        waitForReading()
        waitForText(SAHEEH)
        waitForText(AL_BAYAN)
        // The unmerged tree is the name that belongs to each column: the
        // merged ayah block carries both names in one node, so its bounds are
        // the block's and say nothing about the order.
        val english = compose.onAllNodesWithText(SAHEEH, useUnmergedTree = true)
            .onFirst()
            .getUnclippedBoundsInRoot()
        val bangla = compose.onAllNodesWithText(AL_BAYAN, useUnmergedTree = true)
            .onFirst()
            .getUnclippedBoundsInRoot()
        assertTrue(
            "the reader's own translation stands above the other language",
            english.top < bangla.top,
        )
    }

    private fun waitForReading() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The reading, the chrome, Settings, and the hub. */
    private fun openSettings() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(page).onFirst().tapThePaper()
        val settings = compose.activity.getString(R.string.action_settings)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(settings).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(settings).performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ENGLISH = "translation-saheeh-en"
        const val BANGLA = "translation-al-bayan-bn"
        const val SAHEEH = "Saheeh International"
        const val AL_BAYAN = "Al-Bayan Simple Translation"
    }
}
