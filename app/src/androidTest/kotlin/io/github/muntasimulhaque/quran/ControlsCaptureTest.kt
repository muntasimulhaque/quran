package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.ui.settings.SizeRow
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The size controls, photographed for review.
 *
 * The five cell row was four 250 dp pills down the page; the rail is one
 * line per kind of text, so the whole ladder has to be seen at the page's
 * width before it is believed. This class draws the four rows in the light
 * theme and writes one frame to the build's screenshot directory, the same
 * way the type specimen does: it is a review frame, not a store frame, and it
 * changes no setting.
 */
@RunWith(AndroidJUnit4::class)
class ControlsCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTheFourSizeRails() {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light) { SizeRails() }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "size-rails.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Composable
    private fun SizeRails() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp),
        ) {
            TypeRole.entries.forEach { role ->
                SizeRow(role = role, step = TextSize.DEFAULT, onChange = {})
            }
        }
    }
}
