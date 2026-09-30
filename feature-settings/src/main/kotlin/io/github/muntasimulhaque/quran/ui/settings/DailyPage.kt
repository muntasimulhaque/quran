package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.clockText
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.reader.Icon
import io.github.muntasimulhaque.quran.ui.reader.IconGlyph
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The daily reminder's own page: the moment it arrives, and the one thing
 * that can stop it arriving. The switch is not here: it lives on the hub row,
 * which is the one place a setting is set, and this page carries the moment
 * (owner report, D-105).
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
 * prove the reader wants the reminder, which the shell wires up: turning it
 * on, and moving its moment. Never when the page is merely opened, because a
 * reader who came to look is not the reader who came to set, and a system
 * dialog is not a thing to spend on curiosity (owner decision, 2.3).
 *
 * The phone's own exact alarm switch is never asked for, and nothing here
 * says that it is missing. It used to: a line under the time and a button
 * that opened the phone's page for it, on the reasoning that a reminder late
 * by a few minutes is a broken promise. It is not the reader's setting to
 * manage and it is not the app's to spend a screen on: the phone decides,
 * the app takes the best alarm it is given, and a reader who is not reading
 * about a second permission keeps a page with one row and one note (owner
 * report, D-130, which closes the page D-114 opened).
 *
 * The exact time is still used whenever the phone allows it, and it is still
 * the manifest's own permission: `DailyAyahScheduler` asks the alarm manager
 * on every arm and takes the exact path when the answer is yes, which is the
 * answer on every release before Android 12 and on any phone the reader has
 * granted it in. The reminder arrives inside the minute otherwise, which on
 * a locked phone is the platform's own timing and not a choice this app
 * makes.
 */
@Composable
fun DailyPage(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    onDailyAyahTime: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .sheetVerticalScroll(rememberScrollState())
            .testTag("daily-page"),
    ) {
        // The switch lives on the hub row and nowhere else; this page is the
        // moment (owner report, D-105). The row stays live when the reminder
        // is off: a reader who came to move the time is not asked to turn the
        // reminder on first, and a time that is set is a time that is shown,
        // whether or not it will arrive.
        if (!settings.dailyAyah) {
            Text(
                text = stringResource(R.string.settings_daily_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Line),
            )
        }
        PageRow(
            title = stringResource(R.string.settings_daily_time_title),
            summary = clockText(settings.dailyAyahMinute),
            onClick = { choosing = true },
        )
        Text(
            text = stringResource(R.string.settings_daily_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = Space.Section),
        )
        // The reminder has one dependency outside the app, and a page that
        // hides it is a page that lets the reader wonder. The phone, not the
        // app, is what has turned notifications off, so the way out is named
        // as the phone's own settings rather than as a switch that does not
        // work. This is the one door the app names, and it is named once: the
        // reader is either getting their reminder or they are not, and there
        // is one place in the phone's settings where that is decided.
        if (notificationsBlocked) {
            Text(
                text = stringResource(R.string.settings_daily_blocked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = Space.Line),
            )
            Box(Modifier.padding(start = 16.dp, top = Space.Line)) {
                TextButton(
                    label = stringResource(R.string.settings_daily_blocked_action),
                    onClick = onOpenNotificationSettings,
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        },
        modeToggleButton = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { typing = !typing }
                    .semantics { contentDescription = toggleLabel },
                contentAlignment = Alignment.Center,
            ) {
                IconGlyph(
                    icon = if (typing) Icon.Clock else Icon.Keyboard,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        dismissButton = {
            TextButton(
                label = stringResource(R.string.settings_cancel),
                onClick = onDismiss,
                quiet = true,
                modifier = Modifier.padding(end = 8.dp),
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
