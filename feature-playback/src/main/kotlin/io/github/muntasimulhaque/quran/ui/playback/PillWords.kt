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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.feature.playback.R
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.kit.localizedDigits
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import kotlin.math.min
import io.github.muntasimulhaque.quran.ui.theme.compactLabel

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
 * The offer's one line: the reciter's name, the chevron that says the line
 * opens, then the surah and its size.
 *
 * **The line is a door** (owner decision, this session). Its row carries the
 * app's 48 dp target around a 35 dp flash, so the name can open the voice
 * list without reaching into the answers' row. The chevron stands 4 dp after
 * the name, the same door mark the playing pill wears, and the surah follows
 * 8 dp after it. The name is the accent's 13, the surah and its size the
 * compact 14; when the line is long the size is what gives way, because the
 * surah's name is the fact the reader is judging.
 */
@Composable
internal fun OfferLine(
    name: String,
    title: String,
    /** The door the line is, or null where the voice cannot be chosen now. */
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** True while the title is a failure the reader has to hear. */
    announce: Boolean = false,
) {
    val press = remember { MutableInteractionSource() }
    Row(
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
                            onClickLabel = stringResource(R.string.playback_change_reciter),
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
                    Modifier.indication(press, ripple(color = MaterialTheme.colorScheme.primary))
                },
            )
            .padding(horizontal = Space.Tight, vertical = Space.Line),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onOpen != null) {
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = Space.Tight)
                    .size(GlyphSize.ChevronInline),
            )
        }
        Text(
            text = title,
            style = compactLabel(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = Space.Line)
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
 * The playing pill's words: the voice and its pace, then the place and its
 * key, on one line.
 *
 * **The voice leads, then the place** (owner decision, this session). The
 * offer says who, then which surah; the playing pill now says who, then
 * where, so the pill's two states read with one grammar and every voice of
 * the pill leads with the reciter's name. The order is the chosen voice, the
 * pace when it is not 1x, the surah's own name at the card's 14 Medium, and
 * the ayah's key; the door's chevron closes the line. The pace is drawn only
 * when it is not 1x, because 1x is the pace the app starts at and a line does
 * not need to say it.
 *
 * **The place and the key never give way; the voice does** (owner decision,
 * this session). The pill's type follows the system scale only up to the
 * app's largest step, 1.2, and stops there. The place, the key, and the pace
 * keep their ends; at the longest place with a non-1x pace the voice is the
 * part an ellipsis takes, which is the rule the pill already kept. The
 * reading and the listening panel still take the reader's own scale; the pill
 * is a control with a fixed line.
 *
 * The block is 48 dp tall inside the capsule, so the line and the chevron
 * are not the whole of what a finger has to hit; its 8 dp inset keeps the
 * type off its own press shape, and the caller keeps [PillEndAir] at each end
 * where the capsule's end is a sphere.
 */
@Composable
internal fun PlayingWordsBlock(
    /** The surah's name in the reader's language; null while it is not known. */
    placeName: String?,
    /** The reciter's name. */
    reciter: String,
    /** The ayah's reference, already in the reader's digits. */
    reference: String,
    /** The reader's pace, drawn only when it is not the app's own 1x. */
    speed: Float,
    /** The door the block is, or null where there is nothing to open. */
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    // The pill is a control with a fixed line, so its type follows the system
    // scale only up to the app's largest text step. Past it the line would
    // cut the place, and a cut place is worse than a control that holds its
    // measure; the reading and the listening panel still take the reader's
    // own scale (owner decision, this session).
    val density = LocalDensity.current
    val capped = Density(density.density, min(density.fontScale, TextSize.STEPS.last()))
    Row(
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
            .focusRing(press, Radii.Pill)
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.indication(press, ripple(color = MaterialTheme.colorScheme.primary))
                },
            )
            .padding(horizontal = Space.Tight, vertical = Space.Line),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalDensity provides capped) {
            Text(
                text = reciter,
                // The voice leads: the accent's 13, and the one part of the
                // line that gives way when the top step and the pace leave
                // it no room (owner decision, this session).
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (speed != 1f) {
                Text(
                    text = speedText(speed),
                    // The pace is the pill's read-back after the panel closes:
                    // the quiet value beside the voice, not a second title
                    // (owner decision, this session).
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = Space.Line),
                )
            }
            if (!placeName.isNullOrBlank()) {
                Text(
                    text = placeName,
                    // The place follows the voice: the card's 14 Medium, in
                    // the page's ink, and it is never the thing an ellipsis
                    // takes (owner decision, this session).
                    style = compactLabel(),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Space.Line),
                )
            }
            Text(
                text = reference,
                // The ayah key closes the place: 13 at Regular, the precise
                // reference beside the surah's own name. The key never gives
                // way (owner decision, this session).
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(start = Space.Tight),
            )
            if (onOpen != null) {
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = Space.Tight)
                        .size(GlyphSize.ChevronInline),
                )
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
            .semantics { contentDescription = name },
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
            IconGlyph(
                icon = Icon.Chevron,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = Space.Tight)
                    .size(GlyphSize.ChevronInline),
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
