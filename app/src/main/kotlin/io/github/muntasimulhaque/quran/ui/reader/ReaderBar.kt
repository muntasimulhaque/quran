package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The reading's bar as one number: how much of it stands over the page.
 *
 * **The bar is the page's head, not a curtain.** It has its own band above the
 * reading, and the page begins at the band's bottom, so no line is ever behind
 * it. The reading's first scroll is spent moving the bar, and only after the
 * bar is away does the page scroll; a backward scroll spends its first part
 * bringing the bar back, and the line the reader is returning to arrives below
 * it (owner decision, 19.4: the Chromium and Material app bar's own
 * rule, because a reading app must never put the text under glass).
 *
 * [shown] is 1 with the bar fully standing and 0 with only the status band
 * left. The bar's own height is [ownHeightPx], the row and its air without the
 * status bar, because the status band is always on the glass and is not the
 * reader's to spend. A finger's delta is consumed up to that height and the
 * remainder is the page's; when the finger lifts, [ReaderBar.settleTarget]
 * finishes the nearer end in 200ms, or at once when the phone asks motion to
 * be reduced.
 *
 * The values are a moment and never saved: a rotation, a language change, or
 * a return from the background opens with the bar standing, because the
 * reader has just arrived at the page. The reader's place is saved; the
 * chrome is not.
 */
internal class ReaderBarState {

    /**
     * The bar's place on its travel, as the state Compose watches.
     *
     * It is a state rather than a field because the bar moves on every scroll
     * delta and on every frame of its own 200ms settle. The reading hands
     * [shownFraction] down and never reads it in its own body: the page that
     * owns the bar is not rebuilt because the bar moved, the bar's band is
     * re-laid out and nothing above it is touched (owner decision, 19.8).
     * A reader with a 120 Hz phone gave the old read 8 ms a frame to rebuild
     * a whole surah in, which is what the scroll felt like.
     */
    private val shownState = mutableFloatStateOf(1f)

    /** The bar's own place: 1 fully standing, 0 with only the status band left. */
    var shown: Float
        get() = shownState.floatValue
        private set(value) {
            shownState.floatValue = value
        }

    /** [shown] as state, for the bar alone; the reading passes it and never reads it. */
    val shownFraction: State<Float> get() = shownState

    /** The row and its air, without the status bar the phone owns. */
    var ownHeightPx by mutableIntStateOf(0)

    private var job: Job? = null

    /**
     * Spends [delta] on the bar and returns what the bar took. The page gets
     * the rest. A forward drag is negative and hides; a backward drag is
     * positive and shows.
     */
    fun consume(delta: Float): Float {
        if (ownHeightPx <= 0 || delta == 0f) return 0f
        val range = ownHeightPx.toFloat()
        val current = shown.coerceIn(0f, 1f) * range
        val next = (current + delta).coerceIn(0f, range)
        shown = next / range
        return next - current
    }

    /** A reader's finger is on the page: whatever the bar was doing is over. */
    fun stop() {
        job?.cancel()
    }

    /** The bar stands at once, for an arrival or a return to the page. */
    fun showNow() {
        job?.cancel()
        shown = 1f
    }

    /** The reader asked for the bar with a tap. */
    fun show(scope: CoroutineScope, reducedMotion: Boolean) {
        job?.cancel()
        job = scope.launch { animateTo(1f, reducedMotion) }
    }

    /** The finger lifted with the bar between its two ends. */
    fun settle(scope: CoroutineScope, reducedMotion: Boolean) {
        val target = ReaderBar.settleTarget(shown)
        if (target == shown) return
        job?.cancel()
        job = scope.launch { animateTo(target, reducedMotion) }
    }

    private suspend fun animateTo(target: Float, reducedMotion: Boolean) {
        if (reducedMotion) {
            shown = target
            return
        }
        animate(
            initialValue = shown,
            targetValue = target,
            animationSpec = tween(durationMillis = ReaderBar.SnapMillis),
        ) { value, _ -> shown = value }
    }
}

/**
 * The bar's arithmetic, apart from its state so the JVM suite can pin it.
 *
 * The 0.5 split is the browser's own: a bar past its middle finishes opening
 * and a bar short of it finishes closing. It is a fact of the glass and not of
 * a screen, so it lives here beside the state and is measured without one
 * (owner decision, 19.4).
 */
internal object ReaderBar {

    /** The fraction at which a released bar finishes opening rather than closing. */
    const val SnapFraction = 0.5f

    /** The browser's own ceiling for a bar's show or hide, in milliseconds. */
    const val SnapMillis = 200

    /** Which end a released bar finishes at. */
    fun settleTarget(shown: Float): Float =
        if (shown.coerceIn(0f, 1f) >= SnapFraction) 1f else 0f
}
