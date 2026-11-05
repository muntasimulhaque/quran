package io.github.muntasimulhaque.quran

import android.Manifest
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.WordSegment
import io.github.muntasimulhaque.quran.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * The Opus packages through the app's own player, at the three ayahs that
 * matter on every surah: its first, its middle, and its last, for both
 * reciters. The files are staged on the device by the release check, because
 * a two gigabyte pack set cannot ride in a test APK. Without that staging
 * this class is skipped, not failed, so CI still holds the rest of the
 * surface.
 *
 * Each ayah is played through Media3 from the URI [RecitationStore] returns,
 * which is the path a reader's tap takes. The test proves the format is Ogg
 * Opus mono 48 kHz, the duration is readable, and a seek to the quarter, the
 * middle, and the three-quarter of the ayah lands and advances; it also reads
 * the word region under each landed position, which is the same region the
 * reading's wash draws. The host has already proved the decoded waveform is
 * aligned with the MP3 source at those points; this test proves the app's
 * player and its stored timing agree on the files that shipped.
 *
 * The last leg uses [PlaybackController] itself, so the word wash is seen
 * moving on the app's own state and not only in the test's arithmetic.
 * Everything the class installs or stages it hands back in [AfterClass],
 * because every instrumented class in a run shares one app process and one
 * set of files (owner decision, the Opus migration).
 */
