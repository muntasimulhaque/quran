package io.github.muntasimulhaque.quran.ui.rich

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import io.github.muntasimulhaque.quran.core.TajweedCodec
import io.github.muntasimulhaque.quran.ui.theme.TajweedColors

/**
 * The one place the reading's colors are applied.
 *
 * The QPC code is one character per character of the text, and the color is
 * the family the rule belongs to. The caller chooses the palette: the page's
 * own for the reading, the paper palette for a card that always wears paper.
 * [enabled] is the reader's Tajweed switch, so every surface that draws the
 * Book's words answers the switch the same way (owner report, the QPC rebuild
 * audit: the switch reached the verse and not the basmalah, the word aid, or
 * the size sample).
 *
 * A code line whose length does not match the text is not drawn: a missing
 * byte is a stale pack or a build mistake, and a color span placed from a
 * shorter line would paint the wrong letters. The text itself is always
 * drawn.
 */
fun tajweedText(
    text: String,
    code: String,
    colors: TajweedColors,
    enabled: Boolean = true,
): AnnotatedString = buildAnnotatedString {
    append(text)
    tajweed(code, text.length, colors, enabled)
}

/**
 * Applies the code to a line that is already in the builder. [textLength] is
 * the length of the ayah's own text, which is the part the code describes;
 * an ornament or a number appended after it keeps the base style.
 */
fun AnnotatedString.Builder.tajweed(
    code: String,
    textLength: Int,
    colors: TajweedColors,
    enabled: Boolean = true,
) {
    if (!enabled || code.length != textLength) return
    for (span in TajweedCodec.spans(code)) {
        addStyle(SpanStyle(color = colors.of(span.rule)), span.start, span.end)
    }
}
