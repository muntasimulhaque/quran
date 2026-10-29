package io.github.muntasimulhaque.quran.ui

import io.github.muntasimulhaque.quran.data.ContentDatabase
import io.github.muntasimulhaque.quran.data.ContentPack
import io.github.muntasimulhaque.quran.data.ReadingRow
import io.github.muntasimulhaque.quran.data.TranslationLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

/**
 * The reading rows: one surah built in one pass, one ayah kept for the
 * smaller surfaces, and one surah read ahead while the reader is still in the
 * surah before it.
 *
 * The cache lives beside the view model rather than inside it: it is the one
 * part of the reading with a life of its own, it is read from three surfaces
 * at once (the reading list, the ayah card's share, and the size sample), and
 * it is the place where a pack arriving and a query in flight meet. Keeping
 * it whole here means the view model's own file is about the reader's choices
 * and the row cache's file is about the rows (owner decision, this session).
 *
 * Everything it answers is shaped by three things the caller owns: the open
 * library, the translation packs the reader has on, and the reader's own
 * language. Anything that changes one of them clears the cache through
 * [clear]: a row cached under one language and read back under another put
 * the Bangla translation in an English Font size sample (owner report).
 */
internal class ReadingRowsCache(
    /** The open library, or null while it is turning over. */
    private val database: () -> ContentDatabase?,
    /** The translations the reader has on, in the order they are drawn. */
    private val translationPacks: () -> List<ContentPack>,
    /** The reader's own language: the word meanings speak it and no other. */
    private val language: () -> String,
) {

    /**
     * One ayah's row, kept hot for the surfaces that ask for a single ayah:
     * the size sample and the share card. The reader's last hundred and
     * sixty ayahs of context are enough for both, and the bound is what
     * keeps a long reading from keeping the whole Book in memory.
     */
    private val rowCache = object : LinkedHashMap<Int, ReadingRow>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ReadingRow>?): Boolean = size > 160
    }

    /**
     * The library the rows on hand belong to. It is bumped by [clear], and
     * both caches carry the value they were read under, so a row read for
     * one set of packs can never be drawn under another.
     *
     * Both caches and the generation they share are guarded by [lock], which
     * is also what makes dropping them atomic: a read that lands while the
     * library is turning over is dropped rather than kept, because the check
     * and the store happen under one lock.
     */
    private val lock = Any()
    private var generation = 0L

    /**
     * The surah after the one being read, read ahead while the reader is
     * still in this one. One entry, replaced each time, and dropped whenever
     * the library turns over.
     */
    private var ahead: ReadAhead? = null

    /** One surah's rows, read ahead, and the library they were read under. */
    private class ReadAhead(
        val generation: Long,
        val surah: Int,
        val rows: List<ReadingRow>,
    )

    /**
     * The read-ahead copy of one surah's rows, when the library has not
     * turned over since they were read.
     *
     * Null on the first arrival at a surah and after any change of packs,
     * which is when the database is the only honest answer. It is read during
     * composition, so it must stay a plain field read and nothing more.
     */
    fun cached(surah: Int): List<ReadingRow>? = synchronized(lock) {
        ahead?.takeIf { it.generation == generation && it.surah == surah }?.rows
    }

    /**
     * Every row of one surah in a single pass: the ayahs, their words, every
     * translation the reader turned on, and the word by word aid. One query
     * per source instead of one per ayah, and the reading list is drawn
     * complete, so the reader's place lands exactly and nothing shifts under
     * them while they read.
     */
    suspend fun rows(surah: Int, wordByWord: Boolean): List<ReadingRow> =
        withContext(Dispatchers.IO) {
            runCatching {
                cached(surah)?.let { return@runCatching it }
                val open = database() ?: return@runCatching emptyList()
                val ayahs = open.ayahsOfSurah(surah)
                if (ayahs.isEmpty()) return@runCatching emptyList()
                val numbers = ayahs.map { it.number }
                val words = open.wordsForAyahs(numbers)
                val lines = translationLines(open, numbers)
                val meanings = if (wordByWord) {
                    open.wordMeanings(numbers, language())
                } else {
                    emptyMap()
                }
                ayahs.map { ayah ->
                    ReadingRow(
                        ayah = ayah,
                        words = words[ayah.number].orEmpty(),
                        translations = lines[ayah.number].orEmpty(),
                        meanings = meanings[ayah.number].orEmpty(),
                    )
                }
            }.getOrElse { failure ->
                // One unreadable pack must not take the reading down with it:
                // the page stays paper until the library can answer, and the
                // pack's own row reads Add so the reader can repair it.
                android.util.Log.e(TAG, "the rows of surah $surah could not be read", failure)
                emptyList()
            }
        }

    /**
     * One ayah's row, from the cache when it is hot and from the library
     * otherwise. The caller is on a worker when it reads the library, which
     * is every call site but the size sample, and that one wraps this in one.
     */
    fun row(ayahNumber: Int, wordByWord: Boolean): ReadingRow? {
        // The generation is read before the database, and the row is kept only
        // under the same generation. A pack arriving, a language change, or a
        // switch of the word aid clears the cache on the main thread while a
        // worker may be reading one ayah for the size sample or the share
        // card; without this check that worker could put the old library's row
        // back after the clear, and the next surface would draw it.
        val readUnder = synchronized(lock) { generation }
        val open = database() ?: return null
        synchronized(lock) { rowCache[ayahNumber] }?.let { return it }
        val ayah = open.ayah(ayahNumber) ?: return null
        val words = open.wordsForAyahs(listOf(ayahNumber))[ayahNumber].orEmpty()
        val translations = translationLines(open, listOf(ayahNumber))[ayahNumber].orEmpty()
        val meanings = if (wordByWord) {
            open.wordMeanings(ayahNumber, language())
        } else {
            emptyList()
        }
        val row = ReadingRow(ayah, words, translations, meanings)
        synchronized(lock) {
            if (generation != readUnder) return null
            rowCache[ayahNumber] = row
        }
        return row
    }

    /**
     * Reads the surah after [surah] while the reader is still in this one, so
     * the turn at the end of a surah lands on text that is already measured
     * instead of on the paper.
     *
     * One surah ahead is the whole of it: the reader who taps Next surah is
     * the reader this is for, and a surah they never reach is a read nobody
     * asked for. Nothing is read past the end of the Book.
     */
    suspend fun readAhead(surah: Int, wordByWord: Boolean) {
        val next = surah + 1
        if (next > TOTAL_SURAHS) return
        val readUnder = synchronized(lock) { generation }
        val rows = rows(next, wordByWord)
        // The library turned over under this read: the rows belong to packs
        // that are no longer on the device, so they are dropped rather than
        // offered to the next surah. The check and the store are under one
        // lock, so a clear that lands while this read was running cannot
        // leave the stale copy behind it.
        synchronized(lock) {
            if (generation == readUnder) ahead = ReadAhead(readUnder, next, rows)
        }
    }

    /** Drops everything: the library or the reader's packs have turned over. */
    fun clear() {
        synchronized(lock) {
            generation++
            rowCache.clear()
            ahead = null
        }
    }

    /**
     * Every translation the reader turned on for a set of ayahs, each read
     * from its own pack and named, so a row draws its lines without knowing
     * which packs are on.
     */
    private fun translationLines(
        open: ContentDatabase,
        numbers: List<Int>,
    ): Map<Int, List<TranslationLine>> {
        val out = HashMap<Int, MutableList<TranslationLine>>(numbers.size)
        for (pack in translationPacks()) {
            val found = open.translations(numbers, pack.id)
            for ((number, text) in found) {
                out.getOrPut(number) { mutableListOf() }
                    .add(TranslationLine(pack.id, pack.name, pack.language, text))
            }
        }
        return out
    }

    private companion object {
        const val TAG = "ReadingRowsCache"

        /** The Quran's surah count; nothing is read past the last one. */
        const val TOTAL_SURAHS = 114
    }
}
