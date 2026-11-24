package io.github.muntasimulhaque.quran.tools

/**
 * What adding a pack takes, one block per type.
 *
 * The pipeline has one door: a row in [PackSources], a pack file built from
 * the audited database, and a catalog that names both. Each block below is
 * that door for one type, in the order a person walks through it: the shape
 * an adapter has to produce, the row to add, the steps to run, and the checks
 * that must hold before the change is committed. A block is not a second
 * truth about the code: it is a checklist over the one registry, so the
 * command exists to make the order readable, not to explain what the code
 * does (owner decision, 20.2).
 *
 * No em dash appears here, and none may be added: this file is scanned like
 * any other.
 */
object Runbook {

    fun print() {
        println()
        println("adding a pack")
        println()
        println("Every pack is a row in tools PackSources and a file built from content/quran.db.")
        println("The app never names a pack: it reads the catalog, by type and language.")
        println()
        println("A translation:")
        println("  1. Write an adapter under tools that reads the source into the translation")
        println("     table: ayah_number, text, footnotes (the footnotes as JSON).")
        println("  2. Add its dataset to content/manifest.json with its source, version,")
        println("     checksum, and license.")
        println("  3. Add one row to PackSources with type translation, the datasets, and")
        println("     both names.")
        println("  4. Run: build, packs build, verify, checkdb, audit, search.")
        println("  5. If it is a language's default, add it to PackSources.languageDefaults")
        println("     and to that language's values-xx only if it is a new interface.")
        println()
        println("A tafsir:")
        println("  1. An adapter writes tafsir_passage (source_id, surah, from_ayah,")
        println("     to_ayah, text) and tafsir_ayah (ayah_number, passage_id).")
        println("  2. The rest is a translation's: manifest, one row, build, packs build,")
        println("     then every gate.")
        println()
        println("A recitation voice:")
        println("  1. Its word timings are a quran-align file, one per voice, placed under")
        println("     content/raw/quran-align and recorded in the manifest. The QUL export")
        println("     names the audio, the voice, and the credit; its own segments column is")
        println("     never read, because it was cut against a different edit of the take.")
        println("  2. Build reads both halves: audio_path from the export, and the words")
        println("     from the timing file through QuranAlignSegments, whose pinned tables")
        println("     hold the muqata'ah gaps and the words that rest with no region.")
        println("  3. The audio steps bring the files: audio packs, audio edges if the")
        println("     word boundaries need measuring, then audio publish.")
        println("  4. One row in PackSources with type recitation, its datasets naming")
        println("     both the recording and its timings, then build, packs build, packs")
        println("     publish, audio words, then every gate.")
        println()
        println("A word list:")
        println("  1. An adapter writes word_meaning with ayah_number, position, and")
        println("     meaning, and the grammar columns when the source has them.")
        println("  2. One row in PackSources with type words and the language in its id,")
        println("     then build, packs build, then every gate.")
        println("  3. The word list is the reader's own switch, so a language's defaults")
        println("     block names it but the first open never turns it on.")
        println()
        println("A word pronunciation set:")
        println("  1. word-audio cut builds the clips and their manifest; there is no")
        println("     database, only content/word-audio/manifest.json and the Opus files.")
        println("  2. A second voice is a new row in PackSources of type word-audio with")
        println("     its own datasets. No app code changes.")
        println("  3. Run word-audio publish, then packs build, then every gate.")
        println()
        println("A script (the Quran text):")
        println("  1. The core is the one reading: an adapter writes ayah and the metadata")
        println("     tables. Changing its version replaces every held copy, so the")
        println("     version moves only with a content decision.")
        println("  2. Run build, packs build, then every gate.")
        println()
        println("Every one of those ends here:")
        println("  tools verify, checkdb, audit, search, then from the repository root")
        println("  ./gradlew :core:test :tools:test :data:testDebugUnitTest")
        println("  ./gradlew :ui-kit:testDebugUnitTest :feature-browse:testDebugUnitTest")
        println("  ./gradlew :feature-playback:testDebugUnitTest :app:testDebugUnitTest")
        println("  ./gradlew :app:lintDebug :app:assembleDebug")
        println()
        println("And git status must show content/catalog.json with the new entry and")
        println("nothing else moved: every other pack's sha256 and bytes stay as they")
        println("were, and content/quran.db rebuilds to its committed hash.")
    }
}
