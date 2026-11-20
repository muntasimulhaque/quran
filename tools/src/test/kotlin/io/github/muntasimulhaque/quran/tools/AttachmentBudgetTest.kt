package io.github.muntasimulhaque.quran.tools

import io.github.muntasimulhaque.quran.core.Attachments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The catalog, weighed against the working set a screen may hold.
 *
 * SQLite attaches ten databases to one connection and the app keeps one
 * spare, so a surface may ask for nine packs. The reading's screen is the
 * largest: every enabled translation is a column, every enabled tafsir is a
 * card the reader may open, and one word list, its root, and one voice stand
 * beside them. The catalog is the only thing that can grow that set without
 * a line of app code, so it is weighed here: the day a pack would push the
 * screen past the budget, the build fails and names the count, and the fix
 * is a decision (a column cap, or attaching a tafsir only when its card
 * opens) rather than a reader meeting a missing pack (owner report, 20.5).
 */
class AttachmentBudgetTest {

    @Test
    fun theCatalogFitsOneScreenInsideTheAttachmentBudget() {
        val packs = catalogPacks()
        val translations = idsOf(packs, "translation")
        val tafsirs = idsOf(packs, "tafsir")
        // The screen reads one language at a time, so one word list and one
        // voice are the most it can ask for, even with every language on the
        // shelf.
        val words = idsOf(packs, "words").firstOrNull()?.let { listOf(it) } ?: emptyList()
        val root = idsOf(packs, "word-root").take(1)
        val voice = idsOf(packs, "recitation").take(1)
        val wanted = (translations + tafsirs + words + root + voice).distinct()

        assertTrue(
            "the reading's screen can ask for ${wanted.size} packs (${translations.size} translations, " +
                "${tafsirs.size} tafsirs, one word list, one root, one voice) and the budget is " +
                "${Attachments.BUDGET}. Cap the columns or attach a tafsir only when its card opens, " +
                "then raise the budget or batch the search.",
            wanted.size <= Attachments.BUDGET,
        )
        assertTrue(
            "the built set must be the whole wanted set, never a truncation",
            Attachments.screen(
                translations = translations,
                tafsirs = tafsirs,
                words = words.firstOrNull(),
                root = root.firstOrNull(),
                voice = voice.firstOrNull(),
            ).size == wanted.size,
        )
    }

    @Test
    fun oneLanguageOfSearchFitsInsideTheBudget() {
        val packs = catalogPacks()
        val byLanguage = packs
            .filter { it.type in setOf("translation", "tafsir", "words") }
            .groupBy { it.language }
        for ((language, group) in byLanguage) {
            val wanted = Attachments.search(
                translations = group.filter { it.type == "translation" }.map { it.id },
                tafsirs = group.filter { it.type == "tafsir" }.map { it.id },
                words = group.firstOrNull { it.type == "words" }?.id,
            )
            val distinct = group.map { it.id }.distinct().size
            assertTrue(
                "$language asks one search for $distinct packs and the budget is " +
                    "${Attachments.BUDGET}",
                distinct <= Attachments.BUDGET && wanted.size == distinct,
            )
        }
    }

    @Test
    fun theReminderFitsInsideTheBudget() {
        val translations = idsOf(catalogPacks(), "translation")
        val wanted = Attachments.reminder(translations)
        assertTrue(
            "the reminder asks for ${translations.size} translations and the budget is " +
                "${Attachments.BUDGET}",
            translations.size <= Attachments.BUDGET && wanted.size == translations.size,
        )
    }

    private data class Pack(val id: String, val type: String, val language: String)

    private fun catalogPacks(): List<Pack> {
        val text = File(projectRoot(), "content/catalog.json").readText()
        val root = Json.parseToJsonElement(text).jsonObject
        return root["packs"]?.jsonArray.orEmpty().mapNotNull { element ->
            val pack = element.jsonObject
            Pack(
                id = pack["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                type = pack["type"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                language = pack["language"]?.jsonPrimitive?.content ?: "ar",
            )
        }
    }

    private fun idsOf(packs: List<Pack>, type: String): List<String> =
        packs.filter { it.type == type }.map { it.id }

    /** The module runs from `tools/`, so the repository is found upward. */
    private fun projectRoot(): File {
        var directory = File(".").canonicalFile
        while (directory.parentFile != null && !File(directory, "content/quran.db").isFile) {
            directory = directory.parentFile ?: break
        }
        return directory
    }
}
