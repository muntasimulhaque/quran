package io.github.muntasimulhaque.quran

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.muntasimulhaque.quran.data.AppSettings
import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.PackCatalog
import io.github.muntasimulhaque.quran.data.PackStore
import io.github.muntasimulhaque.quran.data.PlaybackPositionStore
import io.github.muntasimulhaque.quran.data.RecitationStore
import io.github.muntasimulhaque.quran.data.SettingsStore
import io.github.muntasimulhaque.quran.data.WordSegment
import io.github.muntasimulhaque.quran.ui.ContentLibrary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * Al-Fatiha is the one surah the app carries in both reciters' voices, so the
 * first Play on 1:1 has to work with no network and no offer. This class pins
 * the three parts of that promise: the two timing packs install from assets on
 * the first open, every ayah of the surah answers with a bundled asset, and
 * the word wash moves through the same ExoPlayer engine the app's service uses
 * for both reciters.
 *
 * The whole-surah rule is pinned by asking about 2:1: the debug sample carries
 * that one ayah, but surah 2 is not whole, so it must still be a download.
 */
@RunWith(AndroidJUnit4::class)
class BundledFatihaTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val packStore = PackStore(context)
    private val recitationStore = RecitationStore(context)
    private var original: AppSettings = AppSettings()

    @Before
    fun prepareTheReader() {
        original = runBlocking { SettingsStore(context).settings.first() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(
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
            }
            PlaybackPositionStore(context).clear()
        }
    }

    @After
    fun handTheAppBack() = runBlocking {
        SettingsStore(context).apply {
            setAyah(original.ayah)
            setRecitation(original.recitation)
            setTranslationPacks(original.translationPacks)
            setTafsirPacks(original.tafsirPacks)
            setShowTranslation(original.showTranslation)
            setWordByWord(original.wordByWord)
            original.uiLanguage?.let { setUiLanguage(it) }
        }
        PlaybackPositionStore(context).clear()
    }

    @Test
    fun theShippedTimingsInstallOnFirstOpenAndKeepEveryAyahPlayable() = runBlocking {
        // A fresh install has neither timing pack and no downloaded audio.
        packStore.remove("reciter-minshawi")
        packStore.remove("reciter-husary")
        recitationStore.removeAll(MINSHAWI_FOLDER)
        recitationStore.removeAll(HUSARY_FOLDER)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val opened = CompletableDeferred<ContentLibrary.Opening>()
        val library = ContentLibrary(
            application = context,
            scope = scope,
            settingsStore = SettingsStore(context),
            settings = { AppSettings() },
            onInstalled = { _: ContentPack, _: Boolean -> },
            onOpened = { opening -> opened.complete(opening) },
            onReopened = { },
            onRemoved = { _: ContentPack, _: String? -> },
        )
        try {
            library.open()
            withTimeout(90_000) { opened.await() }
            assertTrue(
                "the Minshawi timing pack must install from the first open",
                library.isInstalled("reciter-minshawi"),
            )
            assertTrue(
                "the Husary timing pack must install from the first open",
                library.isInstalled("reciter-husary"),
            )
            assertTrue(
                "Minshawi's whole Al-Fatiha must be playable",
                library.hasSurahAudio("minshawi", 1),
            )
            assertTrue(
                "Husary's whole Al-Fatiha must be playable",
                library.hasSurahAudio("husary", 1),
            )
            assertFalse(
                "2:1 is a sample, not a whole surah",
                library.hasSurahAudio("minshawi", 2),
            )
            for (path in fatihaPaths(MINSHAWI_FOLDER) + fatihaPaths(HUSARY_FOLDER)) {
                assertEquals(
                    "$path must come from the app's own assets",
                    "asset",
                    recitationStore.uri(path)?.scheme,
                )
            }
            // Removing the private folder is the Reciters page's own act; it
            // must not touch the bundled ayahs.
            assertFalse(
                "there is no downloaded Fatiha to remove",
                library.removeDownloads("minshawi", 1),
            )
            recitationStore.removeAll(MINSHAWI_FOLDER)
            assertEquals(
                "the bundled audio must survive a removal",
                "asset",
                recitationStore.uri("$MINSHAWI_FOLDER/001001.mp3")?.scheme,
            )
        } finally {
            library.retire()
            scope.cancel()
        }
    }

    @Test
    fun theWordWashMovesOnTheFirstMiddleAndLastAyahForBothReciters() = runBlocking {
        assertTrue("the build must carry the Minshawi pack", packStore.install("reciter-minshawi"))
        assertTrue("the build must carry the Husary pack", packStore.install("reciter-husary"))
        val database = ContentDatabase.open(
            context,
            PackCatalog.load(context),
            packStore.installed() + PackCatalog.CORE_ID,
        )
        val session = mainValue { ExoPlayer.Builder(context).build() }
        try {
            for (reciter in listOf("minshawi", "husary")) {
                for (ayahNumber in listOf(1, 4, 7)) {
                    val audio = database.recitationAyah(reciter, ayahNumber)
                        ?: throw AssertionError("$reciter $ayahNumber has no timing row")
                    val uri = recitationStore.uri(audio.audioPath)
                        ?: throw AssertionError("$reciter $ayahNumber has no bundled audio")
                    assertEquals("$reciter $ayahNumber must play from assets", "asset", uri.scheme)
                    val words = playOne(session, uri, audio.segments)
                    assertTrue(
                        "$reciter $ayahNumber must wash more than one word across its " +
                            "quarter, middle, and three-quarter points, got $words",
                        words.size >= 2,
                    )
                }
            }
        } finally {
            mainSync { session.release() }
            database.close()
            PlaybackPositionStore(context).clear()
        }
    }

    /**
     * One ayah, three points, through the same ExoPlayer engine the service
     * uses. The asset URI is the one [RecitationStore.uri] returns and the
     * segment list is the one the wash reads, so the two promises meet here:
     * the file plays from the app's own assets and the timing maps to drawn
     * words that move.
     */
    private fun playOne(
        session: ExoPlayer,
        uri: android.net.Uri,
        segments: List<WordSegment>,
    ): Set<Int> {
        mainSync {
            session.setMediaItem(MediaItem.fromUri(uri))
            session.prepare()
        }
        if (!awaitReady(session)) throw AssertionError("$uri did not become ready")
        val duration = mainValue { session.duration }
        val span = when {
            duration != C.TIME_UNSET && duration > 0 -> duration
            else -> (segments.lastOrNull()?.endMs ?: 0L) + 500L
        }
        if (span <= 0) throw AssertionError("$uri has no readable duration")
        val words = mutableSetOf<Int>()
        for (fraction in listOf(0.25, 0.5, 0.75)) {
            val target = (span * fraction).toLong()
            mainSync {
                session.seekTo(target)
                session.play()
            }
            Thread.sleep(220)
            val position = mainValue { session.currentPosition }
            if (position < target - 100 || position > target + 1_000) {
                throw AssertionError("$uri did not land at $target, it was at $position")
            }
            val segment = segments.lastOrNull { it.startMs <= position }
                ?: throw AssertionError("$uri has no timing at $position")
            words += segment.wordTo
        }
        mainSync { session.stop() }
        return words
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

    private fun fatihaPaths(folder: String): List<String> =
        (1..7).map { ayah -> "$folder/00100$ayah.mp3" }

    private companion object {
        const val MINSHAWI_FOLDER = "minshawyMurattal"
        const val HUSARY_FOLDER = "husary"
    }
}
