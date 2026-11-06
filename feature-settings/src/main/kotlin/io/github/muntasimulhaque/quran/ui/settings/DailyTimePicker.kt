package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.glyph.Icon
import io.github.muntasimulhaque.quran.ui.kit.AppChip
import io.github.muntasimulhaque.quran.ui.kit.ChipKind
import io.github.muntasimulhaque.quran.ui.kit.ChipStyle
import io.github.muntasimulhaque.quran.ui.kit.RoundedSelectionColors
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.floatingLift
import io.github.muntasimulhaque.quran.ui.kit.focusRing
import io.github.muntasimulhaque.quran.ui.kit.rememberReducedMotion
import io.github.muntasimulhaque.quran.ui.kit.roundedSelection
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.theme.Radii
import io.github.muntasimulhaque.quran.ui.theme.Space
import io.github.muntasimulhaque.quran.ui.theme.screenSubtitle
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
 * The air a typed digit keeps inside its own slot: enough for the caret at
 * the end of the hour, which is where a select-all puts it.
 */
private val TimeSlotAir = 4.dp

/**
 * The room the pinned foot takes under the fields: its target, the gap over
 * it, and its own padding. The fields are capped by what is left, so the
 * keyboard mark, Cancel and Set are on the glass at every text size and
 * every keyboard height (owner decision, this version).
 */
