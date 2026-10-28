package io.github.muntasimulhaque.quran.core

/**
 * The reference a passage of commentary covers: "41:46-48" for a span, and
 * "41:48" for one ayah.
 *
 * The ayah card heads a passage with the pack's own reference, read from the
 * pack that returned it, so this one string is what the reader is told the
 * prose covers. A single ayah stays a single reference and never takes a
 * hyphen, so a range of one cannot read as a span the commentary does not
 * have (owner decision, this session).
 */
fun ayahSpan(surah: Int, fromAyah: Int, toAyah: Int): String =
    if (fromAyah == toAyah) "$surah:$fromAyah" else "$surah:$fromAyah-$toAyah"
