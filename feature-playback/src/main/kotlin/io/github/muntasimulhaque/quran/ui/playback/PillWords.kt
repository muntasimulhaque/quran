package io.github.muntasimulhaque.quran.ui.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.core.EndOfAudio
import io.github.muntasimulhaque.quran.playback.ListenOption
import io.github.muntasimulhaque.quran.ui.kit.speedText
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import kotlin.math.abs

/**
 * The pill's words: the reciter who is reading, where the reader is, and the
 * two doors those two lines are. Nothing else is written here, because the
 * pill is a control over the page, and a control that has to say a sentence
 * stops being one (owner report).
 */

/**
 * The pill's words on one line, which is the shape the two-row pill wears:
 * who is reading, and where the reader is, side by side, with the whole line
 * centred on the pill and the controls centred under it (owner report).
 *
 * The reciter's name is the door to the reciter chooser and the line beside it
 * is the door to the listening menu, which wears a chevron so it can be found
 * without a guess. Neither door is the line around it: the row is a container
 * for two controls, and a container that answered taps would take both.
 */
@Composable
internal fun PlaybackWordsLine(
    reciterName: String,
    reciterId: String?,
    reciterOptions: List<ListenOption>,
    onReciter: ((String) -> Unit)?,
    status: String,
    /** The pace and the end of the audio, or null where there is nothing to hear. */
    onListening: Pair<(Float) -> Unit, (EndOfAudio) -> Unit>?,
    speed: Float,
    end: EndOfAudio,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.testTag("playback-words"),
        horizontalArrangement = Arrangement.spacedBy(WordsGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReciterDoor(
            name = reciterName,
            selected = reciterId,
            options = reciterOptions,
            onChoose = onReciter,
        )
        PlaybackStatusLine(
            text = status,
            onListening = onListening,
            speed = speed,
            end = end,
        )
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
 * is not a door of its own.
 */
@Composable
internal fun PlaybackWords(
    reciterName: String,
    reciterId: String?,
    reciterOptions: List<ListenOption>,
    onReciter: ((String) -> Unit)?,
    status: String,
    /** The pace and the end of the audio, or null where there is nothing to hear. */
    onListening: Pair<(Float) -> Unit, (EndOfAudio) -> Unit>?,
    speed: Float,
    end: EndOfAudio,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("playback-words")) {
        ReciterDoor(
            name = reciterName,
            selected = reciterId,
            options = reciterOptions,
            onChoose = onReciter,
        )
        PlaybackStatusLine(
            text = status,
            onListening = onListening,
            speed = speed,
            end = end,
        )
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
    selected: String?,
    options: List<ListenOption>,
    onChoose: ((String) -> Unit)?,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .then(
                    if (onChoose == null) {
                        Modifier
                    } else {
                        Modifier.clickable(role = Role.Button) { open = true }
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
            if (onChoose != null) {
                IconGlyph(
                    icon = Icon.Chevron,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(12.dp),
                )
            }
        }
        if (onChoose != null) {
            ReciterMenu(
                open = open,
                options = options,
                selected = selected,
                onChoose = {
                    open = false
                    onChoose(it)
                },
                onDismiss = { open = false },
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
 * place decides what the status says and what its door opens.
 */
@Composable
private fun PlaybackStatusLine(
    text: String,
    onListening: Pair<(Float) -> Unit, (EndOfAudio) -> Unit>?,
    speed: Float,
    end: EndOfAudio,
) {
    var open by remember { mutableStateOf(false) }
    if (onListening == null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button) { open = true }
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
        ListeningMenu(
            open = open,
            onDismiss = { open = false },
            speed = speed,
            end = end,
            onSpeed = onListening.first,
            onEndOfAudio = onListening.second,
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
