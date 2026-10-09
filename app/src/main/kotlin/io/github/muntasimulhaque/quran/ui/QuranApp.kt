package io.github.muntasimulhaque.quran.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.quran.R
import io.github.muntasimulhaque.quran.ui.mushaf.StartupPage
import io.github.muntasimulhaque.quran.ui.reader.ReaderScreen
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.LocalPagePalette
import io.github.muntasimulhaque.quran.ui.theme.LocalPageThemeName
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.data.isDark
import io.github.muntasimulhaque.quran.data.resolved
import io.github.muntasimulhaque.quran.ui.kit.TextButton
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme


/**
 * The app: one theme, one screen, and a first paint that is never blank. The
 * reader's page is painted from the picture the last session left on disk,
 * and the content opens behind it.
 *
 * The theme is resolved once, here, from the reader's choice and, when they
 * asked for it, the system's own day and night. Everything below reads the
 * resolved theme, including the launch picture, so a launch in the night
 * never paints the day's page for a frame.
 */
@Composable
fun QuranApp(
    viewModel: ReaderViewModel = viewModel(),
    /** The ayah a reminder's tap asked for, before the reading is on screen. */
    initialAyah: Int? = null,
    onPlaybackPermission: () -> Unit = {},
    /**
     * What the two acts that set the reminder ask for: the notification,
     * and then, on the phones that withhold it, the exact alarm in the
     * phone's own screen.
     */
    onReminderPermission: () -> Unit = {},
    /**
     * The reader has chosen a language, before the reminder card. The shell
     * brings the Activity back up in the language just chosen, so the card
     * and every window after it speak it.
     */
    afterFirstScreen: () -> Unit = {},
    /**
     * The reminder card's answer: whether the reader turned the daily ayah
     * on. The shell asks for the two grants it needs (the notification, and
     * the phone's own exact time) when it is on, and the reading opens in
     * either case.
     */
    onFirstScreenDone: (Boolean) -> Unit = {},
    /** Whether the phone will show this app's notifications, read from the phone. */
    notificationsBlocked: () -> Boolean = { false },
    /** Whether the phone is still withholding the exact alarm. */
    exactGrantWithheld: () -> Boolean = { false },
    /** Opens the phone's own page for this app's notifications. */
    onOpenNotificationSettings: () -> Unit = {},
    /** Opens the phone's own screen for the exact alarm. */
    onAskExactAlarm: () -> Unit = {},
) {
    // A tap on the reminder opens that ayah in the study reading, which is
    // what a reminder is for: the reader meets the words, not the app. The
    // jump runs once, after the library has opened, and the same ayah is
    // written down as the place, so closing the app leaves it there.
    LaunchedEffect(initialAyah, viewModel.ready) {
        if (initialAyah == null || !viewModel.ready) return@LaunchedEffect
        viewModel.jumpToAyahInStudy(initialAyah)
    }
    val settings = viewModel.settings
    val theme = settings.theme.resolved(
        autoNight = settings.autoNight,
        systemDark = isSystemInDarkTheme(),
    )
    QuranTheme(theme, language = UiLanguage.of(settings.uiLanguage) ?: UiLanguage.English) {
        // The status and navigation bars belong to the theme the reader
        // chose, not to the system's own idea of day and night: on a night
        // page the icons must be light, or they vanish into it.
        val view = LocalView.current
        val dark = theme.isDark()
        LaunchedEffect(dark) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }

        val content = viewModel.content
        // The language just chosen but the reminder card still on screen: the
        // value survives the recreation that brings the new locale up, so the
        // card speaks the chosen language. It is forgotten with the process,
        // which costs the reader nothing: the reading opens and the reminder
        // stays on, its state told on the Daily page.
        var welcomePending by rememberSaveable { mutableStateOf<String?>(null) }
        // These are read here, in the screen's own scope, and not only inside
        // a nested lambda: the first paint gives way to the reading the
        // moment the library opens, and that switch must never wait for a
        // later recomposition to notice.
        val ready = viewModel.ready
        val failed = viewModel.failure
        when {
            failed -> ContentProblem(onRetry = { viewModel.retryOpen() })
            !ready || content == null -> FirstPaint(viewModel.startupPage)
            settings.uiLanguage == null -> {
                // The first screen is on screen; the system can stop counting.
                LaunchedEffect(Unit) {
                    (view.context as? Activity)?.reportFullyDrawn()
                }
                LanguageWelcome(
                    suggested = systemLanguage(),
                    onChoose = { language ->
                        viewModel.chooseLanguage(language)
                        welcomePending = language.tag
                        // The locale belongs to the Activity's own resources,
                        // so the recreation brings every window up speaking
                        // the language the reminder card is about to ask in.
                        afterFirstScreen()
                    },
                )
            }
            welcomePending != null -> {
                WelcomeReminder(
                    onTurnOn = {
                        welcomePending = null
                        // The two grants are asked by the shell once the card
                        // is answered; the view model only writes the choice.
                        viewModel.setDailyAyah(enabled = true, onPermission = {})
                        onFirstScreenDone(true)
                    },
                    onNotNow = {
                        welcomePending = null
                        // Off rather than on under a permission that will
                        // never arrive; the Daily page asks again at the act
                        // of setting it.
                        viewModel.setDailyAyah(enabled = false, onPermission = {})
                        onFirstScreenDone(false)
                    },
                )
            }
            else -> {
                // The first page is on screen; the system can stop counting.
                LaunchedEffect(Unit) {
                    (view.context as? Activity)?.reportFullyDrawn()
                }
                ReaderScreen(
                    viewModel,
                    content,
                    onPlaybackPermission,
                    onReminderPermission,
                    notificationsBlocked,
                    exactGrantWithheld,
                    onOpenNotificationSettings,
                    onAskExactAlarm,
                )
            }
        }
    }
}

/**
 * The one thing that could keep the reader from the text: the content on the
 * device cannot be read. The screen says so in one sentence and offers the
 * one action that might fix it.
 */
@Composable
private fun ContentProblem(onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.content_problem_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.content_problem_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(
                label = stringResource(R.string.content_problem_retry),
                onClick = onRetry,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

/**
 * What the reader sees in the first moments: the page they left, drawn at the
 * width they left it, or, on the very first launch, the paper and the name of
 * the Book in the script it was written in. No spinner, no logo animation.
 */
@Composable
private fun FirstPaint(startup: StartupPage?) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(LocalPagePalette.current.paper),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.toInt()
        val heightPx = with(density) { maxHeight.toPx() }.toInt()
        val theme = LocalPageThemeName.current
        val bitmap = startup?.takeIf {
            it.glassWidthPx == widthPx && it.glassHeightPx == heightPx && it.theme == theme
        }?.bitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.first_paint_title),
                    style = TextStyle(
                        fontFamily = Amiri,
                        fontSize = 52.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                    ),
                )
                Text(
                    text = stringResource(R.string.first_paint_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

