package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * The app's one Snackbar: the sentence a surface owes the reader when an act
 * has no visible result of its own.
 *
 * The reading and Browse each hold their own [Sentence], because a sentence
 * belongs to the surface that raised it and the reading's is behind the
 * Browse sheet. What they do not each hold is the drawing: the shape, the
 * fill, and the ink were typed twice, so the two could drift the way the
 * app's failure sentences already had. There is one shape now, and the
 * surfaces differ only in where they hang it.
 *
 * A sentence is a raised thing, and every other raised thing in this app
 * leaves on the reader's first touch: the search reveal, the download offer,
 * a paused recitation, the ayah menu, and the sheets all answer a touch, and
 * the sentence was the one that did not. A reader who touched the page to
 * reach an ayah still wore "Note saved" over the foot of it until the timer
 * ran out (owner report, this session). [putAwaySentence] is that rule, and
 * the sentence's own paper is the one ground it spares: a touch there
 * belongs to the sentence, and the pill's answer must not be taken by the
 * dismissal. Material's timer is the backstop, never the only way out.
 *
 * The action is the app's own [TextButton], not Material's bare action text:
 * a snackbar is not the one place a word that acts stops wearing a shape.
 * The pill sits inside the same sentence, and Undo or Settings reads as the
 * one thing the reader can do without becoming a second voice.
 *
 * A screen reader is answered by Material, not by this file: the host marks
 * the paper it draws with a pane title, a polite live region while it shows,
 * and a dismiss action whose label the platform speaks in the reader's own
 * language. Two answers to one question would be one too many, so the app
 * adds none (owner decision, this session).
 *
 * **A sentence is never merged over.** The host merges its own descendants,
 * and the pane title it puts on the paper is one of Compose's unmergeable
 * properties, so a merging ancestor over this composable (a clickable, a
 * toggleable, a list item) throws on the semantics tree before it ever
 * throws on the glass. The sentence is a sibling of the page and never a
 * child of something that merges (owner decision, this session).
 */
@Composable
fun AppSnackbar(
    sentence: Sentence,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = sentence.host, modifier = modifier) { data ->
        Snackbar(
            // The paper's own edge is the ground the touch rule spares, so
            // it is the paper that is measured, not the host around it.
            modifier = Modifier
                .testTag("sentence-paper")
                .onGloballyPositioned { sentence.ground = it.boundsInWindow() },
            shape = Radii.Card,
            // The app's one floating paper, the same the playback pill and
            // the offer wear: the sheet's white on the day and the one-step
            // night paper, above the page it interrupts. The Material
            // Snackbar carries its own elevation, so the sentence keeps an
            // edge even on a sheet, where its fill is the sheet's own (owner
            // decision, this session).
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            action = data.visuals.actionLabel?.let { label ->
                {
                    TextButton(
                        label = label,
                        onClick = { data.performAction() },
                        pillTag = "snackbar-action-pill",
                    )
                }
            },
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * The sentence a surface owes the reader, and the ground it stands on.
 *
 * A surface makes one with [rememberSentence] and hands the same value to
 * both [AppSnackbar], which draws the paper, and [putAwaySentence], which
 * ends it. The two have to agree about where the paper is, so they share one
 * measurement instead of taking one each.
 *
 * A ground left behind by a sentence that has gone is harmless: with
 * nothing showing, putting it away is nothing, and the next sentence
 * measures its own paper on its first frame.
 */
@Stable
class Sentence internal constructor() {

    /** Material's host: the queue, the timer, and the announcement. */
    val host = SnackbarHostState()

    /**
     * The paper's own place on the glass, in window coordinates, measured
     * where it is drawn. A plain value rather than a state: only the touch
     * stream reads it, and a frame that moves the paper recomposes nothing
     * to say so.
     */
    internal var ground: Rect? = null
}

/** The one sentence a surface owes, remembered across recompositions. */
@Composable
fun rememberSentence(): Sentence = remember { Sentence() }

/**
 * The sentence's answer to the reader: a touch that is not its own paper
 * puts it away, and the touch still reaches whatever it landed on.
 *
 * A reader must never pay twice for touching the page, so this only
 * watches. It takes the initial pass, which sees the touch before the child
 * that owns the gesture, and it consumes nothing (owner decision, this
 * session). The offer, the paused recitation, and the search reveal already
 * answer a page touch that way; the sentence is the same shape of thing.
 *
 * It belongs on the root of the surface's own window, and never as a layer
 * laid over the page: a full size sibling above the page takes the hit test
 * from it, and the reading would stop hearing the reader at all.
 */
@Composable
fun Modifier.putAwaySentence(sentence: Sentence): Modifier {
    // The node's own corner in the window, kept beside the touch stream: a
    // press arrives in this node's coordinates and the sentence's ground is
    // in the window's, so the two are brought to one plane before they meet.
    val corner = remember { Corner() }
    return this
        .onGloballyPositioned { corner.at = it.positionInWindow() }
        .pointerInput(sentence) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val press = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                        ?: continue
                    if (sentenceGivesWay(sentence.ground, press.position + corner.at)) {
                        sentence.host.currentSnackbarData?.dismiss()
                    }
                }
            }
        }
}

/** A node's corner in the window, kept without a recomposition. */
private class Corner {
    var at: Offset = Offset.Zero
}

/**
 * Whether a touch puts the sentence away: any touch that is not the paper
 * itself. The ground is the whole paper, so the action pill and the words
 * are equally the sentence's own, and the answer to the sentence cannot be
 * taken by the touch that gives it.
 *
 * This is arithmetic rather than a reading of where the paper happened to
 * land, so it is pinned on a machine with no screen.
 */
internal fun sentenceGivesWay(ground: Rect?, at: Offset): Boolean =
    ground == null || !ground.contains(at)
