package io.github.muntasimulhaque.quran.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.muntasimulhaque.quran.ui.reader.ReaderScreen
import io.github.muntasimulhaque.quran.ui.theme.Amiri
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme

/**
 * The app: one theme, one screen, and a first paint that is never blank.
 * The content opens in the background while the paper is already on screen.
 */
@Composable
fun QuranApp(
    viewModel: ReaderViewModel = viewModel(),
    onPlaybackPermission: () -> Unit = {},
) {
    QuranTheme(viewModel.settings.theme) {
        val content = viewModel.content
        if (!viewModel.ready || content == null) {
            FirstPaint()
        } else {
            // The first page is on screen; the system can stop counting.
            val view = androidx.compose.ui.platform.LocalView.current
            LaunchedEffect(Unit) {
                (view.context as? android.app.Activity)?.reportFullyDrawn()
            }
            ReaderScreen(viewModel, content, onPlaybackPermission)
        }
    }
}

/**
 * What the reader sees in the first moments: the paper, and the name of the
 * Book in the script it was written in. No spinner, no logo animation.
 */
@Composable
private fun FirstPaint() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u0627\u0644\u0642\u0631\u0622\u0646",
                style = TextStyle(
                    fontFamily = Amiri,
                    fontSize = 52.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                ),
            )
            Text(
                text = "The Noble Book",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
