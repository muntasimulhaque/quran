package io.github.muntasimulhaque.quran.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.feature.search.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.glyph.GlyphSize
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.glyph.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Wash

@Composable
internal fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    onClose: () -> Unit,
    focus: FocusRequester,
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                // The field's own pill owes a finger the 48 dp floor every
                // other control holds; at 47 it was the one control under it
                // (owner decision, the theme and consistency session).
                .defaultMinSize(minHeight = 48.dp)
                .clip(Radii.Pill)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    // The pill is the field's own shape, so a tap anywhere
                    // inside it reaches the field: the words, and the quiet
                    // ground around them. The text field's own target is the
                    // line of text it draws, and the padding around that line
                    // used to answer nothing (owner decision, this session).
                    // The clear mark keeps its own tap beside this region, so
                    // emptying the field is never also a tap into it.
                    .pointerInput(focus) {
                        detectTapGestures(onTap = { focus.requestFocus() })
                    }
                    .padding(
                        start = 16.dp,
                        end = if (value.isEmpty()) 16.dp else ClearGap,
                        top = 12.dp,
                        bottom = 12.dp,
                    ),
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
                    // The keyboard's Search key is the reader saying the query
                    // is done; it closes the keyboard and leaves the results
                    // on the page. The query already runs as it settles.
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    // The hint is the only text in an empty field,
                                    // so it has to be read to be a hint at all. The
                                    // 0.55 alpha it wore measured 2.5:1, under the
                                    // theme's 4.5:1 rule.
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            // The mark that empties the field, drawn only while there is a
            // query to empty. It keeps the field focused and the keyboard up,
            // so the reader types their next word straight away rather than
            // reaching back for a backspace at the end of the line (owner
            // decision, this session).
            if (value.isNotEmpty()) {
                ClearFieldButton(
                    onClick = {
                        onChange("")
                        focus.requestFocus()
                    },
                )
            }
        }
        TextButton(
            label = stringResource(R.string.search_close),
            onClick = onClose,
            modifier = Modifier.padding(start = 8.dp),
            quiet = true,
        )
    }
}

/** The gap between the field's words and the clear mark that ends them. */
private val ClearGap = 2.dp

/** The drawn circle the clear mark's ripple fills inside its 48 dp target. */
private val ClearRipple = 40.dp

/**
 * The mark that empties the field: the standard door of a search, and a
 * different job from the Close beside the field. Close ends the search; this
 * empties it and leaves the reader ready to type another query. It appears
 * only while there is a query to clear, so it is never a dead mark on an
 * empty field. Its 48 dp node carries a 40 dp drawn circle for the press,
 * so the ripple stays clear of the field's own pill and the mark reads as a
 * mark inside the pill rather than a control the size of it (owner decision,
 * this session).
 */
@Composable
private fun ClearFieldButton(onClick: () -> Unit) {
    val description = stringResource(R.string.search_clear)
    val press = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(
                interactionSource = press,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(ClearRipple)
                .clip(CircleShape)
                .indication(press, ripple(color = MaterialTheme.colorScheme.onSurfaceVariant)),
            contentAlignment = Alignment.Center,
        ) {
            IconGlyph(
                icon = Icon.Close,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(GlyphSize.Control),
            )
        }
    }
}
