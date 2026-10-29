package io.github.muntasimulhaque.quran

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.core.localizedDigits
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.feature.settings.R as SettingsR
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.Locale

/**
 * About speaks the reader's own language to the end of the page.
 *
 * The page used to name the core text "Quran text" and print every pack's
 * credit and license from the English catalog beside its Bangla labels, and
 * the version wore Latin digits (owner report). This opens it in a Bangla
 * interface and reads the version, the core pack's own name, and its credit
 * back in Bangla.
 */
@RunWith(AndroidJUnit4::class)
class AboutLanguageTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            LanguagePreference(context).set("bn")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("bn")
                    setAyah(1)
                }
            }
        }

        override fun after() {
            LanguagePreference(context).set("en")
            runBlocking { SettingsStore(context).setUiLanguage("en") }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun thePageNamesTheCoreTextAndItsCreditInBangla() {
        openAbout()

        // The version's digits are the reader's own, on the page's one line
        // about the app itself.
        val version = localizedDigits(BuildConfig.VERSION_NAME, Locale.forLanguageTag("bn"))
        compose.onNodeWithText(version, substring = true).assertExists()

        assertTrue(
            "the core text must be named in Bangla",
            compose.onAllNodesWithText("\u0995\u09c1\u09b0\u0986\u09a8\u09c7\u09b0 \u09b2\u09c7\u0996\u09be", substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "the core pack's credit must be in Bangla",
            compose.onAllNodesWithText("QPC \u09b9\u09be\u09ab\u09b8, \u09b6\u09ac\u09cd\u09a6\u09c7 \u09b6\u09ac\u09cd\u09a6\u09c7", substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "the catalog's English name must not stand beside the Bangla label",
            compose.onAllNodesWithText("Quran text", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "the catalog's English credit must not stand beside the Bangla one",
            compose.onAllNodesWithText("QPC Hafs", substring = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    /** The reading, the chrome, Settings, and the About row at the hub's foot. */
    private fun openAbout() {
        val readingPage = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 90_000) {
            compose.onAllNodesWithContentDescription(readingPage).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription(readingPage).onFirst().tapThePaper()
        val settings = compose.activity.getString(R.string.action_settings)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription(settings).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(settings).performClick()
        val about = compose.activity.getString(SettingsR.string.settings_title_about)
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(about).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(about).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(
                compose.activity.getString(SettingsR.string.about_group_content),
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