private val FooterRoom = 48.dp + Space.Block + 20.dp

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
 * **The title stands on the dialog's one axis** (owner report, this session).
 * The time is centered and the page is a floating sheet of its own, so a
 * left-aligned question over a centered subject read as a layout accident.
 * It is still the small page's 16, not the 20 of a full sheet: the time is the
 * subject, and the question steps back.
 *
 * **The moment can be typed** (owner report, an earlier session). The big
 * digits are a field pair: tapping or long pressing the hour or the minute
 * selects that value and raises the number keyboard, and the existing
 * keyboard mark at the leading end of the button row is the visible door to
 * the same editor. The field selects all of its value, takes two digits,
 * commits on the IME's Next or Done or on losing focus, and brings the tape
 * to the value it committed. A value typed in 12-hour mode stays in the
 * period already chosen; an out-of-range entry is clamped rather than
 * rejected, so the reader always leaves with a time.
 *
 * **The tape and the field are one value** (owner report, this session).
 * A tape that moves while its part is being typed writes the draft, so the
 * big digits follow the tape and the next commit cannot put the old value
 * back. The tape's one programmatic scroll waits for the drag or fling to
 * finish, so a value that came from the finger is never pulled back under
 * it.
 *
 * **The typed digits keep the row, and the buttons keep the glass** (owner
 * report, this version). A text field takes every dp it is offered, so the
 * editing half took the whole row, put its digits on the dialog's edge and
 * pushed the colon and the minute out of it; each half now has the slot of
 * two tabular digits, and the row is the same shape at rest and while
 * typing. And the IME inset is kept outside the scroll: inside it, the
 * keyboard added its own height to the scrolling content and carried the
 * keyboard mark, Cancel and Set below the fold. The sheet stands above the
 * keyboard now, the fields scroll if the glass is short, and the one answer
 * stays on the glass.
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
    val typeName = stringResource(R.string.settings_daily_picker_type)

    // The typed value is a draft until it is committed, so an empty field can
    // be cleared and retyped without the displayed time jumping to 0. The
    // tape and the field share the committed hour and minute, and the field
    // selects the whole value when it opens: a reader who taps 08 and types 1
    // gets 01, not 081.
    var editing by rememberSaveable { mutableStateOf(TimePart.None) }
    // The keyboard is hidden only after an editor was actually open. Hiding it
    // on the dialog's first frame sent an IME transaction with no field on
    // screen, and the next Back landed while the window was still settling
    // (found by the accessibility run, this session).
    var hadEditing by remember { mutableStateOf(false) }
    var hourField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var minuteField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val hourFocus = remember { FocusRequester() }
    val minuteFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val time = timeStyle()
    val hourStyle = time.copy(textAlign = TextAlign.End)
    val minuteStyle = time.copy(textAlign = TextAlign.Start)
    // Two tabular digits, measured in the face the time is set in, so the
    // slot follows the system font scale with the digits and the row never
    // reflows when an editor opens (owner report, this version).
    val measurer = rememberTextMeasurer()
    val slot = with(LocalDensity.current) {
        measurer.measure(AnnotatedString("00"), style = time).size.width.toDp()
    } + TimeSlotAir

    fun commitHour() {
        if (editing != TimePart.Hour) return
        val entered = hourField.text.toIntOrNull()
        if (entered != null) {
            hour = if (twentyFour) {
                entered.coerceIn(0, 23)
            } else {
                val twelve = entered.coerceIn(1, 12)
                (twelve % 12) + if (evening) 12 else 0
            }
        }
        editing = TimePart.None
    }

    fun commitMinute() {
        if (editing != TimePart.Minute) return
        minuteField.text.toIntOrNull()?.let { minute = it.coerceIn(0, 59) }
        editing = TimePart.None
    }

    fun begin(part: TimePart) {
        commitHour()
        commitMinute()
        editing = part
        when (part) {
            TimePart.Hour -> {
                val text = hourText(hour, twentyFour, locale)
                hourField = TextFieldValue(text, selection = TextRange(0, text.length))
            }
            TimePart.Minute -> {
                val text = minuteText(minute, locale)
                minuteField = TextFieldValue(text, selection = TextRange(0, text.length))
            }
            TimePart.None -> Unit
        }
    }

    /**
     * The tape is a door to the same value the field edits, so a tape that
     * moves while its part is being typed has to write the draft too. Without
     * this the big digits kept the old draft and the next commit wrote it
     * back over the tape's choice (owner report, this session).
     */
    fun selectHour(value: Int) {
        hour = if (twentyFour) value else (value % 12) + if (evening) 12 else 0
        if (editing == TimePart.Hour) {
            val text = hourText(hour, twentyFour, locale)
            hourField = TextFieldValue(text, selection = TextRange(0, text.length))
        }
    }

    /** The minute's half of [selectHour]. */
    fun selectMinute(value: Int) {
        minute = value
        if (editing == TimePart.Minute) {
            val text = minuteText(minute, locale)
            minuteField = TextFieldValue(text, selection = TextRange(0, text.length))
        }
    }

    fun dismiss() {
        // The dialog's window is about to leave; a focused child inside it
        // would keep the focus on a window that no longer exists, and the
        // settings page behind it never gets the next Back. Clear the
        // dialog's own focus before the window goes, the same way a popup
        // hands its focus back when it closes.
        focusManager.clearFocus(force = true)
        if (editing != TimePart.None) keyboard?.hide()
        onDismiss()
    }

    LaunchedEffect(editing) {
        when (editing) {
            TimePart.Hour -> {
                hadEditing = true
                hourFocus.requestFocus()
                keyboard?.show()
            }
            TimePart.Minute -> {
                hadEditing = true
                minuteFocus.requestFocus()
                keyboard?.show()
            }
            TimePart.None -> if (hadEditing) {
                hadEditing = false
                keyboard?.hide()
            }
        }
    }

    Dialog(
        onDismissRequest = ::dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            // The room the keyboard and the system bars leave, read once: the
            // fields and the foot below divide it.
            val room = maxHeight
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .floatingLift(Radii.Popup)
                    .clip(Radii.Popup)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .semantics { paneTitle = title }
                    .testTag("daily-time-picker"),
            ) {
                Column(
                    modifier = Modifier
                        // The fields take what the foot and the gutters leave,
                        // and scroll inside it, so the foot is on the glass at
                        // every keyboard height.
                        .heightIn(max = (room - 40.dp - FooterRoom).coerceAtLeast(1.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp),
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
                        TimeField(
                            value = hourText(hour, twentyFour, locale),
                            editing = editing == TimePart.Hour,
                            field = hourField,
                            onFieldChange = { value ->
                                val digits = value.text.filter { it.isDigit() }.take(2)
                                hourField = value.copy(
                                    text = digits,
                                    selection = TextRange(digits.length),
                                )
                            },
                            style = hourStyle,
                            slot = slot,
                            name = hourName,
                            focus = hourFocus,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    commitHour()
                                    begin(TimePart.Minute)
                                },
                                onDone = {
                                    commitHour()
                                    focusManager.clearFocus()
                                },
                            ),
                            onFocusLost = ::commitHour,
                            onClick = { begin(TimePart.Hour) },
                        )
                        Text(
                            text = ":",
                            style = time,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                        TimeField(
                            value = minuteText(minute, locale),
                            editing = editing == TimePart.Minute,
                            field = minuteField,
                            onFieldChange = { value ->
                                val digits = value.text.filter { it.isDigit() }.take(2)
                                minuteField = value.copy(
                                    text = digits,
                                    selection = TextRange(digits.length),
                                )
                            },
                            style = minuteStyle,
                            slot = slot,
                            name = minuteName,
                            focus = minuteFocus,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    commitMinute()
                                    focusManager.clearFocus()
                                },
                            ),
                            onFocusLost = ::commitMinute,
                            onClick = { begin(TimePart.Minute) },
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
                            ) {
                                commitHour()
                                hour = hour % 12
                            }
                            Spacer(Modifier.width(Space.Action))
                            PeriodChoice(
                                label = periodName(pm = true, locale = locale),
                                chosen = evening,
                            ) {
                                commitHour()
                                hour = (hour % 12) + 12
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.Block))
                    TimeTape(
                        label = hourName,
                        values = hours,
                        selected = hourValue,
                        format = { value -> hourText(value, twentyFour, locale) },
                        onSelect = ::selectHour,
                    )
                    TimeTape(
                        label = minuteName,
                        values = minutes,
                        selected = minute,
                        format = { value -> minuteText(value, locale) },
                        onSelect = ::selectMinute,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = Space.Block, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The keyboard is the visible door to the typed time: the
                    // large digits also answer a tap or a long press, but a
                    // gesture that has to be taught is not a signifier, and
                    // this mark already says what the second way in is.
                    IconButton(
                        icon = Icon.Keyboard,
                        description = typeName,
                        onClick = { begin(TimePart.Hour) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            label = stringResource(R.string.settings_cancel),
                            onClick = ::dismiss,
                            quiet = true,
                        )
                        TextButton(
                            label = stringResource(R.string.settings_set),
                            onClick = {
                                commitHour()
                                commitMinute()
                                focusManager.clearFocus(force = true)
                                onPick(hour * 60 + minute)
                            },
                            modifier = Modifier.padding(start = Space.Action),
                            prominent = true,
                        )
                    }
                }
            }
        }
    }
}

