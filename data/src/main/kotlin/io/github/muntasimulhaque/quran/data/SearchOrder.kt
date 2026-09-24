package io.github.muntasimulhaque.quran.data

/**
 * The order a search result list is read in: the kinds run from the verse
 * outward, and each kind keeps the Book's own order inside itself.
 *
 * 1. A typed reference (the reader named an exact place).
 * 2. A matched surah name (they named a surah).
 * 3. Ayahs whose **Arabic text** matched.
 * 4. Ayahs whose **translation** matched.
 * 5. Ayahs whose **word meaning** matched.
 * 6. **Tafsir** passages, which are a comment about a verse rather than the
 *    verse.
 *
 * Before this, one Mushaf-order list mixed the kinds: a translation match for
 * 2:2 sat above an Arabic match for 2:255. The Book's own order is still kept,
 * it just no longer mixes what kind of thing a reader found (owner decision,
 * 28, D-090).
 *
 * An ayah that matched in more than one source ranks by its strongest one and
 * still draws every source it matched, so a row never loses material. The
 * function is pure so the order is pinned by a JVM test rather than by a
 * device reading, which is what the first cut of this test got wrong.
 */
fun orderSearchHits(
    leading: List<SearchHit>,
    ayahHits: List<SearchHit.AyahHit>,
    tafsirHits: List<SearchHit.TafsirHitResult>,
    limit: Int,
): Pair<List<SearchHit>, Boolean> {
    val merged = ArrayList<SearchHit>(leading.size + ayahHits.size + tafsirHits.size)
    merged += leading
    for (kind in 0..2) {
        merged += ayahHits
            .filter { ayahSearchKind(it) == kind }
            .sortedBy { it.ayah.number }
    }
    merged += tafsirHits.sortedWith(compareBy({ it.ayah.number }, { it.pack }))
    // A reader wants one row per passage, not one per ayah it covers.
    val deduped = ArrayList<SearchHit>(merged.size)
    var lastTafsir: SearchHit.TafsirHitResult? = null
    for (hit in merged) {
        if (hit is SearchHit.TafsirHitResult) {
            if (lastTafsir != null && lastTafsir.pack == hit.pack &&
                lastTafsir.fromAyah == hit.fromAyah && lastTafsir.surah == hit.surah
            ) {
                continue
            }
            lastTafsir = hit
        }
        deduped += hit
    }
    return deduped.take(limit) to (deduped.size > limit)
}

/**
 * The Arabic text itself ranks first, the translation next, then the word
 * meanings. The number is the rank [orderSearchHits] orders by.
 */
private fun ayahSearchKind(hit: SearchHit.AyahHit): Int = when {
    hit.arabicMatchedWords.isNotEmpty() -> 0
    hit.translation != null -> 1
    else -> 2
}
