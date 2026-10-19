package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconButton
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The daily ayah's own page: the switch, the moment it arrives, and the
 * things that can stop it arriving. The switch is here as well as on the hub
 * row, because the page that owns the feature should be able to turn it on
 * (owner decision, this session).
 *
 * The hour used to be a strip of hour chips that unfolded under the switch in
 * the hub, and a reader reported two things about it: it filled the hub with
 * twenty-four shapes, and it did not look like every time of day was in it
 * (owner report, 2.3). Both were true of a list of hours: it is long, and it
 * only ever held the top of the hour. The hub now carries one row, and this
 * page carries the choice, opened from inside it the way the translation and
 * the tafsir open their lists.
 *
 * The moment itself is asked for with the one control for it that a reader
 * has already learned somewhere else: the phone's own clock, with the time
 * available to be typed as well. That reaches every minute of the day, which
 * a row of hours could not, and it says AM or PM in the reader's own language
 * rather than leaving a bare number to be interpreted.
 *
 * The phone's permission to notify is asked for from the two moments that
 * prove the reader wants the daily ayah, which the shell wires up: turning it
 * on, and moving its moment. Never when the page is merely opened, because a
 * reader who came to look is not the reader who came to set, and a system
 * dialog is not a thing to spend on curiosity (owner decision, 2.3). The
 * exact alarm is asked for at those same two moments, and only on the phones
 * that withhold it.
 *
 * The phone's own exact alarm used to be asked for nowhere, and nothing here
 * said that it was missing, on the reasoning that a daily ayah late by a few
 * minutes is a broken promise. Play has settled that: USE_EXACT_ALARM is for
 * an alarm clock or a calendar, and the 3.9 build was rejected for it. So the
 * ask is back, it is made once at each of the two acts in the phone's own
 * screen, and the page again carries the one line that says where it stands,
 * the same shape as the notification line, with the one door the phone
 * offers (owner decision).
 *
 * The exact time is the manifest's own promise: `SCHEDULE_EXACT_ALARM` is
 * granted at install before Android 14, and from Android 14 it starts out
 * denied, so the daily ayah arrives at the minute even on a phone that is
 * locked and idle then exactly when that grant has been given. Nothing about
 * it is said here except where it stands: the phone's own batched alarm is
 * left for the reader who declines, the page says so in one line, and it
 * names the one screen where the answer is taken (owner decision).
 */
@Composable
fun DailyPage(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    exactGrantWithheld: Boolean,
    onDailyAyah: (Boolean) -> Unit,
    onDailyAyahTime: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onAskExactAlarm: () -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .sheetVerticalScroll(rememberScrollState())
            .testTag("daily-page"),
    ) {
        // The switch is here as well as on the hub row, because the page that
        // owns the feature should be able to turn it on: a reader who opened
        // it to set the time and found it off should not have to go back a
        // page to turn it on (owner decision, this session). The row stays
        // live while it is off, so a reader who came to move the time is not
        // asked to turn the daily ayah on first, and a time that is set is a
        // time that is shown, whether or not it will arrive.
        ToggleRow(
            title = stringResource(R.string.settings_daily_title),
            subtitle = if (settings.dailyAyah) null else stringResource(R.string.settings_daily_off),
            checked = settings.dailyAyah,
            onChange = onDailyAyah,
            switchTag = "switch-daily-page",
        )
        PageRow(
            title = stringResource(R.string.settings_daily_time_title),
            summary = clockText(settings.dailyAyahMinute),
            onClick = { choosing = true },
        )
        // The daily ayah has one dependency outside the app, and a page that
        // hides it is a page that lets the reader wonder. The phone, not the
        // app, is what has turned notifications off, so the way out is named
        // as the phone's own settings rather than as a switch that does not
        // work. This is the one door the app names, and it is named once: the
        // reader is either getting their daily ayah or they are not, and there
        // is one place in the phone's settings where that is decided.
        if (notificationsBlocked) {
            Text(
                text = stringResource(R.string.settings_daily_blocked),
                style = MaterialTheme.typography.bodySmall,
                // The same ink as the exact-time line below: both are state
                // lines of the same shape, and the one the page calls the
                // same shape of truth must not be the loud one (owner
                // decision, the theme and consistency session).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.Line),
            )
            Box(Modifier.padding(start = Space.Gutter, top = Space.Line)) {
                TextButton(
                    label = stringResource(R.string.settings_daily_blocked_action),
                    onClick = onOpenNotificationSettings,
                )
            }
        }
        // The exact time is the other thing the phone can withhold, and it is
        // the same shape of truth: the ask was made at the act of setting,
        // and what is left here is state, so a reader who said no, or who
        // answered on a phone that could not ask, knows the ayah can be late
        // and has the one door the phone offers.
        if (settings.dailyAyah && exactGrantWithheld) {
            Text(
                text = stringResource(R.string.settings_daily_exact_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.Line),
            )
            Box(Modifier.padding(start = Space.Gutter, top = Space.Line)) {
                TextButton(
                    label = stringResource(R.string.settings_daily_exact_action),
                    onClick = onAskExactAlarm,
                )
            }
        }
        Spacer(Modifier.height(Space.Section))
    }
    if (choosing) {
        DailyTimeDialog(
            minuteOfDay = settings.dailyAyahMinute,
            onDismiss = { choosing = false },
            onPick = { minute ->
                choosing = false
                onDailyAyahTime(minute)
            },
        )
    }
}

/**
 * The clock the moment is chosen on: the platform's Material clock, wearing
 * this app's colors, with the time available to be typed as well.
 *
 * Both halves are here on purpose. Turning a clock is the fastest way to "the
 * usual time", and typing is the fastest way to an exact one, and a reader who
 * wants the one should never be handed the other. The typed half is also the
 * half a blind reader and a reader with a shaky finger will use, since it is
 * two fields and no aim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTimeDialog(
    minuteOfDay: Int,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = minuteOfDay / 60,
        initialMinute = minuteOfDay % 60,
    )
    var typing by rememberSaveable { mutableStateOf(false) }
    val toggleLabel = stringResource(
        if (typing) R.string.settings_daily_picker_clock else R.string.settings_daily_picker_keyboard,
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_daily_picker_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        },
        modeToggleButton = {
            IconButton(
                icon = if (typing) Icon.Clock else Icon.Keyboard,
                description = toggleLabel,
                onClick = { typing = !typing },
            )
        },
        dismissButton = {
            TextButton(
                label = stringResource(R.string.settings_cancel),
                onClick = onDismiss,
                // The platform's clock dialog lays its two buttons edge to
                // edge with no spacing of its own, so the action gap is drawn
                // here; without it Cancel and Set abut (owner decision, this
                // session).
                modifier = Modifier.padding(end = Space.Row),
                quiet = true,
            )
        },
        confirmButton = {
            TextButton(
                label = stringResource(R.string.settings_set),
                onClick = { onPick(state.hour * 60 + state.minute) },
            )
        },
    ) {
        if (typing) {
            TimeInput(state)
        } else {
            TimePicker(state)
        }
    }
}
