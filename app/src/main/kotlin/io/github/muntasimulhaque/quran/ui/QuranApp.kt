package io.github.muntasimulhaque.quran.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import io.github.muntasimulhaque.quran.ui.theme.isDark

/**
 * The app: one theme, one screen, and a first paint that is never blank. The
 * reader's page is painted from the picture the last session left on disk,
 * and the content opens behind it.
 */
@Composable
fun QuranApp(
    viewModel: ReaderViewModel = viewModel(),
    onPlaybackPermission: () -> Unit = {},
) {
    QuranTheme(viewModel.settings.theme) {
        // The status and navigation bars belong to the theme the reader
        // chose, not to the system's own idea of day and night: on a night
        // page the icons must be light, or they vanish into it.
        val view = LocalView.current
        val dark = viewModel.settings.theme.isDark()
        LaunchedEffect(dark) {
            val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }

        val content = viewModel.content
        when {
            viewModel.failure -> ContentProblem(onRetry = { viewModel.retryOpen() })
            !viewModel.ready || content == null -> FirstPaint(viewModel.startupPage)
            else -> {
                // The first page is on screen; the system can stop counting.
                LaunchedEffect(Unit) {
                    (view.context as? Activity)?.reportFullyDrawn()
                }
                ReaderScreen(viewModel, content, onPlaybackPermission)
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
            Text(
                text = stringResource(R.string.content_problem_retry),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
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
        val theme = LocalPageThemeName.current
        val bitmap = startup?.takeIf { it.widthPx == widthPx && it.theme == theme }?.bitmap
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
