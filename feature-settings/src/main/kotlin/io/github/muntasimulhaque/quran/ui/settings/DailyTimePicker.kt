package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.muntasimulhaque.quran.core.clockHourText
import io.github.muntasimulhaque.quran.core.clockPeriodText
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.AppChip
import io.github.muntasimulhaque.quran.ui.kit.ChipKind
import io.github.muntasimulhaque.quran.ui.kit.ChipStyle
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.theme.ControlMetrics
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.screenSubtitle
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** The tape's cell: the app's one 48 dp target, so a number is a full stop. */
private val TapeCell = ControlMetrics.Target

/**
 * The room the pinned foot takes under the fields: its target, the gap over
 * it, and its own padding. The tape is capped by what is left, so Set stays
 * on the glass at every text size.
 */
private val FooterRoom = ControlMetrics.Target + Space.Block + Space.Gutter

/**
 * The hour the daily ayah arrives in, chosen on the app's own time page.
 *
 * The page is drawn rather than borrowed: the Material clock was the one
 * stock surface left in an app that draws every other control, and it opened
 * on a 72 sp header block that stood over its own question. The new page keeps
 * the app's grammar instead. The question is a small page's name at 16; the
 * hour is the subject, set in the interface's tabular digits; and the hour is
 * a tape the reader scrolls, with the chosen number in the accent under a thin
 * tick and its neighbors quiet on either side (owner decision, 13.9).
 *
 * **The minute left with the exact alarm** (owner decision, 23.8). The page
 * kept every minute of the day because the alarm promised the minute; the
 * alarm is now the platform's batched one, and the reminder says "around".
 * An hour is the honest unit for a nudge, so the minute tape, the typed field,
 * and the colon are gone, and the page is one tape and the AM/PM pair. A
 * stored half hour from before is read as its hour and keeps its moment until
 * the reader next sets it.
 *
 * The tape speaks the phone's own clock: 0 to 23 where the phone is 24 hour,
 * 1 to 12 with an AM/PM pair where it is not, so the page and the row that
 * opened it say the same hour in the same language. The tape is one TalkBack
 * node with a range and a `setProgress` action, so a reader can choose without
 * aiming at a number; a sighted reader can drag the tape or tap the number
 * they want and the tape carries it to the middle. The numbers are drawn in
 * the interface's own digits, so the page says the hour in the reader's
 * language.
 *
 * The line under the tape says what the hour means: the ayah arrives around
 * it. It is the page's only sentence, and it is the promise the batched alarm
 * can keep. Set is the page's one answer and Cancel its quiet exit, in the
 * app's own two button shapes. The dialog is the app's floating cloth,
 * corners, and lift, so it stands over the settings page as a part of the
 * same app.
 */
@Composable
internal fun DailyTimeDialog(
    minuteOfDay: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val twentyFour = remember(context) {
        android.text.format.DateFormat.is24HourFormat(context)
    }
    val start = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    var hour by rememberSaveable { mutableIntStateOf(start / 60) }
    val hours = remember(twentyFour) {
        if (twentyFour) (0..23).toList() else (1..12).toList()
    }
    val hourValue = if (twentyFour) hour else ((hour + 11) % 12) + 1
    val evening = hour >= 12
    val title = stringResource(R.string.settings_daily_picker_title)
    val hourName = stringResource(R.string.settings_daily_picker_hour)
    val around = stringResource(R.string.settings_daily_picker_around)
    val focusManager = LocalFocusManager.current

    fun dismiss() {
        // The dialog's window is about to leave; a focused child inside it
        // would keep the focus on a window that no longer exists, and the
        // settings page behind it never gets the next Back. Clear the
        // dialog's own focus before the window goes, the same way a popup
        // hands its focus back when it closes.
        focusManager.clearFocus(force = true)
        onDismiss()
    }

    fun select(value: Int) {
        hour = if (twentyFour) value else (value % 12) + if (evening) 12 else 0
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // The room the system bars leave, read once: the tape and the
            // foot below divide it.
            val room = maxHeight
            Column(
                modifier = Modifier
                    .padding(Space.Gutter)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .floatingLift(Radii.Popup)
                    .clip(Radii.Popup)
                    // A panel, like the sheets and the menus it stands among,
                    // so it wears their paper and not the tone of a control
                    // that floats over the reading (owner decision, 17.0: the
                    // day's hour is a page the reader sets, not a pill over
                    // the page).
                    .background(MaterialTheme.colorScheme.surface)
                    .semantics { paneTitle = title }
                    .testTag("daily-time-picker"),
            ) {
                Column(
                    modifier = Modifier
                        // The subject takes what the foot and the gutters
                        // leave, and scrolls inside it, so the foot is on the
                        // glass at every text size.
                        .heightIn(max = (room - 40.dp - FooterRoom).coerceAtLeast(1.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Gutter),
                ) {
                    Text(
                        text = title,
                        style = screenSubtitle(),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Space.Line))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = clockHourText(hour, twentyFour, locale),
                            style = timeStyle(),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (!twentyFour) {
                            Text(
                                text = clockPeriodText(evening, locale),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Space.Line),
                            )
                        }
                    }
                    if (!twentyFour) {
                        Spacer(Modifier.height(Space.Line))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PeriodChoice(
                                label = clockPeriodText(pm = false, locale = locale),
                                chosen = !evening,
                            ) {
                                hour = hour % 12
                            }
                            Spacer(Modifier.width(Space.Action))
                            PeriodChoice(
                                label = clockPeriodText(pm = true, locale = locale),
                                chosen = evening,
                            ) {
                                hour = (hour % 12) + 12
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.Block))
                    TimeTape(
                        label = hourName,
                        values = hours,
                        selected = hourValue,
                        format = { value -> clockHourText(value, twentyFour, locale) },
                        onSelect = ::select,
                    )
                    Spacer(Modifier.height(Space.Line))
                    Text(
                        text = around,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Space.Gutter, end = Space.Gutter, top = Space.Block, bottom = Space.Gutter),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        label = stringResource(R.string.settings_cancel),
                        onClick = ::dismiss,
                        quiet = true,
                    )
                    TextButton(
                        label = stringResource(R.string.settings_set),
                        onClick = {
                            focusManager.clearFocus(force = true)
                            onPick(hour * 60)
                        },
                        modifier = Modifier.padding(start = Space.Action),
                        prominent = true,
                    )
                }
            }
        }
    }
}

