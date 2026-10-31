package io.github.muntasimulhaque.quran.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The reading's top bar, and the one place its margins live.
 *
 * The bar is summoned, never resident: it fades in over the page when the
 * reader asks for it and steps back on its own, which is the whole of the
 * chrome the reading carries. It lives beside the reading screen so the page
 * file is about the page, and the bar is about the bar.
 *
 * The three marks are Browse, Search, and Settings. The gear opens the
 * settings hub directly: the reading's display panel was a second door to
 * the same values, and a reader who wants a setting should not have to learn
 * a shortcut to it (owner decision, this session).
 *
 * Its marks stand on the same two lines as the words under them. A round
 * control is a 48 dp target holding a 24 dp mark, so the row is inset by the
 * app's one edge ([Space.Gutter]) less the 12 dp of air inside that target,
 * and Browse, Search, and Settings land exactly on the text they act on. The
 * row used to be the reading column's own width, centered, which left the
 * marks in the margins of a ten inch screen (owner report, this session); the
 * column is the glass now, so the bar is too (owner decision, this session).
 */

/** The bar's own margin: the app's edge less the air inside a round target. */
private val BarInset = Space.Gutter - 12.dp

@Composable
internal fun ReaderTopBar(
    title: String,
    detail: String?,
    visible: Boolean,
    reducedMotion: Boolean,
    /** What a tap on the title does, said out loud; null when it does nothing. */
    titleAction: String?,
    onTitle: () -> Unit,
    onBrowse: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = if (reducedMotion) fadeIn() else fadeIn() + slideInVertically { -it / 3 },
        exit = if (reducedMotion) fadeOut() else fadeOut() + slideOutVertically { -it / 3 },
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The scrim is solid behind the controls and gives way below
                // them, so the surah never sits on the fading edge. It used to
                // stay solid to seventy-two percent of its own height, which
                // is exactly where the page's own surah name falls: the gold
                // name was measurably washed every time the bar came up (the
                // ornament's contrast dropped from 5.0 to 4.4 on paper). The
                // solid run is now the bar's own height, and the fade is short,
                // so the page's first line is never under it.
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background,
                        0.55f to MaterialTheme.colorScheme.background,
                        0.82f to MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                )
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = BarInset, end = BarInset, top = Space.Line, bottom = Space.Gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The bar is one row: Browse leads, the surah
                // and its juz sit centered, and Search and
                // Settings close it.
                IconButton(Icon.Browse, stringResource(R.string.action_browse), onBrowse)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Space.Tight),
                    contentAlignment = Alignment.Center,
                ) {
                    ReadingTitle(
                        surah = title,
                        detail = detail,
                        action = titleAction,
                        onClick = onTitle,
                    )
                }
                IconButton(Icon.Search, stringResource(R.string.action_search), onSearch)
                IconButton(Icon.Settings, stringResource(R.string.action_settings), onSettings)
            }
        }
    }
}
