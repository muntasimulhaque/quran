package io.github.muntasimulhaque.quran

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.AppTheme
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.feature.reading.R as ReadingR
import io.github.muntasimulhaque.quran.playback.PlaybackService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.io.File

/**
 * The listening mark, as it is drawn: the reciter's word wearing the page's
 * word mark, over the reading, with the pill at the foot.
 *
 * The mark is a tint and not a word, so no semantics tree can carry it: the
 * frame is the whole evidence. Al-Fatiha is bundled in both voices and the
 * timing pack ships beside the app, so Play on 1:1 starts with no offer and
 * the first word's mark lands on its own, which is what the two frames keep,
 * a few seconds apart. Whether the mark has moved by the second frame is the
 * word clock's business and not the mark's: an emulator whose audio timestamp
 * stalls holds the first word, and the pair is then one frame twice. Nothing
 * here depends on the two differing.
 *
 * The class borrows the tour's two rules: it writes through
 * [screenshotOutputDirectory] and nowhere else, and it hands the app back as
 * it found it, because every instrumented class in a run shares one process
 * and one set of settings.
 */
@RunWith(AndroidJUnit4::class)
class ListeningMarkCaptureTest {

    private val compose = createAndroidComposeRule<MainActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var original: AppSettings = AppSettings()

    /** A reading on 1:1 that is the first open a fresh install gives. */
    private val library = object : ExternalResource() {
        override fun before() {
            original = runBlocking { SettingsStore(context).settings.first() }
            LanguagePreference(context).set("en")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                InstrumentationRegistry.getInstrumentation().uiAutomation
                    .grantRuntimePermission(
                        context.packageName,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
            }
            runBlocking {
                SettingsStore(context).apply {
                    setUiLanguage("en")
                    setAyah(1)
                    setTranslationPacks(emptySet())
                    setTafsirPacks(emptySet())
                    setShowTranslation(true)
                    setWordByWord(false)
                    setRecitation("husary")
                    // The frame is the day ground, not the emulator's own
                    // setting: a capture that follows the machine is a frame
                    // of the machine (owner decision, this session).
                    setTheme(AppTheme.Light)
                }
                PackStore(context).apply {
                    remove("reciter-husary")
                    remove("reciter-minshawi")
                }
                RecitationStore(context).apply {
                    removeAll("husary")
                    removeAll("minshawyMurattal")
                }
                PlaybackPositionStore(context).clear()
            }
        }
    }

    @get:Rule
    val rule: TestRule = RuleChain.outerRule(library).around(compose)

    @Test
    fun theRecitersWordWearsTheWordMark() {
        val page = compose.activity.getString(ReadingR.string.reading_page_description)
        compose.waitUntil(timeoutMillis = 180_000) {
            compose.onAllNodesWithContentDescription(page).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("door-1:1").performClick()
        val play = compose.activity.getString(R.string.action_play)
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithText(play).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(play).performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodesWithTag("playback-bar").fetchSemanticsNodes().isNotEmpty()
        }
        // The timings drive the mark, so the frame waits for the word clock to
        // have moved rather than for the screen to be idle.
        Thread.sleep(1_500)
        capture("listening-mark")
        Thread.sleep(2_500)
        capture("listening-mark-next-word")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val frame = compose.onRoot().captureToImage().asAndroidBitmap()
        File(screenshotOutputDirectory(), "$name.png").outputStream().use { output ->
            frame.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    @After
    fun handTheAppBack() {
        context.stopService(Intent(context, PlaybackService::class.java))
        runBlocking {
            SettingsStore(context).apply {
                setAyah(original.ayah)
                setRecitation(original.recitation)
                setTranslationPacks(original.translationPacks)
                setTafsirPacks(original.tafsirPacks)
                setShowTranslation(original.showTranslation)
                setWordByWord(original.wordByWord)
                setTheme(original.theme)
                original.uiLanguage?.let { setUiLanguage(it) }
            }
            PlaybackPositionStore(context).clear()
        }
    }
}
