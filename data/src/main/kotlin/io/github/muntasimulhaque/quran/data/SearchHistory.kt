package io.github.muntasimulhaque.quran.data

import android.content.Context
import java.util.Locale

/**
 * The reader's recent queries, kept on the phone and nowhere else.
 *
 * A search begins with a word the reader has used before more often than not,
 * so an empty sheet offers the last few back as chips. A query is recorded
 * when a result is opened, never when a key is typed: a half-typed word is not
 * a search, and a list of them is a list of nothing. Five at most, newest
 * first, and one spelling per query, because the same word typed in two cases
 * is one word. The reader can clear the list in the sheet, and the app never
 * sends it anywhere (owner decision, this session).
 *
 * The arithmetic lives in [RecentQueries] rather than here, so a machine with
 * no screen pins the cap, the dedupe, and the round trip; this class is only
 * the shelf the list stands on. The list is a preference on the device, and
 * the app's backup is off, so it leaves with the app when the reader removes
 * it and never arrives from anywhere else.
 */
class SearchHistory(context: Context) {

    private val preferences =
        context.applicationContext.getSharedPreferences("search-history", Context.MODE_PRIVATE)

    /** The reader's queries, newest first. */
    fun read(): List<String> = RecentQueries.decode(preferences.getString(KEY_QUERIES, null))

    /** [query] at the head of the list, on the phone and in the caller's copy. */
    fun record(query: String): List<String> {
        val next = RecentQueries.record(read(), query)
        write(next)
        return next
    }

    /** The list emptied, in one place and in the other. */
    fun clear(): List<String> {
        write(emptyList())
        return emptyList()
    }

    private fun write(queries: List<String>) {
        preferences.edit().putString(KEY_QUERIES, RecentQueries.encode(queries)).apply()
    }

    private companion object {
        const val KEY_QUERIES = "queries"
    }
}

/** The arithmetic of the recent list, apart from the shelf it is stored on. */
object RecentQueries {

    /** How many queries the sheet offers back: more than a hand can scan. */
    const val LIMIT = 5

    /** A run of spaces of any kind, folded to one before a query is kept. */
    private val Spaces = Regex("\\s+")

    /**
     * [query] at the head, one spelling per query, at most [LIMIT].
     *
     * The newest spelling wins, so a reader who typed a word with a capital
     * letter this time sees it that way, and the older row is not kept beside
     * it. A blank query changes nothing: nothing was searched.
     */
    fun record(existing: List<String>, query: String): List<String> {
        val trimmed = query.trim().replace(Spaces, " ")
        if (trimmed.isEmpty()) return existing.take(LIMIT)
        val folded = trimmed.lowercase(Locale.ROOT)
        return buildList {
            add(trimmed)
            existing.forEach { line ->
                val kept = line.trim().replace(Spaces, " ")
                if (kept.isNotEmpty() && kept.lowercase(Locale.ROOT) != folded) add(kept)
            }
        }.take(LIMIT)
    }

    /** Newline is the one separator a single-line query cannot hold. */
    fun encode(queries: List<String>): String = queries.joinToString("\n")

    /** The stored list, cleaned of the blanks and the duplicate spellings. */
    fun decode(raw: String?): List<String> =
        raw.orEmpty()
            .split('\n')
            .map { it.trim().replace(Spaces, " ") }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(LIMIT)
}
