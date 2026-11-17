package io.github.muntasimulhaque.quran.core

/**
 * A relative link inside a surah's introduction.
 *
 * The introductions the content carries link to their own places as
 * `/2/67-73`: Al-Baqarah from ayah 67 to 73, a range whose first verse is what
 * a tap can open. A link with one number, `/2`, names the surah alone. The
 * parser accepts the leading slash or its absence, and refuses a surah
 * outside the Book, an ayah below one, or a shape it does not know, so a link
 * the content carries can never become a jump to a place that is not there
 * (owner decision, 19.7).
 */
object ReferenceLink {

    /** The surah and first ayah the link names, or null when it names none. */
    fun parse(href: String): Search.Reference? {
        val parts = href.trim().trimStart('/').split('/')
        if (parts.size !in 1..2) return null
        val surah = parts[0].toIntOrNull() ?: return null
        if (surah !in 1..114) return null
        if (parts.size == 1) return Search.Reference(surah, null)
        val first = parts[1].substringBefore('-').toIntOrNull() ?: return null
        if (first < 1) return null
        return Search.Reference(surah, first)
    }
}
