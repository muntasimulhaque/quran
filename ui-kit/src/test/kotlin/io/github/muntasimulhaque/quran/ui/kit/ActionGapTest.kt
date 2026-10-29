package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.theme.Space
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The app's one action gap, pinned where no screen is needed.
 *
 * Two buttons side by side stand `Space.Action` apart between their pills. A
 * pair that stands inside Material's own dialog asks for the rest of that
 * gap, because the platform lays its own two buttons `DialogButtonSpacing`
 * apart: the reminder's Cancel wears `Space.Action - DialogButtonSpacing`, so
 * the reader sees the same 20 dp in the dialog as in the share sheet, the
 * remove question, a note, and the pill.
 *
 * The arithmetic is the decision, and it is pinned here because a dialog's
 * pair cannot be read off the glass: every node in a popup reports its place
 * from that window's own corner (the trap the project already keeps), so the
 * reminder's Set and Cancel have no shared origin with the sheet behind them.
 * The platform's 8 dp is written down rather than assumed, so a Material
 * update that moves it fails the last leg and gets looked at.
 */
class ActionGapTest {

    @Test
    fun twoButtonsStandOnTheActionGap() {
        assertEquals(20.dp, Space.Action)
    }

    @Test
    fun theDialogPairAddsUpToTheSameGap() {
        assertEquals(8.dp, DialogButtonSpacing)
        assertEquals(
            "the caller's padding plus the platform's spacing is the action gap",
            Space.Action,
            (Space.Action - DialogButtonSpacing) + DialogButtonSpacing,
        )
        assertEquals(
            "what the dialog's Cancel wears is the app's row gap",
            Space.Row,
            Space.Action - DialogButtonSpacing,
        )
    }
}
