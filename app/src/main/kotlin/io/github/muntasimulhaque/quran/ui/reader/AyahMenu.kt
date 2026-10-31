package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.kit.AppPopup
import io.github.muntasimulhaque.quran.ui.kit.MenuActionRow
import io.github.muntasimulhaque.quran.ui.kit.PopupAlignment

/**
 * The ayah menu's own width: the app's 288 dp panel measure less a quarter,
 * because its longest row is one word and the shared width left more than
 * half the popup empty. The 216 dp holds the five rows on the one leading
 * rail, keeps the menu from covering more of the page than it needs to, and
 * lets a label that outgrows it wrap rather than be clipped (owner decision,
 * this session).
 */
private val AyahMenuMeasure = 216.dp

/**
 * The ayah's own actions, in the app's one popup, hung from the ayah's door.
 *
 * The popup is the pill's popup, not a second menu: the same corners, the
 * same floating ground, the same focus that waits for the panel to settle so
 * the first row never opens looking chosen, the same row rhythm, and the
 * same rule that an answer puts the panel away. Only the width is its own,
 * because five one-word rows do not need the panel's 288 dp (owner decision,
 * this session). The only thing this menu adds to the app's vocabulary is a
 * row that acts instead of one that chooses, and even that shares the choice
 * row's rail to the dp (owner decision, this session).
 *
 * The order is the reader's intent, not the control's kind (owner decision,
 * forty-sixth session). The two reasons to open an ayah's actions lead: hear
 * it (Play) and understand it (Tafsir), the deep door this reading is built
 * around. Then the two that keep the ayah, Save before Note, because the keep
 * is the lighter tap and the note is the writing. Share stands last: it is
 * the only action that leaves the app, the least common, and the one a stray
 * tap costs most, so it takes the far end, away from where the thumb rests.
 *
 * Tafsir sits in the second slot when the reader has a tafsir on the device
 * and is absent otherwise: the packs are the setting, and a pack that is not
 * there is a door that does not exist rather than a card with nothing behind
 * it. Play is always first, Save is always before Note, and Share is always
 * last, so the three fixed actions never move and only the deep door comes
 * and goes with the reader's own choice.
 *
 * The two marked actions say their state: the note mark is lit by a note and
 * the bookmark by a Save, never the one by the other, and both say their
 * state out loud, because a lit glyph is a color and a color is not a
 * sentence (owner report).
 *
 * **Every tap closes the menu and then does its work** (owner decision, this
 * session: the pill's own rule, one panel asks one question and the tap
 * answers it). The reader is returned to the reading rather than to a panel
 * they have to dismiss, and a Save on a kept ayah with a note still asks its
 * question on the page, where the note is the subject.
 *
 * [anchor] is the door's own place on the glass, measured by the door, and
 * the menu hangs from its leading edge on the roomier side: below a door near
 * the head of the page, above one near the foot, and inside the window's safe
 * band when neither side can hold the whole list. [name] is the menu's pane
 * title, spoken and never drawn.
 */
@Composable
internal fun AyahMenu(
    reference: String,
    anchor: IntRect,
    isSaved: Boolean,
    hasNote: Boolean,
    showTafsir: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onTafsir: () -> Unit,
    onSave: () -> Unit,
    onNote: () -> Unit,
    onShare: () -> Unit,
) {
    val play = stringResource(R.string.action_play)
    val tafsir = stringResource(R.string.action_tafsir)
    val save = stringResource(R.string.action_save)
    val saved = stringResource(R.string.action_saved)
    val note = stringResource(R.string.action_note)
    val share = stringResource(R.string.action_share)
    val name = stringResource(R.string.ayah_menu_title, reference)
    AppPopup(
        anchor = anchor,
        onDismiss = onDismiss,
        width = AyahMenuMeasure,
        alignment = PopupAlignment.Start,
        name = name,
    ) {
        MenuActionRow(
            icon = Icon.Play,
            label = play,
            onClick = {
                onDismiss()
                onPlay()
            },
        )
        if (showTafsir) {
            MenuActionRow(
                icon = Icon.Tafsir,
                label = tafsir,
                onClick = {
                    onDismiss()
                    onTafsir()
                },
            )
        }
        MenuActionRow(
            icon = if (isSaved) Icon.BookmarkFilled else Icon.Bookmark,
            label = if (isSaved) saved else save,
            onClick = {
                onDismiss()
                onSave()
            },
            active = isSaved,
            state = markState(isSaved),
        )
        MenuActionRow(
            icon = Icon.Note,
            label = note,
            onClick = {
                onDismiss()
                onNote()
            },
            active = hasNote,
            state = markState(hasNote),
        )
        MenuActionRow(
            icon = Icon.Share,
            label = share,
            onClick = {
                onDismiss()
                onShare()
            },
        )
    }
}

/**
 * What one of the menu's marks says about itself, in the app's own words. The
 * mark a reader cannot see is a mark they were never told about, and the
 * glyph's color is not a sentence.
 */
@Composable
private fun markState(on: Boolean): String =
    stringResource(if (on) R.string.action_state_on else R.string.action_state_off)