/** The page's own hour: the interface face at 40 on a 48 line, tabular. */
@Composable
private fun timeStyle() = MaterialTheme.typography.titleLarge.copy(
    fontSize = 40.sp,
    lineHeight = 48.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
)

/**
 * One tape: a row of numbers whose middle one is the chosen value, and a thin
 * accent tick under it.
 *
 * The tape is the app's scale control read as an hour: it is one TalkBack node
 * with the range's own progress information and a `setProgress` action, so a
 * screen reader moves one step at a time, while a sighted reader can drag the
 * tape or tap a number and the list carries it to the middle. [label] is the
 * tape's spoken name; the page's title and the AM/PM pair already say what the
 * numbers are, so the tape draws no visible label of its own.
 */
@Composable
private fun TimeTape(
    label: String,
    values: List<Int>,
    selected: Int,
    format: (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    if (values.isEmpty()) return
    val initial = values.indexOf(selected).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val scope = rememberCoroutineScope()
    val report = rememberUpdatedState(onSelect)
    val reducedMotion by rememberReducedMotion()
    val focusSource = remember { MutableInteractionSource() }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(TapeCell),
    ) {
        val pad = ((maxWidth - TapeCell) / 2).coerceAtLeast(0.dp)
        val centered by remember(state) {
            derivedStateOf {
                val info = state.layoutInfo
                val middle = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo
                    .minByOrNull { abs(it.offset + it.size / 2f - middle) }
                    ?.index
                    ?: initial
            }
        }
        // A value that came from outside the tape has to bring the tape with
        // it. The tape's drag already reports the value it centered, so that
        // case stays where it is; only a value that is not under the tick
        // scrolls. The padding that centers the first cell makes scrollToItem
        // land the item under the tick.
        LaunchedEffect(selected) {
            val index = values.indexOf(selected)
            if (index < 0 || index == centered) return@LaunchedEffect
            // Wait for the finger and the fling to finish before the one
            // programmatic scroll, so the effect never fights the drag it
            // came from (owner report, 14.4).
            if (state.isScrollInProgress) {
                snapshotFlow { state.isScrollInProgress }.first { !it }
            }
            if (index != centered) state.scrollToItem(index)
        }
        LaunchedEffect(Unit) {
            snapshotFlow { centered }.drop(1).collect { index ->
                values.getOrNull(index)?.let { report.value(it) }
            }
        }
        val describe = format(values.getOrElse(centered) { values.first() })
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(horizontal = pad),
            modifier = Modifier
                .fillMaxSize()
                .focusable(interactionSource = focusSource)
                .focusRing(focusSource, Radii.Pill)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val direction = when (event.key) {
                        Key.DirectionLeft -> -1
                        Key.DirectionRight -> 1
                        else -> return@onKeyEvent false
                    }
                    val next = (values.indexOf(selected) + direction)
                        .coerceIn(0, values.lastIndex)
                    if (next != values.indexOf(selected)) {
                        report.value(values[next])
                        scope.launch {
                            if (reducedMotion) {
                                state.scrollToItem(next)
                            } else {
                                state.animateScrollToItem(next)
                            }
                        }
                    }
                    true
                }
                .semantics {
                    contentDescription = label
                    stateDescription = describe
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = centered.toFloat(),
                        range = 0f..values.lastIndex.toFloat(),
                        steps = (values.size - 2).coerceAtLeast(0),
                    )
                    setProgress { target ->
                        val index = target.roundToInt().coerceIn(0, values.lastIndex)
                        report.value(values[index])
                        scope.launch {
                            if (reducedMotion) {
                                state.scrollToItem(index)
                            } else {
                                state.animateScrollToItem(index)
                            }
                        }
                        true
                    }
                },
        ) {
            itemsIndexed(values) { index, value ->
                val chosen = index == centered
                Box(
                    modifier = Modifier
                        .width(TapeCell)
                        .fillMaxSize()
                        .clickable {
                            scope.launch {
                                if (reducedMotion) {
                                    state.scrollToItem(index)
                                } else {
                                    state.animateScrollToItem(index)
                                }
                            }
                        }
                        // The tape is one adjustable node; its cells are
                        // the drawing's steps, never a row of controls.
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = format(value),
                        style = if (chosen) {
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = 20.sp,
                                lineHeight = 24.sp,
                            )
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (chosen) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .width(16.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

/** One half of the 12 hour day: the AM/PM pair's own choice. */
@Composable
private fun PeriodChoice(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    AppChip(
        label = label,
        selected = chosen,
        onClick = onClick,
        kind = ChipKind.Radio,
        style = ChipStyle.Place,
    )
}
