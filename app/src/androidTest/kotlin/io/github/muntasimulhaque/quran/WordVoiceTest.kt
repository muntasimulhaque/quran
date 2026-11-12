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
import org.junit.runner.RunWith

/**
 * The word sheet's one control, offered where it can answer and left out where
 * it cannot.
 *
 * A speaker that plays nothing is a dead button, so the sheet draws it only
 * for a surah whose audio is on the device under the reader's reciter. This
 * class holds the two sides of that rule: Al-Fatiha, whose fourteen ayahs the
 * app carries, and Al-Baqarah, whose audio arrives only when the reader asks
 * for it and is not on the device in this run (owner decision, this session).
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
     * Al-Fatiha's audio ships with the app, so the sheet's one control is
     * here: the word can be heard without a download.
     */
    @Test
    fun theSpeakerIsDrawnForASurahWhoseAudioIsHere() {
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
     * not a session: it is one clipped item on its own player, so no capsule
     * rises behind the sheet while it sounds, and none is left behind when the
     * clip ends (owner decision, this session).
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
     * Al-Baqarah's audio is a download the reader has not asked for in this
     * run, so the mark is absent and the sheet is the word, its gloss, and its
     * root: a button that answered nothing is worse than no button.
     *
     * The word is 2:7's first, which has a root (kh-t-m), because the sheet's
     * root line is what proves the sheet is whole while the mark is missing:
     * a demonstrative or a relative pronoun has no root at all, and its sheet
     * is right to draw no line.
     */
    @Test
    fun theSpeakerIsLeftOutForASurahWhoseAudioIsNot() {
        atAyah(BAQARAH_SEVENTH)
        onTheSheetOf("خَتَمَ") {
            // The sheet is drawn and its root is named: the absence is the
            // mark's, not the sheet's.
            assertTrue(
                "the sheet must name the word's root",
                compose.onAllNodesWithTag("word-sheet-root", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty(),
            )
            assertTrue(
                "a surah with no audio must offer no speaker",
                compose.onAllNodesWithTag("word-sheet-hear", useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty(),
            )
        }
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
