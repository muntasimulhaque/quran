package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.AppChevron
import io.github.muntasimulhaque.quran.ui.kit.ChevronRole
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlin.math.min
import io.github.muntasimulhaque.quran.ui.theme.compactLabel
import io.github.muntasimulhaque.quran.uikit.R as UiKitR

/**
 * The transfer's words: the reciter's name over the state's sentence, with
 * the progress line under the row.
 *
 * The transfer keeps the old two-line block because it is a one-row capsule
 * with the X at its end, not the offer's two centered rows. The block is not
 * a door: while a package is on its way, the only control is the X, and a
 * chevron over words that answer nothing is a door that lies (owner decision,
 * this session).
 */
@Composable
internal fun TransferWordsBlock(
    /** The reciter's name, or whatever names the thing the block is about. */
    name: String,
    /** The percent or the state's sentence. */
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Space.Tight, vertical = 2.dp),
    ) {
        Text(
            text = name,
            // A name in a control, not a group label: 13 at Regular. The
            // title is the compact 14 Medium line, so the reader sees the
            // state before who is offering it (owner decision, this version).
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = compactLabel(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The playing pill's words: the voice and its pace over the place and its
 * key, a two-line lockup that shares one row with the transport.
 *
 * **The words are stacked, because one line cannot hold them** (owner
 * decision, this session). The capsule is 328 dp, and one line of words has
 * 236 dp beside the answer's one mark. The four facts on one line measure 231
 * dp in English at the default type, but 247 in Bangla, where the place's own
 * name is longest, and 257 in English at the top step: one line cuts the
 * place, which the pill never cuts. The lockup is the answer that keeps every
 * fact: the two lines are read one after the other, so the block's width is
 * the wider of the two and never their sum. The order is the one the app
 * already speaks: who is reading, then where. The voice leads with its pace
 * when the pace is not 1x, and the place follows at the card's 14 Medium with
 * the ayah's key and the door's turn. The pace is drawn only when it is not
 * 1x, because 1x is the pace the app starts at.
 *
 * **The place and the key never give way; the voice does** (owner decision,
 * this session). The pill's type follows the system scale only up to the
 * app's largest step, 1.2, and stops there. Each line is its own row, so the
 * place and the key keep their ends, the pace keeps the voice's line, and the
 * voice is the only part an ellipsis can take. The reading and the listening
 * panel still take the reader's own scale; the pill is a control with a fixed
 * measure.
 *
 * **The block is the transfer's own lockup** (owner decision, this session).
 * The transfer's words are a name over a state at a 2 dp inset, and this is
 * the same block with the place under the voice, in the same capsule: the
 * offer, the transfer, and the recitation are one shape in three states. The
 * 2 dp lets the two lines keep their own leading as the air between them, and
 * the block stands inside a 48 dp target that the finger gets whole.
 *
 * **The offer borrows the block** (owner decision, this session). The surah
 * and its size stand on the place line with no key, and the whole block is
 * the door to the offer's voice list, so the offer, the transfer, and the
 * recitation are one two-line lockup and the offer is the playing pill's own
 * height. The size is the fact the reader is spending; it stays on the line
 * instead of being the first thing an ellipsis takes.
 *
 * **The door says its state** (owner decision, this session). The chevron is
 * [AppChevron]'s [ChevronRole.Expand], so it points down while the listening
 * panel is closed and up while it is open, and the block speaks the app's
 * Expanded or Collapsed. The panel's own name is spoken when it opens; this is
 * the block's half of the same answer.
 */
@Composable
internal fun PlayingWordsBlock(
    /** The surah's name in the reader's language; null while it is not known. */
    placeName: String?,
    /** The reciter's name. */
    reciter: String,
    /**
     * The ayah's reference, already in the reader's digits. Empty on the
     * offer, whose place line is the surah and its size and closes with the
     * turn alone.
     */
    reference: String,
    /** The reader's pace, drawn only when it is not the app's own 1x. */
    speed: Float,
    /** The door the block is, or null where there is nothing to open. */
    onOpen: (() -> Unit)?,
    /** True while the listening panel is on the glass. */
    expanded: Boolean = false,
    /** True while the place's line is a failure the reader has to hear. */
    announce: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    val expandedText = stringResource(UiKitR.string.door_expanded)
    val collapsedText = stringResource(UiKitR.string.door_collapsed)
    // The pill is a control with a fixed line, so its type follows the system
    // scale only up to the app's largest text step. Past it the line would
    // cut the place, and a cut place is worse than a control that holds its
    // measure; the reading and the listening panel still take the reader's
    // own scale (owner decision, this session).
    val density = LocalDensity.current
    val capped = Density(density.density, min(density.fontScale, TextSize.STEPS.last()))
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
                        .semantics {
                            stateDescription =
                                if (expanded) expandedText else collapsedText
                        }
                },
            )
            .clip(Radii.Pill)
            .focusRing(press, Radii.Pill)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.indication(press, ripple(color = MaterialTheme.colorScheme.primary))
                },
            )
            // The transfer's own inset: 2 dp holds a lockup whose two lines
            // keep their own leading, so the block is 42 dp inside the 48 dp
            // target and the air the reader sees is the line boxes' own
            // (owner decision, this session).
            .padding(horizontal = Space.Tight, vertical = 2.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalDensity provides capped) {
            // The voice's line: who is reading, and how fast when the pace is
            // not the app's own 1x. The voice is the part that gives way.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reciter,
                    // The voice leads: the accent's 13, and the one part of
                    // the block that gives way when a long name meets the top
                    // step (owner decision, this session).
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (speed != 1f) {
                    Text(
                        text = speedText(speed),
                        // The pace is the pill's read-back after the panel
                        // closes: the quiet value beside the voice, not a
                        // second title (owner decision, this session).
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = Space.Line),
                    )
                }
            }
            // The place's line: the surah at the card's 14 Medium, its key,
            // and the turn. The place is never the thing an ellipsis takes.
            // The offer borrows this line with no key and its size after the
            // surah's name, so the block is the same two-line lockup in both
            // of its homes.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!placeName.isNullOrBlank()) {
                    Text(
                        text = placeName,
                        style = compactLabel(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (announce) {
                                    Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                if (reference.isNotBlank()) {
                    Text(
                        text = reference,
                        // The ayah key closes the place: 13 at Regular, the
                        // precise reference beside the surah's own name, and it
                        // never gives way (owner decision, this session).
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.padding(start = Space.Tight),
                    )
                }
                if (onOpen != null) {
                    AppChevron(
                        role = ChevronRole.Expand,
                        expanded = expanded,
                        tint = MaterialTheme.colorScheme.primary,
                        size = GlyphSize.ChevronInline,
                        modifier = Modifier.padding(start = Space.Tight),
                    )
                }
            }
        }
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
 *
 * The name's chevron is the door's own state: it turns up while the voice list
 * is open, and the name speaks Expanded or Collapsed (owner decision, this
 * session).
 */
@Composable
internal fun PlaybackWordsLine(
    reciterName: String,
    /** The door to the voice, or null where the voice cannot be chosen now. */
    onOpenReciters: (() -> Unit)?,
    /** The state's own sentence, which is not a door. */
    status: String,
    modifier: Modifier = Modifier,
    /** True while the sentence is a failure or an answer the reader has to hear. */
    announce: Boolean = false,
    /** True while the voice list is on the glass. */
    expanded: Boolean = false,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WordsGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterDoor(name = reciterName, onOpen = onOpenReciters, expanded = expanded)
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
            modifier = Modifier
                .weight(1f, fill = false)
                .then(
                    if (announce) {
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    } else {
                        Modifier
                    },
                ),
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
 *
 * **The door says its state** (owner decision, this session): the chevron is
 * [AppChevron]'s [ChevronRole.Expand] and turns up while the list is open, and
 * the name speaks Expanded or Collapsed. The list itself speaks when it opens;
 * the door is where the closed state is read.
 */
@Composable
private fun ReciterDoor(
    name: String,
    onOpen: (() -> Unit)?,
    /** True while the voice list is on the glass. */
    expanded: Boolean = false,
) {
    val press = remember { MutableInteractionSource() }
    val expandedText = stringResource(UiKitR.string.door_expanded)
    val collapsedText = stringResource(UiKitR.string.door_collapsed)
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
            .focusRing(press, Radii.Pill)
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
            .padding(horizontal = Space.Tight, vertical = Space.Line)
            .testTag("playback-reciter")
            .semantics {
                contentDescription = name
                if (onOpen != null) {
                    stateDescription = if (expanded) expandedText else collapsedText
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (onOpen != null) {
            AppChevron(
                role = ChevronRole.Expand,
                expanded = expanded,
                tint = MaterialTheme.colorScheme.primary,
                size = GlyphSize.ChevronInline,
                modifier = Modifier.padding(start = Space.Tight),
            )
        }
    }
}

/**
 * What the pill says under the reciter's name while an ayah plays: the place,
 * in the reader's own digits.
 *
 * The pace does not stand here any more (owner decision, this session). It was
 * appended after the app's middot, which made it the first thing the line's
 * ellipsis cut: at the longest names the place alone is wider than the line,
 * so a pace trailing it was never read at all, and at the shorter ones it would
 * be read half cut ("1.2..."), which says the wrong thing. It stands beside the
 * reciter's name now, where it is a fact about the voice and where the line has
 * room to spare.
 *
 * What happens at the end of the audio is not said here either. The sentence it
 * takes does not fit the line the block has, and the switch that carries the
 * answer says its own state where the reader turns it: a repeat is evident
 * from the switch that is on, and the pill is not a third place the answer is
 * written down (owner report).
 */
@Composable
internal fun playbackPlace(reference: String): String = localizedDigits(reference)
