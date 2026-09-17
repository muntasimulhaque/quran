package io.github.muntasimulhaque.quran.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The read-only content database. It is copied once from the app's assets
 * into private storage, named by the content version, and opened read-only.
 * A new content version replaces the old copy on the next launch.
 */
class ContentDatabase private constructor(private val database: SQLiteDatabase) {

    fun surahs(): List<Surah> =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count " +
                "FROM surah ORDER BY number",
            null,
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Surah(
                            number = cursor.getInt(0),
                            nameArabic = cursor.getString(1),
                            nameSimple = cursor.getString(2),
                            nameLatin = cursor.getString(3),
                            revelationPlace = cursor.getString(4),
                            versesCount = cursor.getInt(5),
                        ),
                    )
                }
            }
        }

    fun surah(number: Int): Surah? =
        database.rawQuery(
            "SELECT number, name_arabic, name_simple, name_latin, revelation_place, verses_count " +
                "FROM surah WHERE number = ?",
            arrayOf(number.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            Surah(
                number = cursor.getInt(0),
                nameArabic = cursor.getString(1),
                nameSimple = cursor.getString(2),
                nameLatin = cursor.getString(3),
                revelationPlace = cursor.getString(4),
                versesCount = cursor.getInt(5),
            )
        }

    fun pageLines(page: Int): List<PageLine> =
        database.rawQuery(
            "SELECT line, type, centered, first_word_id, last_word_id, surah FROM page_line " +
                "WHERE page = ? ORDER BY line",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        PageLine(
                            line = cursor.getInt(0),
                            type = cursor.getString(1),
                            centered = cursor.getInt(2) == 1,
                            firstWordId = cursor.getInt(3),
                            lastWordId = cursor.getInt(4),
                            surah = cursor.getInt(5),
                        ),
                    )
                }
            }
        }

    fun words(firstId: Int, lastId: Int): List<Word> =
        database.rawQuery(
            "SELECT id, text, glyph, translation FROM word WHERE id BETWEEN ? AND ? ORDER BY id",
            arrayOf(firstId.toString(), lastId.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Word(
                            id = cursor.getInt(0),
                            text = cursor.getString(1),
                            glyph = cursor.getString(2),
                            translation = if (cursor.isNull(3)) null else cursor.getString(3),
                        ),
                    )
                }
            }
        }

    fun ayahsForPage(page: Int): List<Ayah> =
        database.rawQuery(
            "SELECT number, surah, ayah, verse_key, text FROM ayah WHERE page = ? ORDER BY number",
            arrayOf(page.toString()),
        ).use { cursor ->
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        Ayah(
                            number = cursor.getInt(0),
                            surah = cursor.getInt(1),
                            ayah = cursor.getInt(2),
                            verseKey = cursor.getString(3),
                            text = cursor.getString(4),
                        ),
                    )
                }
            }
        }

    fun translations(ayahNumbers: List<Int>): Map<Int, String> {
        if (ayahNumbers.isEmpty()) return emptyMap()
        val placeholders = ayahNumbers.joinToString(",") { "?" }
        return database.rawQuery(
            "SELECT ayah_number, text FROM translation WHERE ayah_number IN ($placeholders)",
            ayahNumbers.map { it.toString() }.toTypedArray(),
        ).use { cursor ->
            val out = HashMap<Int, String>(ayahNumbers.size)
            while (cursor.moveToNext()) out[cursor.getInt(0)] = cursor.getString(1)
            out
        }
    }

    fun pagePosition(page: Int): PagePosition? =
        database.rawQuery(
            "SELECT surah, juz, hizb FROM ayah WHERE page = ? ORDER BY number LIMIT 1",
            arrayOf(page.toString()),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            PagePosition(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))
        }

    fun firstPageOfSurah(surah: Int): Int =
        database.rawQuery("SELECT MIN(page) FROM ayah WHERE surah = ?", arrayOf(surah.toString()))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 1 }

    fun close() = database.close()

    companion object {
        suspend fun open(context: Context): ContentDatabase = withContext(Dispatchers.IO) {
            val version = context.assets.open("content/version.txt")
                .bufferedReader().use { it.readText().trim() }
            val directory = File(context.filesDir, "content").apply { mkdirs() }
            val target = File(directory, "quran-$version.db")
            if (!target.exists() || target.length() == 0L) {
                val temporary = File(directory, "quran-$version.db.part")
                context.assets.open("content/quran.db").use { input ->
                    temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
                directory.listFiles { file -> file.name != target.name }?.forEach { it.delete() }
            }
            ContentDatabase(
                SQLiteDatabase.openDatabase(
                    target.path,
                    null,
                    SQLiteDatabase.OPEN_READONLY,
                ),
            )
        }
    }
}
