package io.github.muntasimulhaque.quran

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.kit.AppSnackbar
import io.github.muntasimulhaque.quran.ui.kit.putAwaySentence
import io.github.muntasimulhaque.quran.ui.kit.rememberSentence
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The sentence ends on the reader's first touch, and the touch still lands.
 *
 * Every other raised thing in this app answers a touch: the search reveal,
 * the download offer, a paused recitation, the ayah menu, and the sheets. The
 * sentence was the one that did not, and a reader who touched the page to
 * reach an ayah wore "Note saved" over the foot of it until the timer ran out
 * (owner report, this session).
 *
 * The sentence here is shown with no timer at all, so the only thing that can
 * end it is the touch the test makes. Three answers are pinned: a touch on
 * the page ends the sentence and is still the page's touch, a touch on the
 * paper keeps it, and the pill's answer is never taken by the dismissal.
 */
@RunWith(AndroidJUnit4::class)
class SentenceTouchTest {

    @get:Rule
    val compose = createComposeRule()

    /** The page, its own touch counted, and the sentence over its foot. */
    private fun aPageWithASentence(
        message: String,
        action: String? = null,
        onResult: (SnackbarResult) -> Unit = {},
        onPageTouch: () -> Unit = {},
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = UiLanguage.English) {
                val sentence = rememberSentence()
                LaunchedEffect(Unit) {
                    val result = sentence.host.showSnackbar(
                        message = message,
                        actionLabel = action,
                        duration = SnackbarDuration.Indefinite,
                    )
                    onResult(result)
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .putAwaySentence(sentence),
                ) {
                    // The page itself, counted by its own input and not by a
                    // clickable: a clickable merges everything under it, and
                    // the sentence hanging over the page is not part of the
                    // page's own node.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("page")
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.changes.any { it.pressed && !it.previousPressed }) {
                                            onPageTouch()
                                        }
                                    }
                                }
                            },
                    )
                    AppSnackbar(
                        sentence = sentence,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun theSentenceIsGone(message: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(message).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun aTouchOnThePageEndsTheSentenceAndStillLands() {
        var pageTouches by mutableIntStateOf(0)
        aPageWithASentence(
            message = "Note saved",
            onPageTouch = { pageTouches++ },
        )
        // The paper keeps its ground at the foot of the glass, so the touch
        // that ends the sentence is one that belongs to the page.
        compose.onNodeWithTag("page").performTouchInput { click(Offset(centerX, 12f)) }
        theSentenceIsGone("Note saved")
        assertEquals(
            "the touch that ends the sentence is still the page's own touch",
            1,
            pageTouches,
        )
    }

    @Test
    fun aTouchOnThePaperKeepsTheSentence() {
        aPageWithASentence(message = "Note saved")
        compose.onNodeWithText("Note saved").performClick()
        compose.onNodeWithText("Note saved").assertIsDisplayed()
    }

    @Test
    fun thePillStillAnswersTheSentence() {
        var answered by mutableStateOf(false)
        aPageWithASentence(
            message = "Saved ayah removed",
            action = "Undo",
            onResult = { result -> answered = result == SnackbarResult.ActionPerformed },
        )
        compose.onNodeWithTag("snackbar-action-pill", useUnmergedTree = true).performClick()
        compose.waitUntil(timeoutMillis = 10_000) { answered }
        assertTrue("the paper's own ground keeps the pill's answer", answered)
    }
}
