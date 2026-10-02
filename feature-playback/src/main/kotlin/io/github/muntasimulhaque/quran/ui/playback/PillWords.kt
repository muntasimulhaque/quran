package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import kotlin.math.abs

/**
 * The pill's words on one line, which is the shape the two-row pill wears:
 * who is reading, and where the reader is, side by side, with the whole line
 * centred on the pill and the controls centred under it (owner report).
 *
 * The reciter's name is the door to the reciter chooser and the line beside it
 * is the door to the listening menu, which wears a chevron so it can be found
 * without a guess. Neither door is the line around it: the row is a container
 * for two controls, and a container that answered taps would take both.
 *
 * Nothing else is written here, because the pill is a control over the page,
 * and a control that has to say a sentence stops being one (owner report).
 *
 * The two words carry no menu of their own. A menu takes its shape from the
 * anchor it hangs on, and the anchor the reader wants is the pill's own
 * centre, not the edge of the word they touched (owner report), so the
 * capsule holds the menu and these two only say which one was asked for.
 */
@Composable
internal fun PlaybackWordsLine(
    reciterName: String,
    /** The door to the chooser, or null where there is no choice to make. */
    onOpenReciters: (() -> Unit)?,
    status: String,
    /** The door to the pace and the end of the audio, or null where there is nothing to hear. */
    onOpenListening: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("playback-words"),
        horizontalArrangement = Arrangement.spacedBy(WordsGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterDoor(name = reciterName, onOpen = onOpenReciters)
        PlaybackStatusLine(text = status, onOpen = onOpenListening)
    }
}

/**
 * The pill's words on two lines, which is the shape the one-row pill wears: a
 * tablet or a landscape phone has the measure for both lines beside four
 * controls, and this is how they read there.
 *
 * The reciter's name is the door to the reciter chooser and the line under
 * it is the door to the listening menu, which wears a chevron so it can be
 * found without a guess. Two doors on two lines, and the column around them
 * is not a door of its own. The menus themselves hang from the capsule's own
 * centre, as they do on the phone's one line.
 */
@Composable
internal fun PlaybackWords(
    reciterName: String,
    onOpenReciters: (() -> Unit)?,
    status: String,
    onOpenListening: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("playback-words")) {
        ReciterDoor(name = reciterName, onOpen = onOpenReciters)
        PlaybackStatusLine(text = status, onOpen = onOpenListening)
    }
}

/**
 * The reciter's name, and the door it is to: the other reciters, with what
 * each of them would still need for the surah the reader is hearing, and a
 * check on the one in use.
 *
 * The name wears a chevron the way every other door in the app does, so the
 * choice is found without a guess. It is a chooser and not a hop to the
 * settings: a reader who wants another voice is not asking where the voices
 * are listed. The settings page is where the voices are managed, with the
 * surahs each one has on the device, not where one is picked for the ayah
 * being heard (owner report).
 *
 * Null where the choice cannot be made now, which is while a package is on
 * its way: the chevron is gone rather than a door that does nothing.
 */
@Composable
private fun ReciterDoor(
    name: String,
    onOpen: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (onOpen == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onOpen)
                },
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
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
                    .size(12.dp),
            )
        }
    }
}

/**
 * Where the reader is, and, while a recitation plays, the door to the pace
 * and to what happens at the end of the audio. The door is the line itself
 * and it wears a chevron so it can be found without a guess; the chevron is
 * gone when there is nothing behind it.
 *
 * It is the second line of the wide pill's pair of words and the second half
 * of the phone pill's one line, and it is the same composable in both: one
 * place says what the status is, and the capsule says which menu that line
 * opened.
 */
@Composable
private fun PlaybackStatusLine(
    text: String,
    onOpen: (() -> Unit)?,
) {
    if (onOpen == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(end = 2.dp)
            .testTag("playback-listening")
            .semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        IconGlyph(
            icon = Icon.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(12.dp),
        )
    }
}

/**
 * What the pill says under the reciter's name while an ayah plays: where the
 * reader is, and, only when it is not the ordinary one, the pace.
 *
 * What happens at the end of the audio is not said here. The sentence it
 * takes does not fit the line the pill has, a name and a place sharing one
 * line, and the switch that carries the answer says its own state where the
 * reader turns it: a repeat is evident from the switch that is on, and the
 * pill is not a third place the answer is written down (owner report).
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
    return (listOf(reference) + extras).filter { it.isNotBlank() }.joinToString(" \u00b7 ")
}
