package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.ReadPlace
import io.github.muntasimulhaque.quran.data.ReadingMode
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Where the reader has been reading, newest first. Each row names the place
 * the way a reader says it: the surah's own name on the first line, the ayah
 * under it ("Al-Fatihah" then "Ayah 1"), and the mode and the moment as the
 * quiet third line, so a place is recognised by its name first and by when it
 * was left second.
 *
 * The ayah's own text is deliberately not drawn here. A place is a place:
 * the reader is looking for where they were, and a list of Arabic lines and
 * translations makes them read every row to find it. The text is one tap
 * away, which is the whole point of the row.
 */
@Composable
internal fun LastReadList(
    places: List<ReadPlace>,
    texts: Map<Int, AyahText>,
    listState: LazyListState,
    listModifier: Modifier = Modifier,
    onAyah: (Int) -> Unit,
    onForget: (Int) -> Unit,
) {
    if (places.isEmpty()) {
        EmptyNote(
            title = stringResource(R.string.last_read_empty_title),
            body = stringResource(R.string.last_read_empty_body),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        items(places, key = { it.ayahNumber }) { place ->
            val text = texts[place.ayahNumber]
            PlaceRow(
                surah = text?.surahName
                    ?: stringResource(R.string.saved_reference_fallback, place.ayahNumber),
                ayah = text?.ayahLabel,
                detail = stringResource(
                    R.string.last_read_detail,
                    readingModeName(place.mode),
                    moment(place.readAt),
                ),
                onClick = { onAyah(place.ayahNumber) },
                action = stringResource(R.string.action_forget) to { onForget(place.ayahNumber) },
            )
        }
    }
}

/**
 * The ayahs the reader wrote a note on, newest note first. The note itself is
 * not drawn here: a note is the reader's own writing, and the row's work is
 * to name the place that holds it. A tap opens the note where it was written,
 * on its ayah: the reader tapped a row in the Notes list because they want to
 * read or change what they wrote, and sending them to the ayah to hunt for it
 * would be a second errand. The Remove beside the row is the other end of the
 * same work: the note is the reader's own, so they can take it back here.
 */
@Composable
internal fun NotesList(
    saved: List<SavedAyah>,
    texts: Map<Int, AyahText>,
    listState: LazyListState,
    listModifier: Modifier = Modifier,
    onNote: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    // The order is the note's own, not the ayah's: a note written today on an
    // ayah saved last year is today's note, and the reader looking for what
    // they last wrote must find it at the top. A note from before this
    // column existed falls back to the moment the ayah was saved.
    val notes = saved
        .filter { !it.note.isNullOrBlank() }
        .sortedByDescending { it.noteAt ?: it.createdAt }
    if (notes.isEmpty()) {
        EmptyNote(
            title = stringResource(R.string.notes_empty_title),
            body = stringResource(R.string.notes_empty_body),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        items(notes, key = { it.ayahNumber }) { row ->
            val text = texts[row.ayahNumber]
            PlaceRow(
                surah = text?.surahName
                    ?: stringResource(R.string.saved_reference_fallback, row.ayahNumber),
                ayah = text?.ayahLabel,
                detail = stringResource(R.string.notes_detail, moment(row.noteAt ?: row.createdAt)),
                onClick = { onNote(row.ayahNumber) },
                action = stringResource(R.string.action_remove) to { onRemove(row.ayahNumber) },
            )
        }
    }
}

@Composable
private fun readingModeName(mode: ReadingMode): String = stringResource(
    when (mode) {
        ReadingMode.Mushaf -> R.string.last_read_mode_mushaf
        ReadingMode.Study -> R.string.last_read_mode_study
    },
)

/**
 * The names one row draws: the surah as a reader says it, and the ayah's own
 * number under it. The ayah's text is not carried at all: every list that
 * draws these rows is a list of places, and the reading is one tap away.
 */
internal data class AyahText(
    val surahName: String? = null,
    val ayahLabel: String? = null,
)

/**
 * The ayahs the reader saved. A row is the place and the way to unsave it:
 * the ayah's own text and its note are not repeated here. Saved means saved,
 * so this list holds the Save action's work and nothing else; a note written
 * on an ayah never puts it here, and a saved ayah that also has a note is
 * named again in Notes where the note itself lives.
 */
@Composable
internal fun SavedList(
    saved: List<SavedAyah>,
    texts: Map<Int, AyahText>,
    listState: LazyListState,
    listModifier: Modifier = Modifier,
    onAyah: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val rows = remember(saved) { saved.filter { it.saved } }
    if (rows.isEmpty()) {
        EmptyNote(
            title = stringResource(R.string.saved_empty_title),
            body = stringResource(R.string.saved_empty_body),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        items(rows, key = { it.ayahNumber }) { row ->
            val text = texts[row.ayahNumber]
            PlaceRow(
                surah = text?.surahName ?: stringResource(R.string.saved_reference_fallback, row.ayahNumber),
                ayah = text?.ayahLabel,
                onClick = { onAyah(row.ayahNumber) },
                action = stringResource(R.string.action_remove) to { onRemove(row.ayahNumber) },
            )
        }
    }
}

/**
 * One row in the reader's own lists: a place, named the way a reader says it,
 * and at most one action the list's work allows. The ayah's text is
 * deliberately not part of the row. The reader is looking for where they
 * were, or what they wrote, and a list of Arabic lines and translations
 * makes them read every row to find it; the text itself is one tap away, and
 * so is the row itself, which is the door.
 */
@Composable
private fun PlaceRow(
    surah: String,
    ayah: String?,
    detail: String? = null,
    onClick: () -> Unit,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = surah,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!ayah.isNullOrBlank()) {
                    Text(
                        text = ayah,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            // Remove and Forget are actions, so they wear the app's button
            // shape: the two never read as the place's own name.
            action?.let {
                TextButton(
                    label = it.first,
                    onClick = it.second,
                    modifier = Modifier.padding(start = 10.dp),
                    quiet = true,
                )
            }
        }
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Line),
            )
        }
    }
}

@Composable
private fun EmptyNote(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/**
 * A moment in plain words: the reader reads this list to find a place they
 * left, and "yesterday" is a better answer than a timestamp. Each unit is its
 * own plural, so a count of one reads as one.
 */
@Composable
private fun moment(at: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - at) / 60_000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.last_read_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.last_read_minutes, minutes.toInt(), minutes)
        minutes < 60 * 24 -> {
            val hours = minutes / 60
            pluralStringResource(R.plurals.last_read_hours, hours.toInt(), hours)
        }
        minutes < 60 * 24 * 7 -> {
            val days = minutes / (60 * 24)
            pluralStringResource(R.plurals.last_read_days, days.toInt(), days)
        }
        else -> {
            val weeks = minutes / (60 * 24 * 7)
            pluralStringResource(R.plurals.last_read_weeks, weeks.toInt(), weeks)
        }
    }
}

@Composable
internal fun placeName(place: String): String =
    if (place.equals("makkah", true)) {
        stringResource(R.string.place_makkah)
    } else {
        stringResource(R.string.place_madinah)
    }
