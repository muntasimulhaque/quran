package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect

/**
 * The medallion's ink, the box it hangs from, and the target a finger gets.
 *
 * An ayah's number is drawn inside the face's own printed medallion, and the
 * menu it opens is placed from the ink's box in window coordinates. The
 * arithmetic lives here, over plain rectangles, because a popup's place on the
 * glass cannot be read by a compose test: every node in a popup reports
 * coordinates from that window's own corner, so the rule is pinned as
 * arithmetic in a JVM test and the anchor in the compose test (the
 * repository's own trap, 19.3).
 *
 * This was private arithmetic inside the reading's ayah block, where no gate
 * could measure it; it moved here in 21.5 for the reason the trap gives.
 */

/**
 * One span's ink, as one rectangle over every character of it. The face draws
 * the medallion from several glyphs, so the first character's box alone is
 * narrower than the mark the reader sees.
 *
 * An empty span has no ink; the callers only ever pass a span they built, so
 * the zero is a guard and not a case the reading meets.
 */
fun spanInk(boxes: List<Rect>): Rect {
    val first = boxes.firstOrNull() ?: return Rect.Zero
    var box = first
    for (next in boxes) {
        box = Rect(
            left = minOf(box.left, next.left),
            top = minOf(box.top, next.top),
            right = maxOf(box.right, next.right),
            bottom = maxOf(box.bottom, next.bottom),
        )
    }
    return box
}

/**
 * The ink's box moved by the line's own place in the window. The menu's
 * placement is arithmetic in window coordinates, and each edge truncates on
 * its own, as the layout's box does.
 */
fun spanPlace(page: IntRect, ink: Rect): IntRect = IntRect(
    left = page.left + ink.left.toInt(),
    top = page.top + ink.top.toInt(),
    right = page.left + ink.right.toInt(),
    bottom = page.top + ink.bottom.toInt(),
)

/**
 * The medallion's touch target: its ink, grown until the shorter side is
 * [target]. The growth is even on both sides, so the target stays centered on
 * the mark the reader sees; a tap that lands on the paper beside the medallion
 * is the door, not a miss (owner decision, 13.8).
 */
fun spanTarget(ink: Rect, target: Float): Rect {
    val padX = ((target - ink.width) / 2f).coerceAtLeast(0f)
    val padY = ((target - ink.height) / 2f).coerceAtLeast(0f)
    return Rect(
        left = ink.left - padX,
        top = ink.top - padY,
        right = ink.right + padX,
        bottom = ink.bottom + padY,
    )
}