/** The part of the time a field is editing, or no field at all. */
private enum class TimePart { None, Hour, Minute }

/**
 * One half of the big time: the committed value at rest, or the typed draft
 * while it is being edited.
 *
 * The reader can tap or long press the digits; the keyboard mark under the
 * tapes opens the same editor, and the field selects all of its value when it
 * opens. The number keyboard, two digits, and the IME's Next save the reader
 * from a decimal point or a third digit. A lost focus commits, so tapping the
 * other half, the tape, or the Set button never loses what was typed.
 *
 * The slot is the row's own measure, not the field's: the field used to take
 * the whole row because a text field takes every dp it is offered, so the
 * colon and the minute were pushed out of the dialog and the hour would land
 * on the dialog's edge (owner report, this version). The resting value and
 * the draft are the same two tabular digits in the same slot, so the row does
 * not move when the reader starts typing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimeField(
    value: String,
    editing: Boolean,
    field: TextFieldValue,
    onFieldChange: (TextFieldValue) -> Unit,
    style: TextStyle,
    /** The width of two tabular digits; the field takes every dp it is offered. */
    slot: Dp,
    name: String,
    focus: FocusRequester,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onFocusLost: () -> Unit,
    onClick: () -> Unit,
) {
    val press = remember { MutableInteractionSource() }
    // The first focus event of a newly composed field is "not focused"; the
    // editor would close before the request landed. The flag ignores that
    // first empty event and commits only a focus that was actually held.
    var focused by remember(editing) { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val ink = MaterialTheme.colorScheme.onSurface
    if (editing) {
        RoundedSelectionColors {
            BasicTextField(
                value = field,
                onValueChange = onFieldChange,
                textStyle = style.copy(color = ink),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .width(slot)
                    .focusRequester(focus)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            focused = true
                        } else if (focused) {
                            onFocusLost()
                        }
                    }
                    .roundedSelection(layout, field.selection)
                    .semantics { contentDescription = name },
            )
        }
    } else {
        Text(
            text = value,
            style = style,
            color = ink,
            modifier = Modifier
                .width(slot)
                .combinedClickable(
                    interactionSource = press,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onClick,
                )
                .focusRing(press, Radii.Pill)
                .semantics {
                    contentDescription = name
                    stateDescription = value
                },
        )
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
    val reducedMotion by rememberReducedMotion()
    val focusSource = remember { MutableInteractionSource() }
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
            // A value that came from the typed field, not from this tape's
            // own drag, has to bring the tape with it. The tape's drag already
            // reports the value it centered, so that case stays where it is;
            // only a value that is not under the tick scrolls. The padding
            // that centers the first cell makes scrollToItem land the item
            // under the tick.
            LaunchedEffect(selected) {
                val index = values.indexOf(selected)
                if (index < 0 || index == centered) return@LaunchedEffect
                // A typed value has to bring the tape with it, but the tape's
                // own drag already reports the value it centers. Wait for the
                // finger and the fling to finish before the one programmatic
                // scroll, so the effect never fights the drag it came from
                // (owner report, this session).
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
    AppChip(
        label = label,
        selected = chosen,
        onClick = onClick,
        kind = ChipKind.Radio,
        style = ChipStyle.Place,
    )
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
