package io.github.muntasimulhaque.quran.ui.browse

import androidx.compose.foundation.layout.Arrangement
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
import io.github.muntasimulhaque.quran.data.SavedAyah
import io.github.muntasimulhaque.quran.feature.browse.R
import io.github.muntasimulhaque.quran.ui.kit.EmptyNote
import io.github.muntasimulhaque.quran.ui.kit.SheetRowGutter
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.sheetRow
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * Where the reader has been reading, one row per reading, newest first. Each
 * row names the place the way a reader says it: the surah's own name on the
 * first line, the ayah under it ("Al-Fatihah" then "Ayah 1"), and the
 * moment as the
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
            // The note stands in a page that begins under the sheet's own
            // head, so it keeps the air it has always kept there.
            modifier = Modifier.padding(vertical = 60.dp),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = PaddingValues(bottom = Space.SheetFoot),
    ) {
        items(places, key = { it.id }) { place ->
            val text = texts[place.ayahNumber]
            PlaceRow(
                surah = text?.surahName
                    ?: stringResource(R.string.saved_reference_fallback, place.ayahNumber),
                ayah = text?.ayahLabel,
                detail = stringResource(
                    R.string.last_read_detail,
                    moment(place.readAt),
                ),
                onClick = { onAyah(place.ayahNumber) },
                action = stringResource(R.string.action_forget) to { onForget(place.ayahNumber) },
            )
        }
    }
}


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
 * Everything the reader kept: every saved ayah, with the note written on it
 * under the place when there is one, and nothing under the rows without.
 * Save and Note are two actions but one list, because a note is written on a
 * kept ayah and a reader looking for either is looking for the same thing
 * (owner decision).
 *
 * The order is the most recent of the row's own moments, not the save's
 * alone: an ayah saved last year and noted today is today's row, and the
 * reader looking for what they last wrote must find it at the top. The
 * detail line names which moment it is, so a note is never dated as a save.
 * A row saved before the moments existed keeps its row's own moment, the
 * closest truth the migrations could write.
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
    val rows = remember(saved) {
        saved.filter { it.saved || !it.note.isNullOrBlank() }
            .sortedByDescending { maxOf(it.savedAt ?: it.createdAt, it.noteAt ?: 0L) }
    }
    if (rows.isEmpty()) {
        EmptyNote(
            title = stringResource(R.string.saved_empty_title),
            body = stringResource(R.string.saved_empty_body),
            modifier = Modifier.padding(vertical = 60.dp),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = listModifier,
        contentPadding = PaddingValues(bottom = Space.SheetFoot),
    ) {
        items(rows, key = { it.ayahNumber }) { row ->
            val text = texts[row.ayahNumber]
            val note = row.note?.trim()
            PlaceRow(
                surah = text?.surahName ?: stringResource(R.string.saved_reference_fallback, row.ayahNumber),
                ayah = text?.ayahLabel,
                note = note,
                detail = if (!note.isNullOrBlank()) {
                    stringResource(R.string.notes_detail, moment(row.noteAt ?: row.createdAt))
                } else {
                    stringResource(R.string.saved_detail, moment(row.savedAt ?: row.createdAt))
                },
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
    note: String? = null,
    onClick: () -> Unit,
    action: Pair<String, () -> Unit>? = null,
) {
    // Two steps, and the same two everywhere: the ayah is the name's own
    // label and stands `Space.Tight` under it, exactly as a surah's place and
    // ayah count stand under its name in Browse's two numbered lists, while
    // the note and the moment are the row's own facts and stand `Space.Line`
    // below. Each line used to carry its own padding, 4 dp above the ayah and
    // 8 dp above the rest, which cascaded with the fonts' line boxes into 11,
    // 16, and 18 dp steps in one row; the row then went to one 8 dp gap for
    // every line, which unified the row and left the name's label further from
    // its name than the same pair sits one tab away in Surahs (owner report,
    // this session). The eye reads the name and the ayah as one block, and the
    // moment or the note as the row's own line about it.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .sheetRow(onClick = onClick)
            .padding(horizontal = Space.Gutter - SheetRowGutter, vertical = Space.Row),
        verticalArrangement = Arrangement.spacedBy(Space.Line),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Space.Tight),
            ) {
                Text(
                    text = surah,
                    style = MaterialTheme.typography.titleMedium,
                    // A place's name is the row's name, and every other
                    // row's name is the theme's own ink; the accent here
                    // read as a link or a selection the row does not have
                    // (owner decision, the theme and consistency session).
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!ayah.isNullOrBlank()) {
                    Text(
                        text = ayah,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Remove and Forget are actions, so they wear the app's button
            // shape: the two never read as the place's own name.
            action?.let {
                TextButton(
                    label = it.first,
                    onClick = it.second,
                    modifier = Modifier.padding(start = Space.Row),
                    quiet = true,
                )
            }
        }
        if (!note.isNullOrBlank()) {
            // The reader's own words are the one part of the row drawn in the
            // reading ink and the reading face, and the only part allowed two
            // lines: a note is how the place is recognised, while the place
            // and the moment are its labels. The clamp is the same two lines
            // on every row, so one long note cannot change the list's rhythm.
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = LocalReadingVoice.current.style.fontFamily,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
