package io.github.muntasimulhaque.quran

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.LanguagePreference
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.TextSize
import io.github.muntasimulhaque.quran.data.TypeRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import org.junit.runner.RunWith

/**
 * The word sheet's one control, offered for every word.
 *
 * A press on the speaker is the reader's ask, so the mark stands on every
 * word: a word whose file is here sounds, a word whose surah has not arrived
 * fetches the surah, and a word the alignment cut no clip for says so rather
 * than playing nothing (owner decision, this session). This class holds the
 * three sides it can hold without a network: Al-Fatiha, whose first 29 words
 * the app carries; Al-Baqarah, whose word pack is not here in this run, so its
 * speaker must still be drawn; and a surah installed with one word file and a
 * word it does not cover, which must answer with the missing-recording line.
 *
 * Each leg saves the reader's settings before it runs and puts them back
 * after, because every instrumented class in a run shares one process and one
 * set of settings.
 */
@RunWith(AndroidJUnit4::class)
class WordVoiceTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var original: AppSettings

    @Before
    fun prepare() {
        LanguagePreference(context).set("en")
        original = runBlocking { SettingsStore(context).settings.first() }
        val store = PackStore(context)
        store.install("words-en")
        store.install("word-root")
        fakeSurahWord()
        runBlocking {
            SettingsStore(context).apply {
                setUiLanguage("en")
                setShowTranslation(true)
                setWordByWord(true)
                for (role in TypeRole.entries) setTypeSize(role, TextSize.DEFAULT)
            }
        }
        Thread.sleep(300)
    }

    @After
    fun restore() {
        clearFakeSurahWord()
        runBlocking {
            SettingsStore(context).apply {
                setAyah(original.ayah)
                original.uiLanguage?.let { setUiLanguage(it) }
                setShowTranslation(original.showTranslation)
                setWordByWord(original.wordByWord)
                for (role in TypeRole.entries) setTypeSize(role, original.sizeOf(role))
            }
        }
    }

    /**
     * Al-Fatiha's first 29 words ship with the app, so the sheet's one control
     * here: the word can be heard without a download.
     */
    @Test
    fun theSpeakerIsDrawnForABundledWord() {
        atAyah(FATIHA_FIRST)
        onTheSheetOf("بِسۡمِ") {
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithTag("word-sheet-hear", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    /**
     * The speaker answers on the sheet and nowhere else. A word is a sample,
     * not a session: it is one word file on its own player, so no capsule
     * rises behind the sheet while it sounds, and none is left behind when the
     * word ends (owner decision, this session).
     */
    @Test
    fun theWordsVoiceRaisesNoCapsule() {
        atAyah(FATIHA_FIRST)
        onTheSheetOf("بِسۡمِ") {
            compose.onAllNodesWithTag("word-sheet-hear", useUnmergedTree = true)
                .onFirst()
                .performClick()
            // The word is a second or so. Past its end the sheet is the word
            // and its gloss again, and the foot of the reading is bare: a word
            // never stands in the session's playlist, so there is nothing it
            // could leave paused there.
            Thread.sleep(2_500)
            assertTrue(
                "the word's own voice must raise no capsule",
                compose.onAllNodesWithTag("playback-bar", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    /**
     * Al-Baqarah's audio is a download the reader has not asked for, and the
     * speaker is drawn anyway: the press is the ask, so the sheet never opens
     * without its one control (owner decision, this session).
     *
     * The word is 2:7's first, which has a root (kh-t-m), because the sheet's
     * root line proves the sheet is whole while the voice is a download away.
     */
    @Test
    fun theSpeakerIsDrawnForAWordWhoseSurahIsNot() {
        atAyah(BAQARAH_SEVENTH)
        onTheSheetOf("خَتَمَ") {
            assertTrue(
                "the sheet must name the word's root",
                compose.onAllNodesWithTag("word-sheet-root", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty(),
            )
            assertTrue(
                "a word whose surah is not here must still offer its speaker",
                compose.onAllNodesWithTag("word-sheet-hear", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    /**
     * The pack is on the device and this word has no clip in it, so the press
     * answers with the word's own silence and not with a dead mark or a
     * download it does not need. The one fake file is the pack's proof that
     * the surah is here; the word under the finger is not the one it covers.
     */
    @Test
    fun theSheetSaysAWordTheAlignmentCutNoClipFor() {
        atAyah(BAQARAH_SEVENTH)
        onTheSheetOf("خَتَمَ") {
            compose.onAllNodesWithTag("word-sheet-hear", useUnmergedTree = true)
                .onFirst()
                .performClick()
            compose.waitUntil(timeoutMillis = 30_000) {
                compose.onAllNodesWithTag("word-sheet-voice", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText(
                "no recording of its own",
                substring = true,
                useUnmergedTree = true,
            ).onFirst().assertExists()
        }
    }


    /** A one-byte stand-in for a word file the app would otherwise fetch. */
    private fun fakeSurahWord() {
        val folder = File(context.filesDir, "word-audio/002").apply { mkdirs() }
        File(folder, "002_007_002.opus").writeBytes(ByteArray(1))
    }

    private fun clearFakeSurahWord() {
        File(context.filesDir, "word-audio/002").deleteRecursively()
    }

    private fun atAyah(number: Int) {
        runBlocking { SettingsStore(context).apply { setAyah(number) } }
        Thread.sleep(300)
    }

    /**
     * Presses and holds the verse's Arabic line, whose first word stands at
     * its right edge because the line runs right to left, and hands the open
     * sheet to [check] while the activity is still on the glass.
     */
    private fun onTheSheetOf(word: String, check: () -> Unit) {
        val reading = ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java),
        )
        try {
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithText(word, substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText(word, substring = true, useUnmergedTree = true)
                .onFirst()
                .holdTheFirstWord()
            compose.waitUntil(timeoutMillis = 60_000) {
                compose.onAllNodesWithTag("word-sheet", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            check()
        } finally {
            reading.close()
        }
    }

    private companion object {
        /** 1:1 in the Quran's sequential numbering. */
        const val FATIHA_FIRST = 1

        /** 2:7 in the Quran's sequential numbering: seven ayahs, then six. */
        const val BAQARAH_SEVENTH = 14
    }
}