@RunWith(AndroidJUnit4::class)
class RecitationFollowTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = PackStore(context)
    private val recitations = File(context.filesDir, "recitations")
    private var staged = false
    private var database: ContentDatabase? = null
    private var player: ExoPlayer? = null
    private var controller: PlaybackController? = null

    @Before
    fun openTheStagedLibrary() = runBlocking {
        staged = File(recitations, ".follow-staged").isFile
        Assume.assumeTrue("the follow audio is not staged on this device", staged)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        val installedBefore = store.installed()
        for (id in listOf("reciter-minshawi", "reciter-husary")) {
            assertTrue("the build must carry the $id pack", store.install(id))
            if (id !in installedBefore) installedByClass += id
        }
        val catalog = PackCatalog.load(context)
        database = ContentDatabase.open(
            context,
            catalog,
            store.installed() + PackCatalog.CORE_ID,
        )
    }

    @After
    fun handTheAppBack() = runBlocking {
        if (!staged) return@runBlocking
        controller?.let { current -> mainSync { current.release() } }
        controller = null
        player?.let { current -> mainSync { current.release() } }
        player = null
        database?.close()
        database = null
        PlaybackPositionStore(context).clear()
    }

    @Test
    fun everySurahPlaysItsFirstMiddleAndLastAyah() = runBlocking {
        val database = database ?: return@runBlocking
        val session = mainValue { ExoPlayer.Builder(context).build() }
        player = session
        val problems = mutableListOf<String>()
        var checked = 0
        var moved = 0
        for (reciter in RECITERS) {
            for (surah in 1..114) {
                val ayahs = database.ayahsOfSurah(surah)
                if (ayahs.isEmpty()) {
                    problems += "$reciter surah $surah has no ayahs"
                    continue
                }
                val picked = listOf(ayahs.first(), ayahs[ayahs.size / 2], ayahs.last())
                for (ayah in picked) {
                    val audio = database.recitationAyah(reciter, ayah.number)
                    if (audio == null) {
                        problems += "$reciter ${ayah.verseKey} has no timing row"
                        continue
                    }
                    val uri = RecitationStore(context).uri(audio.audioPath)
                    if (uri == null) {
                        problems += "$reciter ${ayah.verseKey} is missing ${audio.audioPath}"
                        continue
                    }
                    assertEquals(
                        "$reciter ${ayah.verseKey} must be a staged file",
                        "file",
                        uri.scheme,
                    )
                    val result = playOne(session, uri, audio.segments)
                    if (result == null) {
                        problems += "$reciter ${ayah.verseKey} did not become ready"
                        continue
                    }
                    if (result.distinctWords >= 2) moved++
                    checked++
                    if (result.distinctWords < 1) {
                        problems += "$reciter ${ayah.verseKey} washed no word at any sampled point"
                    }
                }
            }
        }
        mainSync { session.stop() }
        if (problems.isEmpty()) {
            println(
                "recitation follow: $checked ayahs played, $moved with more than one word " +
                    "under the quarter, middle, and three-quarter points",
            )
        }
        assertTrue(
            "every first, middle, and last ayah must play from its own Opus file:\n" +
                problems.take(20).joinToString("\n"),
            problems.isEmpty(),
        )
        assertTrue(
            "the wash must move over the sampled points of most ayahs",
            moved >= checked * 8 / 10,
        )
    }

    @Test
    fun theWordWashMovesThroughTheAppController() = runBlocking {
        val database = database ?: return@runBlocking
        // 2:255 is long enough to see the wash move even on its first word.
        // The service asks for audio focus, and the platform denies it to a
        // background app with this target. The reading's own activity is the
        // foreground the reader would have; opening it is what makes the
        // service a real playback instead of a paused one (owner report, the
        // Opus migration).
        val ayahNumber = 262
        val reading = ActivityScenario.launch(MainActivity::class.java)
        try {
        for (reciter in RECITERS) {
            val audio = database.recitationAyah(reciter, ayahNumber)
            assertNotNull("$reciter must carry 2:255", audio)
            val words = audio!!.segments.map { it.wordTo }.toSet()
            assertTrue("2:255 must have a word list", words.isNotEmpty())
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            val playback = PlaybackController(context, scope)
            playback.attach(database)
            playback.setWordTracking(true)
            try {
                playback.play(reciter, ayahNumber, resume = false)
                println(
                    "recitation follow: $reciter 2:255 ${audio.audioPath} " +
                        "uri=${RecitationStore(context).uri(audio.audioPath)} state=${playback.state.value}",
                )
                val firstWord = withTimeoutOrNull(15_000) {
                    playback.wordPosition.first { it != null }
                }
                if (firstWord == null) {
                    fail("the wash did not start; state=${playback.state.value}")
                }
                assertTrue("the first wash must name a drawn word", firstWord!! in words)
                val next = withTimeoutOrNull(15_000) {
                    playback.wordPosition.first { it != null && it != firstWord }
                }
                if (next == null) {
                    fail("the wash did not move; state=${playback.state.value}")
                }
                assertTrue("the wash must reach a different word", next!! in words)
                assertTrue("the wash must move forward", next > firstWord)
            } finally {
                withContext(Dispatchers.Main) {
                    playback.stop()
                    playback.release()
                }
                scope.cancel()
            }
        }
        } finally {
            reading.close()
        }
    }

    private data class Played(val distinctWords: Int)

    /**
     * One ayah, three points. The seek is issued by the player the app uses,
     * and the region is read from the same segment list the wash reads.
     */
    private fun playOne(
        session: ExoPlayer,
        uri: android.net.Uri,
        segments: List<WordSegment>,
    ): Played? {
        mainSync {
            session.setMediaItem(MediaItem.fromUri(uri))
            session.prepare()
        }
        if (!awaitReady(session)) return null
        val duration = mainValue { session.duration }
        // A containers duration is normally read from the last Ogg page. If
        // the extractor cannot know it, the segments still end inside the
        // audio, so their last end is a safe span to sample without seeking
        // past the file.
        val span = when {
            duration != C.TIME_UNSET && duration > 0 -> duration
            else -> (segments.lastOrNull()?.endMs ?: 0L) + 500L
        }
        if (span <= 0) return null
        val format = audioFormat(session) ?: return null
        if (format.sampleMimeType != MimeTypes.AUDIO_OPUS ||
            format.channelCount != 1 ||
            format.sampleRate != 48_000
        ) {
            return null
        }
        val words = mutableSetOf<Int>()
        val lastWord = segments.maxOfOrNull { it.wordTo } ?: 0
        for (fraction in listOf(0.25, 0.5, 0.75)) {
            val target = (span * fraction).toLong()
            mainSync {
                session.seekTo(target)
                session.play()
            }
            Thread.sleep(220)
            val position = mainValue { session.currentPosition }
            if (position < target - 100 || position > target + 1_000) return null
            val segment = segments.lastOrNull { it.startMs <= position }
            if (segment != null && segment.wordTo !in 1..lastWord) return null
            segment?.let { words += it.wordTo }
        }
        mainSync { session.stop() }
        return Played(words.size)
    }

    private fun audioFormat(session: ExoPlayer): Format? {
        val deadline = System.currentTimeMillis() + 3_000
        while (System.currentTimeMillis() < deadline) {
            val group = mainValue { session.currentTracks }
                .groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.length > 0 }
            if (group != null) return group.getTrackFormat(0)
            Thread.sleep(50)
        }
        return null
    }

    private fun awaitReady(session: ExoPlayer): Boolean {
        val deadline = System.currentTimeMillis() + 8_000
        while (System.currentTimeMillis() < deadline) {
            when (mainValue { session.playbackState }) {
                Player.STATE_READY -> return true
                Player.STATE_ENDED -> return false
            }
            Thread.sleep(40)
        }
        return false
    }

    private fun mainSync(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    private fun <T> mainValue(block: () -> T): T {
        val value = AtomicReference<T>()
        instrumentation.runOnMainSync { value.set(block()) }
        return value.get()
    }

    private companion object {
        /** The database id and the folder its packages install under. */
        /** The database ids; the folder is inside each ayah path. */
        val RECITERS = listOf("minshawi", "husary")

        /** Packs this class installed, so the class removes exactly those. */
        val installedByClass = mutableSetOf<String>()

        @AfterClass
        @JvmStatic
        fun handTheStageBack() = runBlocking {
            val app = ApplicationProvider.getApplicationContext<android.content.Context>()
            PlaybackPositionStore(app).clear()
            for (id in installedByClass) PackStore(app).remove(id)
            installedByClass.clear()
            val directory = File(app.filesDir, "recitations")
            if (File(directory, ".follow-staged").isFile) directory.deleteRecursively()
        }
    }
}
