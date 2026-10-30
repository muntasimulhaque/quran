package io.github.muntasimulhaque.quran

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import io.github.muntasimulhaque.quran.data.UiLanguage
import io.github.muntasimulhaque.quran.ui.theme.LocalReadingVoice
import io.github.muntasimulhaque.quran.ui.theme.QuranTheme
import io.github.muntasimulhaque.quran.ui.theme.TypeLadder
import io.github.muntasimulhaque.quran.ui.theme.prose
import io.github.muntasimulhaque.quran.ui.theme.title
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The type ladder, photographed for review.
 *
 * The color system has had a measured test since the theme audit, and a
 * layout is reviewed from the tour's frames, but the type had neither a guard
 * nor a picture: the ayah card shipped with its own name smaller than a
 * heading inside its text, and nothing failed. This class draws the ladder
 * the theme is built from, in both scripts, at the three steps a reader is
 * most likely to choose, and writes one frame per script to the build's
 * screenshot directory, so a type change is seen the way a layout change is
 * (owner decision, this session).
 *
 * It is not a store frame: the tour's eight are the listing's own, and this
 * class is not in the capture workflow's list. Run it on a device with the
 * rest of the suite, then read `type-specimen-en.png` and
 * `type-specimen-bn.png` from the additional test output. It changes no
 * setting, so there is nothing to hand back.
 */
@RunWith(AndroidJUnit4::class)
class TypeSpecimenCaptureTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun captureTheEnglishLadder() = capture(
        language = UiLanguage.English,
        name = "type-specimen-en",
        fixed = true,
        steps = listOf(1f),
    )

    @Test
    fun captureTheEnglishStaircase() = capture(
        language = UiLanguage.English,
        name = "type-specimen-staircase-en",
        fixed = false,
        steps = listOf(0.8f, 1.2f),
    )

    @Test
    fun captureTheBanglaLadder() = capture(
        language = UiLanguage.Bangla,
        name = "type-specimen-bn",
        fixed = true,
        steps = listOf(1f),
    )

    @Test
    fun captureTheBanglaStaircase() = capture(
        language = UiLanguage.Bangla,
        name = "type-specimen-staircase-bn",
        fixed = false,
        steps = listOf(0.8f, 1.2f),
    )

    private fun capture(
        language: UiLanguage,
        name: String,
        fixed: Boolean,
        steps: List<Float>,
    ) {
        compose.setContent {
            QuranTheme(theme = AppTheme.Light, language = language) {
                Specimen(language, fixed, steps)
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "$name.png").outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @Composable
    private fun Specimen(language: UiLanguage, fixed: Boolean, steps: List<Float>) {
        val voice = LocalReadingVoice.current
        val bangla = language == UiLanguage.Bangla
        val ink = MaterialTheme.colorScheme.onSurface
        val quiet = MaterialTheme.colorScheme.onSurfaceVariant
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
        ) {
            if (fixed) {
                // The fixed ladder, one line per job, so a reader of the frame
                // can hold it against the document below.
                Text(
                    text = if (bangla) "শিরোনাম" else "A sheet's name",
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                )
                Text(
                    text = if (bangla) "নামের নিচের এক লাইন" else "A source line under the name",
                    style = MaterialTheme.typography.bodySmall,
                    color = quiet,
                )
                Text(
                    text = if (bangla) "একটি দলের নাম" else "A SECTION LABEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = quiet,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = if (bangla) "সারির নাম" else "A row's name",
                    style = MaterialTheme.typography.titleSmall,
                    color = ink,
                )
                Text(
                    text = if (bangla) "সারির মান" else "A row's value",
                    style = MaterialTheme.typography.labelSmall,
                    color = quiet,
                )
                Text(
                    text = if (bangla) {
                        "ইন্টারফেসের এক বাক্য, এক লাইনে।"
                    } else {
                        "A sentence in the interface's voice."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = quiet,
                )
                Text(
                    text = if (bangla) "একটি বোতামের শব্দ" else "A button's word",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // The document's own staircase, at the steps this frame is about:
            // title, head, sub-head, and body are the numbers the card derives
            // from the reader's size. The Bangla body takes the script's own
            // step, the same one the settings hand every prose surface.
            val prose = if (bangla) {
                TextSize.BENGALI_READING_SIZE / TextSize.sp(TypeRole.Tafsir, TextSize.DEFAULT)
            } else {
                1f
            }
            for (step in steps) {
                val body = TextSize.sp(TypeRole.Tafsir, step) * prose
                val title = TypeLadder.articleTitleSp(body)
                val head = TypeLadder.headingSp(body)
                val sub = TypeLadder.subheadingSp(body)
                Text(
                    text = if (bangla) "আল-বাকারা ২:২৫৫" else "Al-Baqarah 2:255",
                    style = voice.title(sizeSp = title),
                    color = ink,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = if (bangla) "আয়াতুল কুরসির মর্যাদা" else "The Virtue of Ayat Al-Kursi",
                    style = voice.title(sizeSp = head),
                    color = ink,
                )
                Text(
                    text = if (bangla) "উপশিরোনামের এক লাইন" else "A line under the sub-head",
                    style = voice.title(sizeSp = sub, weight = FontWeight.Medium),
                    color = ink,
                )
                Text(
                    text = if (bangla) {
                        "অনুবাদটি পাঠকের নিজের মাপে, যাতে উপরের লাইনগুলোর সাথে তার ধাপ পড়া যায়।"
                    } else {
                        "The passage at the reader's own step, so its staircase can be read " +
                            "against the lines above it."
                    },
                    style = voice.prose(sizeSp = body, lineSp = body * voice.lineRatio),
                    color = ink,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (bangla) {
                    "ধাপ: " + steps.joinToString(" · ") { "%.1f".format(it) }
                } else {
                    "Steps: " + steps.joinToString(" · ") { "%.1f".format(it) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = quiet,
            )
        }
    }
}
