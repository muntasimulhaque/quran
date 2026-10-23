package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.theme.Radii

/**
 * What one of the pill's marks says about itself, in the app's own words. The
 * mark a reader cannot see is a mark they were never told about, and the
 * glyph's colour is not a sentence.
 */
@Composable
private fun markState(on: Boolean): String =
    stringResource(if (on) R.string.action_state_on else R.string.action_state_off)

/**
 * The ayah's own actions, on the pill an ayah raises when the reader taps it:
 * hear it, understand it, keep it, write on it, and send it out.
 *
 * The order is the reader's intent, not the control's kind (owner decision,
 * this session). The two reasons to open an ayah's actions lead: hear it (Play) and
 * understand it (Tafsir), the deep door this reading is built around. Then the
 * two that keep the ayah, Save before Note, because the keep is the lighter
 * tap and the note is the writing: Save lands where it stands, and Note opens
 * a sheet and a keyboard. A note keeps its ayah in Saved and never lights the
 * bookmark (below), so the keep is the fact and the note is what is written on
 * it, and a parent before its child reads in the order the eye lands (owner
 * decision, forty-sixth session). Share stands last: it is the only action
 * that leaves the app, the least common, and the one a stray tap is most costed
 * by, so it takes the far edge, away from where the thumb rests.
 *
 * Tafsir sits in the second slot when the reader shows tafsir and is absent
 * otherwise; Play is always first, Save is always before Note, and Share is
 * always last, so the three fixed actions never move and only the deep door
 * comes and goes with the reader's own choice.
 *
 * Each action carries its own mark, and a mark says the tap that made it: the
 * note glyph is lit by a note and the bookmark by a Save, never the one by the
 * other. The two marked actions also say their state out loud, because a lit
 * glyph is a color and color is not something a reader who cannot see it is
 * told (owner report).
 */
@Composable
internal fun AyahActions(
    isSaved: Boolean,
    hasNote: Boolean,
    showTafsir: Boolean,
    onSave: () -> Unit,
    onPlay: () -> Unit,
    onNote: () -> Unit,
    onMore: () -> Unit,
    onShare: () -> Unit,
) {
    val play = stringResource(R.string.action_play)
    val tafsir = stringResource(R.string.action_tafsir)
    val save = stringResource(R.string.action_save)
    val note = stringResource(R.string.action_note)
    val share = stringResource(R.string.action_share)
    // One width for all five actions: the widest word of the five plus the
    // pill's own side padding. Five labels of five lengths then press in five
    // identical capsules instead of five ovals of the words' own making
    // (owner decision, this session).
    val actionWidth = rememberActionWidth(
        listOfNotNull(play, if (showTafsir) tafsir else null, save, note, share),
    )
    val shape = Radii.Pill
    Row(
        modifier = Modifier
            .floatingLift(shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // Five measured capsules can be wider than a small phone or a
            // large system font. A horizontal scroll keeps the last action
            // reachable instead of clipping it off the glass.
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextAction(play, Icon.Play, onPlay, minWidth = actionWidth)
        if (showTafsir) {
            TextAction(
                label = tafsir,
                icon = Icon.Tafsir,
                onClick = onMore,
                minWidth = actionWidth,
            )
        }
        // The two marked actions say their state; the three that only answer a
        // tap say nothing, so nothing on the pill reads as a switch it is not.
        TextAction(
            label = save,
            icon = if (isSaved) Icon.BookmarkFilled else Icon.Bookmark,
            onClick = onSave,
            active = isSaved,
            state = markState(isSaved),
            minWidth = actionWidth,
        )
        TextAction(
            label = note,
            icon = Icon.Note,
            onClick = onNote,
            active = hasNote,
            state = markState(hasNote),
            minWidth = actionWidth,
        )
        TextAction(share, Icon.Share, onShare, minWidth = actionWidth)
    }
}

/**
 * The one width every action of the ayah bar shares: the widest of its words
 * measured in the bar's own label style, plus the pill's horizontal padding,
 * never under the 48 dp floor the action keeps inside its clip. The
 * measurement follows the interface language and the reader's font scale, so
 * a Bangla bar measures Bangla words and a large system text measures large
 * ones (owner decision, this session).
 */
@Composable
private fun rememberActionWidth(labels: List<String>): Dp {
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    return remember(measurer, style, labels, density) {
        val widest = labels.maxOfOrNull { measurer.measure(it, style).size.width } ?: 0
        with(density) {
            val icon = GlyphSize.Action.roundToPx()
            maxOf(widest, icon).toDp() + 12.dp
        }.coerceAtLeast(48.dp)
    }
}
