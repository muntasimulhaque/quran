package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.muntasimulhaque.quran.core.Search
import io.github.muntasimulhaque.quran.core.SearchQuery
import io.github.muntasimulhaque.quran.data.Ayah
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.SearchHit
import io.github.muntasimulhaque.quran.data.Surah
import io.github.muntasimulhaque.quran.data.Word
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LatinReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** More than a screenful of scroll is not a search; the rest is a narrower query. */
private const val LIMIT = 200

/**
 * The search sheet: one field over the Arabic text, the translation, and the
 * surah names. Results stay in Mushaf order, matched Arabic words are marked
 * as whole words, matched English is marked inside the sentence, and a tap
 * takes the reader to the ayah itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSheet(
    content: ContentDatabase,
    onDismiss: () -> Unit,
    onAyah: (Ayah, Int) -> Unit,
    onSurah: (Surah) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focus = remember { FocusRequester() }
    val hafs = remember {
        FontFamily(Font(path = "fonts/UthmanicHafs_V22.ttf", assetManager = context.assets))
    }

    var text by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
    var capped by remember { mutableStateOf(false) }
    val query = remember(text) { Search.parse(text) }

    LaunchedEffect(Unit) { sheetState.expand() }

    // Focus once the sheet has fully settled; asking earlier loses it to the window.
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) focus.requestFocus()
    }

    LaunchedEffect(query) {
        if (query == null) {
            hits = emptyList()
            capped = false
            return@LaunchedEffect
        }
        // A breath between keystrokes; the query itself is a local read.
        delay(200)
        val results = withContext(Dispatchers.IO) { content.search(query, LIMIT + 1) }
        capped = results.size > LIMIT
        hits = results.take(LIMIT)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            SearchField(
                value = text,
                onChange = { text = it },
                onClose = onDismiss,
                focus = focus,
            )
            StatusLine(text = text, query = query, count = hits.size, capped = capped)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                items(hits, key = { key(it) }) { hit ->
                    when (hit) {
                        is SearchHit.SurahHit -> SurahRow(hit.surah, onSurah)
                        is SearchHit.AyahHit -> AyahRow(
                            hit = hit,
                            terms = if (query?.arabic == false) query.terms else emptyList(),
                            hafs = hafs,
                            onAyah = onAyah,
                        )
                    }
                }
            }
        }
    }
}

private fun key(hit: SearchHit): String = when (hit) {
    is SearchHit.SurahHit -> "surah-${hit.surah.number}"
    is SearchHit.AyahHit -> "ayah-${hit.ayah.number}"
}

@Composable
private fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
    focus: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Search the Quran",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        inner()
                    }
                },
            )
            if (value.isNotEmpty()) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { onChange("") }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            text = "Close",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun StatusLine(text: String, query: SearchQuery?, count: Int, capped: Boolean) {
    val message = when {
        text.isBlank() -> "Search the Arabic text, the translation, or a surah name."
        query == null -> "Type at least two letters."
        count == 0 -> "No matches."
        capped -> "$count+ matches"
        else -> if (count == 1) "1 match" else "$count matches"
    }
    Text(
        text = message,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp),
    )
}

@Composable
private fun SurahRow(surah: Surah, onSurah: (Surah) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSurah(surah) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Surah",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = surah.nameSimple,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "${surah.versesCount} ayahs  ·  ${surah.revelationPlace}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = surah.nameArabic,
            style = TextStyle(fontFamily = Amiri, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}

@Composable
private fun AyahRow(
    hit: SearchHit.AyahHit,
    terms: List<String>,
    hafs: FontFamily,
    onAyah: (Ayah, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAyah(hit.ayah, hit.page) }
            .padding(horizontal = 22.dp, vertical = 14.dp),
    ) {
        Text(
            text = hit.ayah.verseKey,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = arabic(hit.words, hit.matchedPositions, hit.ayah.text),
            style = TextStyle(
                fontFamily = hafs,
                fontSize = 22.sp,
                lineHeight = 42.sp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth(),
        )
        hit.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = english(translation, terms),
                style = LatinReading.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The ayah as whole words; a matched word takes the lapis color. */
@Composable
private fun arabic(words: List<Word>, matched: Set<Int>, fallback: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    if (words.isEmpty()) return AnnotatedString(fallback)
    return buildAnnotatedString {
        words.forEachIndexed { index, word ->
            if (index > 0) append(' ')
            if (word.position in matched) {
                withStyle(SpanStyle(color = primary)) { append(word.text) }
            } else {
                append(word.text)
            }
        }
    }
}

/** Matched words in the translation, marked whole; folding hides diacritics. */
@Composable
private fun english(text: String, terms: List<String>): AnnotatedString {
    if (terms.isEmpty()) return AnnotatedString(text)
    val primary = MaterialTheme.colorScheme.primary
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val codepoint = text.codePointAt(index)
            if (isWordCodepoint(codepoint)) {
                var end = index + Character.charCount(codepoint)
                while (end < text.length && isWordCodepoint(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end))
                }
                val word = text.substring(index, end)
                val folded = Search.normalizeEnglish(word)
                if (terms.any { folded.contains(it) }) {
                    withStyle(SpanStyle(color = primary, fontWeight = FontWeight.Medium)) { append(word) }
                } else {
                    append(word)
                }
                index = end
            } else {
                append(String(Character.toChars(codepoint)))
                index += Character.charCount(codepoint)
            }
        }
    }
}

private fun isWordCodepoint(codepoint: Int): Boolean =
    Character.isLetterOrDigit(codepoint) || codepoint == '\''.code || codepoint == 0x2019
