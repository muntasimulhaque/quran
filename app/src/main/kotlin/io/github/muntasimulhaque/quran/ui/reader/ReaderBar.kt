package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
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
 * finishes the nearer end on a spring that carries the finger's own speed,
 * or at once when the phone asks motion to be reduced (owner decision, 22.8).
 *
 * The values are a moment and never saved: a rotation, a language change, or
 * a return from the background opens with the bar standing, because the
 * reader has just arrived at the page. The reader's place is saved; the
 * chrome is not.
 */
internal class ReaderBarState(private val now: () -> Long = { System.nanoTime() }) {

    /**
     * The bar's place on its travel, as the state Compose watches.
     *
     * It is a state rather than a field because the bar moves on every scroll
     * delta and on every frame of its own settle. The reading hands
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

    /** The finger's recent deltas, so a release can carry its own speed. */
    private val samples = mutableListOf<TimedDelta>()

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
        // The release reads only the finger's last hundred milliseconds, so
        // the trail is trimmed as it is written rather than at the release
        // (owner decision, 22.8).
        val at = now()
        samples += TimedDelta(delta, at)
        while (samples.isNotEmpty() && at - samples.first().at > ReaderBar.ReleaseWindowNanos) {
            samples.removeAt(0)
        }
        while (samples.size > ReaderBar.ReleaseSamples) samples.removeAt(0)
        return next - current
    }

    /** A reader's finger is on the page: whatever the bar was doing is over. */
    fun stop() {
        job?.cancel()
        samples.clear()
    }

    /** The bar stands at once, for an arrival or a return to the page. */
    fun showNow() {
        job?.cancel()
        samples.clear()
        shown = 1f
    }

    /** The finger's speed at [at], in travel per second, as the settle reads it. */
    internal fun releaseVelocity(at: Long): Float =
        ReaderBar.releaseVelocity(samples, at, ownHeightPx)

    /** The reader asked for the bar with a tap. */
    fun show(scope: CoroutineScope, reducedMotion: Boolean) {
        job?.cancel()
        job = scope.launch { animateTo(1f, reducedMotion, velocity = 0f) }
    }

    /** The finger lifted with the bar between its two ends. */
    fun settle(scope: CoroutineScope, reducedMotion: Boolean) {
        val target = ReaderBar.settleTarget(shown)
        val velocity = releaseVelocity(now())
        samples.clear()
        if (target == shown) return
        job?.cancel()
        job = scope.launch { animateTo(target, reducedMotion, velocity) }
    }

    private suspend fun animateTo(
        target: Float,
        reducedMotion: Boolean,
        velocity: Float,
    ) {
        if (reducedMotion) {
            shown = target
            return
        }
        // A critically damped spring and not a tween: a flick keeps its own
        // speed into the settle, and NoBouncy means the bar still never passes
        // an end. From rest it settles in about the 200 ms the bar always took
        // (owner decision, 22.8).
        animate(
            initialValue = shown,
            targetValue = target,
            initialVelocity = velocity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = ReaderBar.SettleStiffness,
            ),
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

    /**
     * The settle's stiffness. Four over the square root of this is about 200
     * ms from rest, the browser's own ceiling the bar has always kept, and a
     * critically damped spring means the bar never passes either end
     * (owner decision, 22.8: the curve carries the finger's speed, and every
     * settled number stays).
     */
    const val SettleStiffness = 400f

    /** How far back a release reads the finger's own speed. */
    const val ReleaseWindowNanos = 100_000_000L

    /** The most deltas a release reads, so a long drag cannot grow the trail. */
    const val ReleaseSamples = 8

    private const val OneMilliNanos = 1_000_000L
    private const val NanosPerSecond = 1_000_000_000f

    /** Which end a released bar finishes at. */
    fun settleTarget(shown: Float): Float =
        if (shown.coerceIn(0f, 1f) >= SnapFraction) 1f else 0f

    /**
     * The finger's speed at release, in shares of the bar's travel per second:
     * the deltas inside the window, over the time they cover, over the span the
     * bar moves. The settle takes it as its initial velocity, so a flick is
     * finished at the speed it was thrown rather than at the speed of a tap
     * (owner decision, 22.8).
     */
    fun releaseVelocity(samples: List<TimedDelta>, now: Long, spanPx: Int): Float {
        if (spanPx <= 0) return 0f
        val recent = samples.filter { now - it.at in 0..ReleaseWindowNanos }
        val first = recent.firstOrNull() ?: return 0f
        val elapsed = (now - first.at).coerceAtLeast(OneMilliNanos)
        val px = recent.sumOf { it.delta.toDouble() }.toFloat()
        return px / elapsed * NanosPerSecond / spanPx
    }
}

/** One finger delta on the bar, and when it arrived, for the release's speed. */
internal data class TimedDelta(val delta: Float, val at: Long)
