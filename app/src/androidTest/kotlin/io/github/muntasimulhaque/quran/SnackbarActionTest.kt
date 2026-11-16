package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.kit.AppSnackbar
import io.github.muntasimulhaque.quran.ui.kit.rememberSentence
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Snackbar's one action wears the app's pill, and a screen reader is
 * answered by the host, not by the app.
 *
 * The bar used to borrow Material's bare action text, so the only word that
 * acted on the sentence was the one word on the page without a shape. The
 * action is the app's `TextButton` now, with the same 36 dp pill and 48 dp target as
 * every other action (owner decision, the consistency audit).
 *
 * A finger ends a sentence by touching anywhere but on it. A screen reader
 * has no finger, and Material's host already answers that reader with a
 * dismiss action whose label the platform speaks in the reader's own
 * language, so the app adds no second one (owner decision, this session).
 * This test pins the dependency: an upgrade that took that action away would
 * be read here, not learned from a TalkBack reader.
 */
@RunWith(AndroidJUnit4::class)
class SnackbarActionTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theSnackbarActionWearsTheAppPill() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                val sentence = rememberSentence()
                LaunchedEffect(Unit) {
                    sentence.host.showSnackbar(
                        message = "Saved ayah removed",
                        actionLabel = "Undo",
                    )
                }
                Box(Modifier.fillMaxSize()) {
                    AppSnackbar(sentence = sentence)
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("snackbar-action-pill", useUnmergedTree = true)
            .assertHeightIsEqualTo(36.dp)
        // The way out a finger cannot take: the host's own dismiss, on the
        // sentence's node, wherever inside it the host chose to put it.
        val dismiss = compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss),
                useUnmergedTree = true,
            )
            .fetchSemanticsNodes()
        assertTrue("the sentence carries a dismiss a screen reader can take", dismiss.isNotEmpty())
    }
}
