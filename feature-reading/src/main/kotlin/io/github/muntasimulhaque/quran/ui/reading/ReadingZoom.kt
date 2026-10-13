package io.github.muntasimulhaque.quran.ui.reading

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * A two-finger pinch on the reading changes the text size.
 *
 * The gesture is deliberately deaf to one finger: a one-finger drag is the
 * scroll, and a reading surface whose scroll stopped working would be a
 * reading surface nobody could read (owner decision, this session). Only a
 * gesture that has two pointers down and a zoom consumes anything, so the
 * scrollable under it keeps every ordinary drag.
 *
 * A step is asked for each [ZOOM_STEP] of accumulated pinch, so the reader's
 * own five sizes move one at a time instead of a continuous scale landing
 * between two of them. The accumulators reset when the gesture ends, so the
 * next pinch starts from the reader's hand again.
 */
fun Modifier.readingZoom(onStep: (Int) -> Unit): Modifier = pointerInput(onStep) {
    var accumulated = 1f
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val zoom = event.calculateZoom()
            if (event.changes.size > 1 && zoom != 1f) {
                accumulated *= zoom
                var steps = 0
                while (accumulated >= ZOOM_STEP) {
                    accumulated /= ZOOM_STEP
                    steps++
                }
                while (accumulated <= 1f / ZOOM_STEP) {
                    accumulated *= ZOOM_STEP
                    steps--
                }
                if (steps != 0) onStep(steps)
                event.changes.forEach { change -> if (change.positionChanged()) change.consume() }
            }
        } while (event.changes.any { it.pressed })
        accumulated = 1f
    }
}

/** How much pinch one step of text size takes. */
private const val ZOOM_STEP = 1.12f
