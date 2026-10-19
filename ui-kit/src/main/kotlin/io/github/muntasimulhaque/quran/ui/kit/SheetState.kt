package io.github.muntasimulhaque.quran.ui.kit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable

/**
 * The one sheet state every sheet in the app stands on: Hidden or Expanded,
 * never in between, shown from Hidden.
 *
 * A sheet here is the app's one page raised over the reading. It opens whole
 * and leaves whole, and no sheet ever rests at a half height, so the state
 * asks the library for exactly those two places. Material3 1.5 deprecates
 * `rememberModalBottomSheetState`, the call the sheets used to make, and
 * naming the two places is all that call ever meant here: every sheet passed
 * `skipPartiallyExpanded = true`. One helper keeps the choice in one file
 * instead of seven, and the day a sheet wants to rest part way up this is
 * the one place that changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberExpandedSheetState(): SheetState =
    rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
