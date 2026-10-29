package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The one sheet every surface in the app is raised in: the footnote, the ayah
 * card, the note, the share, the remove question, Search, Browse, and
 * Settings.
 *
 * A sheet is the app's one page raised over the reading, so the page's width,
 * its ground, and its handle are decided once, here, instead of in every
 * caller. A new sheet gets the same still page as the rest, and a change to
 * the way a sheet behaves lands in one file.
 *
 * The handle is the app's own [SheetHandle], drawn as the first thing inside
 * the sheet rather than handed to Material's drag-handle slot. Material wraps
 * that slot in a focusable clickable of its own: from Material3 1.5 the sheet
 * hands its first focus to that clickable when it opens, and the clickable's
 * focus layer painted a gray square behind the hand-drawn bar in every sheet
 * (found in the store frames after the upgrade). The app's handle is a drag
 * mark, not a button, and its tap never took the sheet away (owner decision,
 * the 9.1 session); drawn here it stays outside Material's clickable, so it
 * has no stock state layer to show, and the surface still drags under it
 * because Material's draggable belongs to the whole sheet.
 *
 * The width is [rememberSheetWidth], which is the page and its two gutters: on
 * a phone and on the seven inch tablet standing up the glass is narrower than
 * that and the sheet is the glass, and on the ten inch screen the sheet is a
 * 656 dp page centered on it, with the reading's own 600 dp page behind and
 * the rest of the glass as margin. A sheet is always the page it holds and
 * never more, so a settings row's switch stays as close to the words that
 * name it on the ten inch screen as it is on the seven inch tablet (owner
 * decision, this session).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberExpandedSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetMaxWidth = rememberSheetWidth(),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally))
        content()
    }
}
