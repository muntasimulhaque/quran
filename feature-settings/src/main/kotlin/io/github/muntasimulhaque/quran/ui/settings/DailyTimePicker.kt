package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.Wash
import io.github.muntasimulhaque.quran.ui.theme.screenSubtitle
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** One number's cell on a tape: the finger's 48 dp at every step. */
private val TapeCell = 48.dp

/** The room the hour's and the minute's names take, so their tapes start on one line. */
private val TapeLabel = 64.dp

/**
 * The moment the daily ayah arrives, chosen on the app's own time page.
 *
 * The page is drawn rather than borrowed: the Material clock was the one
 * stock surface left in an app that draws every other control, and it opened
 * on a 72 sp header block that stood over its own question. The new page keeps
 * the app's grammar instead. The question is a small page's name at 16; the
 * moment is the subject, set in the interface's tabular digits; and each part
 * of it is a tape the reader scrolls, with the chosen number in the accent
 * under a thin tick and its neighbours quiet on either side (owner decision,
 * this session).
 *
 * The tapes keep every minute of the day, which is what the clock bought and
 * what a row of hour chips could not. The hour tape speaks the phone's own
 * clock: 0 to 23 where the phone is 24 hour, 1 to 12 with an AM/PM pair where
 * it is not, so the page and the row that opened it say the same time in the
 * same language. Each tape is one TalkBack node with a range and a
 * `setProgress` action, so a reader can choose without aiming at a number;
 * a sighted reader can drag the tape or tap the number they want and the tape
 * carries it to the middle.
 *
 * Set is the page's one answer and Cancel its quiet exit, in the app's own
 * two button shapes. The dialog is the app's floating cloth, corners, and
 * lift, so it stands over the settings page as a part of the same app.
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
    var minute by rememberSaveable { mutableIntStateOf(start % 60) }
    val hours = remember(twentyFour) {
        if (twentyFour) (0..23).toList() else (1..12).toList()
    }
    val minutes = remember { (0..59).toList() }
    val hourValue = if (twentyFour) hour else ((hour + 11) % 12) + 1
    val evening = hour >= 12
    val title = stringResource(R.string.settings_daily_picker_title)
    val hourName = stringResource(R.string.settings_daily_picker_hour)
    val minuteName = stringResource(R.string.settings_daily_picker_minute)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .floatingLift(Radii.Popup)
                    .clip(Radii.Popup)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .semantics { paneTitle = title }
                    .testTag("daily-time-picker"),
            ) {
                Text(
                    text = title,
                    style = screenSubtitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Space.Line))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = hourText(hour, twentyFour, locale),
                        style = timeStyle(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = ":",
                        style = timeStyle(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp),
                    )
                    Text(
                        text = minuteText(minute, locale),
                        style = timeStyle(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!twentyFour) {
                        Text(
                            text = periodName(evening, locale),
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
                            label = periodName(pm = false, locale = locale),
                            chosen = !evening,
                        ) { hour = hour % 12 }
                        Spacer(Modifier.width(Space.Action))
                        PeriodChoice(
                            label = periodName(pm = true, locale = locale),
                            chosen = evening,
                        ) { hour = (hour % 12) + 12 }
                    }
                }
                Spacer(Modifier.height(Space.Block))
                TimeTape(
                    label = hourName,
                    values = hours,
                    selected = hourValue,
                    format = { value -> hourText(value, twentyFour, locale) },
                    onSelect = { value ->
                        hour = if (twentyFour) value else (value % 12) + if (evening) 12 else 0
                    },
                )
                TimeTape(
                    label = minuteName,
                    values = minutes,
                    selected = minute,
                    format = { value -> minuteText(value, locale) },
                    onSelect = { minute = it },
                )
                Spacer(Modifier.height(Space.Block))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        label = stringResource(R.string.settings_cancel),
                        onClick = onDismiss,
                        quiet = true,
                    )
                    TextButton(
                        label = stringResource(R.string.settings_set),
                        onClick = { onPick(hour * 60 + minute) },
                        modifier = Modifier.padding(start = Space.Action),
                        prominent = true,
                    )
                }
            }
        }
    }
}

/** The page's own time: the interface face at 40 on a 48 line, tabular. */
@Composable
private fun timeStyle() = MaterialTheme.typography.titleLarge.copy(
    fontSize = 40.sp,
    lineHeight = 48.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
)

/**
 * One tape: a name, a row of numbers whose middle one is the chosen value,
 * and a thin accent tick under it.
 *
 * The tape is the app's scale control read as a time: it is one TalkBack node
 * with the range's own progress information and a `setProgress` action, so a
 * screen reader moves one step at a time, while a sighted reader can drag the
 * tape or tap a number and the list carries it to the middle. The numbers are
 * drawn in the interface's own digits, so the page says the time in the
 * reader's language.
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TapeCell),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(TapeLabel),
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
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
                                state.animateScrollToItem(index)
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
                                    state.animateScrollToItem(index)
                                }
                            }
                            // The tape is one adjustable node; its cells are
                            // the drawing's steps, never sixty controls.
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
}

/** One half of the 12 hour day: the AM/PM pair's own choice. */
@Composable
private fun PeriodChoice(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .minimumInteractiveComponentSize()
            .clip(Radii.Pill)
            .background(
                if (chosen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = Wash.Door)
                },
            )
            .padding(horizontal = Space.Block, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (chosen) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** The hour as the phone's clock reads it: two digits on a 24 hour clock. */
private fun hourText(hour: Int, twentyFour: Boolean, locale: Locale): String =
    if (twentyFour) {
        String.format(locale, "%02d", hour)
    } else {
        String.format(locale, "%d", ((hour + 11) % 12) + 1)
    }

/** The minute, always two digits, in the reader's language. */
private fun minuteText(minute: Int, locale: Locale): String =
    String.format(locale, "%02d", minute)

/** The locale's own AM or PM marker, the same one `clockText` shows. */
private fun periodName(pm: Boolean, locale: Locale): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, if (pm) 13 else 1)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    return SimpleDateFormat("a", locale).format(calendar.time)
}
