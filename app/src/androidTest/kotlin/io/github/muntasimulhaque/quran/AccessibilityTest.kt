package io.github.muntasimulhaque.quran

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeAccessibilityValidator
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/**
 * The accessibility checks, on the surfaces a reader actually meets: the
 * reading, its chrome, Browse, Settings, and Search. The validator is the
 * Google Accessibility Test Framework's, and it runs its full set of checks
 * from the screen's own root on every interaction this test makes, so a
 * control that loses its spoken name, a target that shrinks under a finger,
 * or a contrast pair that falls under the floor fails here instead of
 * reaching a reader (owner decision, this session).
 *
 * The framework is a test-only dependency and never ships; the shipped app
 * keeps the same set of dependencies it had before.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val library = object : ExternalResource() {
        override fun before() {
            val store = PackStore(context)
            store.install("translation-saheeh-en")
            store.install("words-en")
            store.install("tafsir-ibn-kathir-en")
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTheme(AppTheme.Light)
                    setTranslationPacks(setOf("translation-saheeh-en"))
                    setTafsirPacks(setOf("tafsir-ibn-kathir-en"))
                    setShowTranslation(true)
                    setWordByWord(false)
                    for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
                }
            }
        }

        /**
         * The tests share one install and one settings store, and a reading
         * with a translation on is a taller page than a reading without one:
         * a test that follows this one and looks for a line at the foot of
         * the first ayah can find it pushed below the fold. The choices this
         * test made are put back the way a fresh install holds them, so no
         * test after it inherits a reading it did not ask for (owner report,
         * this session).
         */
        override fun after() {
            runBlocking {
                SettingsStore(context).apply {
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                    for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
                }
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theMainSurfacesPassTheAccessibilityChecks() {
        val validator = AccessibilityValidator()
            .setRunChecksFromRootView(true)
            // Compose exposes an unnamed synthetic node for the content of a
            // clickable (a plain View with the click inherited and no text or
            // description), and ATF measures it as a touch target even though
            // the real, named control beside it carries the 48 dp. Only that
            // node is let go here; a named control that is too small, a
            // contrast pair that is too close, or a missing label still
            // fails, because every other check and every named element is
            // still measured (owner decision, this session).
            .setSuppressingResultMatcher(
                org.hamcrest.Matchers.allOf(
                    com.google.android.apps.common.testing.accessibility.framework
                        .AccessibilityCheckResultUtils.matchesCheckNames(
                            org.hamcrest.Matchers.containsString("TouchTargetSizeCheck"),
                        ),
                    com.google.android.apps.common.testing.accessibility.framework
                        .AccessibilityCheckResultUtils.matchesElements(ComposePhantomElement()),
                ),
            )
            .setThrowExceptionForErrors(true)
        compose.setComposeAccessibilityValidator(
            object : ComposeAccessibilityValidator {
                override fun check(view: android.view.View) {
                    validator.check(view)
                }
            },
        )

        // The reading itself.
        awaitReading()
        compose.onNodeWithContentDescription("Reading").assertExists()

        // The chrome: Browse, Search, and the reading's display. Settings is
        // the display panel's last row now (owner decision, this session).
        revealChrome()
        compose.onNodeWithContentDescription("Browse the Quran").assertExists()
        compose.onNodeWithContentDescription("Search").assertExists()
        compose.onNodeWithContentDescription("Display options").assertExists()

        // Browse, the surah list.
        compose.onNodeWithContentDescription("Browse the Quran").performClick()
        waitForTag("browse-sheet")
        compose.onNodeWithTag("browse-sheet").assertExists()
        Espresso.pressBack()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("browse-sheet", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Settings, the hub, through the reading's display panel.
        revealChrome()
        compose.onNodeWithContentDescription("Display options").performClick()
        waitForTag("display-all-settings")
        compose.onNodeWithTag("display-all-settings").performClick()
        waitForTag("settings-hub")
        compose.onNodeWithTag("settings-hub").assertExists()
        Espresso.pressBack()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("settings-hub", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // Search, which is the sheet with the field and the folded filters.
        // The field is where typing would begin, so the test does not type;
        // it does open the source door, because the chips inside it are
        // controls a reader turns off and every one has to be a named,
        // full-size target (owner decision, this session: the chip's fill had
        // grown to the whole target and was put back to the app's chip
        // height).
        revealChrome()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertExists()
        compose.onNodeWithText("Search in").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Quran text").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Quran text").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Quran text").assertWidthIsAtLeast(48.dp)
        Espresso.pressBack()
    }

    private fun awaitReading() {
        compose.waitUntil(180_000) {
            compose.onAllNodesWithContentDescription("Reading").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Compose's unnamed synthetic node for a clickable's content box: a plain
     * View that inherits the click and carries no text and no description.
     * The real control beside it is the one a reader meets and the one ATF
     * still measures in full.
     */
    private class ComposePhantomElement : org.hamcrest.BaseMatcher<
        com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement,
        >() {
        override fun matches(item: Any?): Boolean {
            val element = item as?
                com.google.android.apps.common.testing.accessibility.framework.uielement
                    .ViewHierarchyElement ?: return false
            return element.className == "android.view.View" &&
                element.text.isNullOrEmpty() &&
                element.contentDescription.isNullOrEmpty()
        }

        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("an unnamed Compose synthetic touch node")
        }
    }

    /** A tap on the reading's own quiet margin brings the chrome up. */
    private fun revealChrome() {
        repeat(3) {
            if (compose.onAllNodesWithContentDescription("Display options")
                    .fetchSemanticsNodes().isNotEmpty()
            ) {
                return
            }
            compose.onNodeWithContentDescription("Reading")
                .performTouchInput { click(Offset(4f, height * 0.5f)) }
            runCatching {
                compose.waitUntil(4_000) {
                    compose.onAllNodesWithContentDescription("Display options")
                        .fetchSemanticsNodes().isNotEmpty()
                }
            }
        }
    }

    private fun waitForTag(tag: String, timeout: Long = 15_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
