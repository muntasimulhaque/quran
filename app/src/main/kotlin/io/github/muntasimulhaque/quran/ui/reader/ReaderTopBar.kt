package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.kit.IconButton
import io.github.muntasimulhaque.quran.ui.kit.ReadingTitle
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlin.math.roundToInt

/**
 * The reading's bar, and the one place its margins live.
 *
 * **The bar is the page's head** (owner decision, 19.4). It has its
 * own band at the top, and the reading begins at the band's bottom: no ayah,
 * no opening, and no translation is ever behind it. As the reader moves
 * forward, [shown] falls and the band, with the page's own top under it,
 * rises; when the band is gone, the page scrolls. A backward move spends its
 * first distance bringing the band back, so the line the reader is returning
 * to arrives below it, never under it. [ReaderBarState] owns that arithmetic.
 *
 * **One row, `Space.Tight` above and below.** The row is 48 dp, the app's
 * touch floor, and the title's capsule is 48 for the same reason. The four
 * points of air above and below the row are the seam that keeps the marks off
 * the status bar and the reading, and the whole of the air the bar spends: the
 * bar was 8 above and 20 below when it floated over the page, and the 20 was
 * the old scrim's tail and the page's gutter, not the head's. The 48 row is
 * the floor; a bar shorter than that would clip the target or the title's
 * capsule, which is the one thing the app never does to a finger.
 *
 * **The sides keep the page head's 8.** A round control is a 48 dp target
 * holding a 24 dp mark, so the row is inset by the app's one edge
 * ([Space.Gutter]) less the 12 dp of air inside that target. Browse, Search,
 * and Settings land on the reading's 20 dp line, and the title's capsule sits
 * on the same line.
 *
 * The three marks are Browse, Search, and Settings, and the surah and its juz
 * sit between them: the bar holds the places the reader can go and the place
 * they are in. The text size is a setting, so it lives in Settings rather than
 * taking the room the juz needs; the bar is not the page's display panel
 * (owner decision, 19.4: Aa left the bar, the folio left the
 * page, and the title is whole again on a phone).
 */

/** The bar's own margin: the app's edge less the air inside a round target. */
private val BarInset = Space.Gutter - 12.dp

/**
 * The bar's band, from the status bar's bottom to the reading's top.
 *
 * [shown] is how much of the row's own height stands; 1 is fully standing and
 * 0 leaves only the status bar's band, which belongs to the phone and is never
 * the reader's to scroll away. The natural content is measured once and placed
 * above the band by the part that is away, so the page's top and the bar's
 * bottom are the same line on every frame.
 *
 * [onOwnHeight] is the row and its air without the status bar, in pixels: the
 * distance a finger spends before the page scrolls. It is measured, not
 * written down, because the title grows with the phone's type scale.
 *
 * A fully away bar leaves the screen reader nothing: the page's own "Show the
 * bar" action is the way back, and a control the reader cannot see should not
 * be the next thing they hear (owner decision, 19.4).
 */
@Composable
internal fun ReaderTopBar(
    title: String,
    detail: String?,
    /**
     * How much of the bar stands, as state: 1 standing and 0 away.
     *
     * A state and not a number, because a number would be read by the screen
     * that owns this bar, and the screen would be rebuilt on every scroll
     * delta for a value only the bar draws (owner decision, 19.8).
     */
    shown: State<Float>,
    /** What a tap on the title does, said out loud; null when it does nothing. */
    titleAction: String?,
    onTitle: () -> Unit,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onOwnHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusPx = with(LocalDensity.current) { status.roundToPx() }
    // The two things a rebuild is owed for: the bar arriving or leaving for
    // good, and the name a test finds it by. Both are derived, so the 200ms a
    // settle spends does not rebuild this row 12 times on the way.
    val away by remember(shown) { derivedStateOf { shown.value <= 0.001f } }
    val tag by remember(shown) {
        derivedStateOf { if (shown.value >= 0.99f) "reading-bar-shown" else "reading-bar" }
    }
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .then(
                        if (away) {
                            Modifier.clearAndSetSemantics {}
                        } else {
                            Modifier
                        },
                    )
                    // The content is measured at its natural height, so the
                    // band's visible part and the row's own height are the
                    // same measurement (owner decision, 19.4).
                    .onSizeChanged { onOwnHeight((it.height - statusPx).coerceAtLeast(0)) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = BarInset,
                            end = BarInset,
                            top = Space.Tight,
                            bottom = Space.Tight,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The bar is one row: Browse leads, the surah
                    // and its juz sit centered, and Search and
                    // Settings close it.
                    IconButton(Icon.Browse, stringResource(R.string.action_browse), onBrowse)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Space.Tight),
                        contentAlignment = Alignment.Center,
                    ) {
                        ReadingTitle(
                            surah = title,
                            detail = detail,
                            action = titleAction,
                            onClick = onTitle,
                        )
                    }
                    IconButton(Icon.Search, stringResource(R.string.action_search), onSearch)
                    IconButton(Icon.Settings, stringResource(R.string.action_settings), onSettings)
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
    ) { measurables, constraints ->
        // Read in the layout, where it is spent: the band's height on this
        // frame. A read here re-lays the band out and never rebuilds
        // anything, which is the whole of the rule above.
        val reveal = shown.value.coerceIn(0f, 1f)
        val content = measurables.first().measure(constraints.copy(minHeight = 0))
        val own = (content.height - statusPx).coerceAtLeast(0)
        val visible = statusPx + (own * reveal).roundToInt()
        layout(constraints.maxWidth, visible) {
            content.place(x = 0, y = -(own * (1f - reveal)).roundToInt())
        }
    }
}
