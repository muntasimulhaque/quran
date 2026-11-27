package io.github.muntasimulhaque.quran.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.feature.settings.R
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.kit.sheetVerticalScroll
import io.github.muntasimulhaque.quran.ui.theme.Space

/**
 * The daily ayah's own page: the switch, the moment it arrives, and the
 * things that can stop it arriving. The switch is here as well as on the hub
 * row, because the page that owns the feature should be able to turn it on
 * (owner decision, 9.1).
 *
 * The hour used to be a strip of hour chips that unfolded under the switch in
 * the hub, and a reader reported two things about it: it filled the hub with
 * twenty-four shapes, and it did not look like every time of day was in it
 * (owner report, 2.3). Both were true of a list of hours: it is long, and it
 * only ever held the top of the hour. The hub now carries one row, and this
 * page carries the choice, opened from inside it the way the translation and
 * the tafsir open their lists.
 *
 * The moment itself is asked for on the app's own time page: the time as the
 * subject, an hour tape and a minute tape, and the phone's own 24 or 12 hour
 * reading with its AM/PM pair. The tapes reach every minute of the day, which
 * a row of hours could not, and the page is drawn in the app's hand rather
 * than borrowing the platform's clock (owner decision, 13.9).
 *
 * The phone's permission to notify is asked for from the two moments that
 * prove the reader wants the daily ayah, which the shell wires up: turning it
 * on, and moving its hour. Never when the page is merely opened, because a
 * reader who came to look is not the reader who came to set, and a system
 * dialog is not a thing to spend on curiosity (owner decision, 2.3).
 *
 * The hour is the unit, and the ayah arrives around it: the alarm is the
 * platform's own batched one, which needs no special access and can be late.
 * The page says "around" in the row and in the picker, and there is no exact
 * alarm, no permission, and no second state to explain (owner decision,
 * 23.8). The notification line below the row stays: it is the one dependency
 * outside the app that can stop the ayah entirely.
 */
@Composable
fun DailyPage(
    settings: AppSettings,
    notificationsBlocked: Boolean,
    /**
     * The phone has no screen for the notification settings at all, so the
     * line that says notifications are off has no door under it. The fact is
     * a property of the phone and not a moment, so it is state here beside
     * the switch that needs it, said once and kept
     * (owner decision, 19.3).
     */
    notificationScreenMissing: Boolean = false,
    onDailyAyah: (Boolean) -> Unit,
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
        // The switch is here as well as on the hub row, and so it is on
        // every page that owns a reading switch: a reader who opened the page
        // to set the time and found the reminder off should not have to go
        // back a page to turn it on. The page that owns the feature owns the
        // switch that turns it on, and the hub's own switch is the reader's
        // hand on the same value from the list (owner decision, 11.9). The
        // line under it says only that the reminder is off,
        // because the moment is the row below it. The row stays live while it
        // is off, so a reader who came to move the time is not asked to turn
        // the daily ayah on first, and a time that is set is a time that is
        // shown, whether or not it will arrive.
        ToggleRow(
            title = stringResource(R.string.settings_daily_title),
            subtitle = if (settings.dailyAyah) null else stringResource(R.string.settings_daily_off),
            checked = settings.dailyAyah,
            onChange = onDailyAyah,
            switchTag = "switch-daily-page",
        )
        PageRow(
            title = stringResource(R.string.settings_daily_time_title),
            summary = hourOfDay(settings.dailyAyahMinute),
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
                text = stringResource(
                    if (notificationScreenMissing) {
                        R.string.settings_daily_no_notification_screen
                    } else {
                        R.string.settings_daily_blocked
                    },
                ),
                // A sentence about the phone's own settings is prose, not a
                // caption: it reads at the meta line's neighbors, not a point
                // under them (owner decision, 13.3).
                style = MaterialTheme.typography.bodyMedium,
                // A state line, not a warning: it is the same ink the
                // page's quiet lines wear, and the one thing the page calls
                // a fact must not be the loud one (owner decision, 7.6).
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.Line),
            )
            // A phone with no such screen has no door to offer, so none is
            // drawn: the sentence above is the whole of what can be done, and
            // a word that opens nothing is worse than a sentence (owner
            // decision, 19.3).
            if (!notificationScreenMissing) {
                Box(Modifier.padding(start = Space.Gutter, top = Space.Line)) {
                    TextButton(
                        label = stringResource(R.string.settings_daily_blocked_action),
                        onClick = onOpenNotificationSettings,
                    )
                }
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
