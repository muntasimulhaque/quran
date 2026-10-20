package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.reader.GlyphSize
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import kotlin.math.abs

/**
 * The pill's words over its answer, on two lines and as one door: who is
 * reading, and where the reader is, or the size the offer names.
 *
 * **The two pills that carry words over an answer wear this block**: the
 * playing capsule, which answers with the transport, and the offer, which
 * answers with Download. The block is the offer's own shape, and the pill
 * joined it (owner decision, this session): one line of type over one answer,
 * 64 dp tall, where the playing pill used to stack a words row over a
 * transport row and pay 120 dp for a line of words that fits beside the
 * controls. The voices that are not a recitation keep the one line beside
 * their answer instead: a failure, a transfer, and an answer the app cannot
 * give each say a sentence rather than a place, and a sentence beside the
 * name is shorter than a block (owner decision, this session).
 *
 * The block is one door. Its two lines are one question, who and where, and
 * the panel behind it is one panel: the voices, the pace, and what happens at
 * the end, the whole of what a reader can change about what they are hearing.
 * The offer's block opens the reciter chooser alone, because before there is
 * anything to hear the one question is which voice to fetch, and the pace and
 * the end are answers to a recitation that does not exist yet (owner decision,
 * this session).
 *
 * The block carries the app's 48 dp target, so the two lines and the chevron
 * are not the whole of what a finger has to hit, while the shape the eye sees
 * is the block itself. Its 4 dp inset keeps the type off its own press shape,
 * and the caller gives its end [PillEndAir]: a capsule's end is a sphere, and
 * at the name's line an 8 dp air measured from the widest point read as a name
 * touching the border, which the Bangla matra made plain (owner report, this
 * session).
 *
 * The second line gives way before the first: the name is short and names the
 * door, and a place, a size, or a percent may end in an ellipsis on the glass
 * narrow for it. Neither line wraps: a control that grows a third line stops
 * being a control over the page.
 */
@Composable
internal fun WordsBlock(
    /** The reciter's name, or whatever names the thing the block is about. */
    name: String,
    /** The place, the size, the percent, or the state's sentence. */
    title: String,
    /**
     * The door the block is, or null where there is nothing to open: while a
     * package is on its way, the transfer's X is the one thing to press, and
     * a chevron over words that answer nothing is a door that lies.
     */
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier
                        .clickable(
                            interactionSource = press,
                            indication = null,
                            role = Role.Button,
                            onClick = onOpen,
                        )
                        .minimumInteractiveComponentSize()
                },
            )
            .clip(Radii.Pill)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.indication(press, ripple(color = MaterialTheme.colorScheme.primary))
                },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // The name is measured first and keeps its whole width; the
                // chevron needs its own room after it, never over it.
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onOpen != null) {
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(GlyphSize.ChevronInline),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The pill's words on one line, for the voices that are not a recitation: who
 * is reading, and the state's own sentence beside it.
 *
 * The reciter's name is the door to the chosen voice, and the sentence is a
 * notice: the pace and the end belong to a recitation, and these voices have
 * none, so the sentence does not open the listening panel (owner decision,
 * this session: the offer showed only the reciter chooser before there was
 * anything to hear, and a failure and an unavailable answer are the same
 * moment). A state whose name cannot be chosen, which is a package on its way,
 * draws the name as a notice too and no chevron.
 *
 * One line rather than the block, because the sentence and the name are one
 * thought wide: a failure beside the name is shorter than the name above it,
 * and a taller capsule for a sentence would be the 120 dp row again (owner
 * decision, this session: the block while playing, this line elsewhere).
 *
 * The name's flash is the door's whole 48 dp target, so the thin strip of type
 * and its chevron are not the whole of what a finger has to hit, and the
 * highlight the reader sees is the shape the finger hits. The sentence is bare
 * type, and it gives way before the name: a long status is cut with an ellipsis
 * before the name is.
 */
@Composable
internal fun PlaybackWordsLine(
    reciterName: String,
    /** The door to the voice, or null where the voice cannot be chosen now. */
    onOpenReciters: (() -> Unit)?,
    /** The state's own sentence, which is not a door. */
    status: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WordsGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterDoor(name = reciterName, onOpen = onOpenReciters)
        // The reciter's name is measured first and keeps its whole width; the
        // status is the weighted line, so a long reference or percent gives way
        // to the name rather than squeezing it into an ellipsis. The name is
        // the pill's door to its own chooser, and a door half-said is a door
        // the reader cannot read (owner report).
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/**
 * The reciter's name, and the door it is to: the voice the recitation will be
 * heard in, with what each one would still need for the surah at hand.
 *
 * The name wears a chevron the way every other door in the app does, so the
 * choice is found without a guess. It is a chooser and not a hop to the
 * settings: a reader who wants another voice is not asking where the voices
 * are listed. The settings page is where the voices are managed, with the
 * surahs each one has on the device, not where one is picked for the ayah
 * being heard (owner report).
 *
 * The door carries the app's 48 dp target around its own line, so the thin
 * strip of type and its chevron are not the whole of what a finger has to
 * hit, while the pill the eye sees is the line plus the app's own chip inset
 * and never the target (owner decision, the theme and consistency session:
 * with the clip outside the target, the press highlight read as a 48 dp
 * capsule over a 16 dp word). The transport is what carries a second row, so
 * there is no seam here for a smaller flash to carry, and the flash is the
 * shape the finger hits.
 *
 * Null where the voice cannot be chosen now, which is while a package is on
 * its way: the chevron is gone rather than a door that does nothing.
 */
@Composable
private fun ReciterDoor(
    name: String,
    onOpen: (() -> Unit)?,
) {
    val press = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier
                        .clickable(
                            interactionSource = press,
                            indication = null,
                            role = Role.Button,
                            onClick = onOpen,
                        )
                        .minimumInteractiveComponentSize()
                },
            )
            .clip(Radii.Pill)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.indication(
                        press,
                        ripple(color = MaterialTheme.colorScheme.primary, bounded = false),
                    )
                },
            )
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .testTag("playback-reciter")
            .semantics { contentDescription = name },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onOpen != null) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(GlyphSize.ChevronInline),
            )
        }
    }
}

/**
 * What the pill says under the reciter's name while an ayah plays: where the
 * reader is, and, only when it is not the ordinary one, the pace.
 *
 * What happens at the end of the audio is not said here. The sentence it
 * takes does not fit the line the block has, and the switch that carries the
 * answer says its own state where the reader turns it: a repeat is evident
 * from the switch that is on, and the pill is not a third place the answer is
 * written down (owner report).
 *
 * The pace stays, because it is a mark and not a sentence, and because a
 * recitation at 0.75x is the one thing about the audio a reader notices
 * without being told.
 */
@Composable
internal fun playbackStatus(
    reference: String,
    speed: Float,
): String {
    val extras = buildList {
        if (abs(speed - 1f) > 0.01f) add(speedText(speed))
    }
    // The reference and the pace are numbers the reader reads, so both follow
    // the interface's own digits; the separator is the app's one middot.
    return (listOf(localizedDigits(reference)) + extras)
        .filter { it.isNotBlank() }
        .joinToString(" \u00b7 ")
}
