package io.github.muntasimulhaque.quran.data

/**
 * A pack's name in the interface's language.
 *
 * The catalog names every pack, and it names the app's own packs twice: the
 * English name and the Bangla one. This is the one rule that picks between
 * them, so the reading, the settings rows, the About page, the pill, and the
 * player's own notice all name a pack the same way in the same language. It
 * used to be a `when` over pack ids inside the app, which meant a name was a
 * code edit; it is content now, and a pack added to the catalog is named
 * everywhere without one (owner decision, 20.2).
 *
 * A blank Bangla name falls back to the English one rather than standing
 * empty, so a pack whose Bangla name nobody wrote is still named. The app
 * carries two interface languages and no more; a third would add its own name
 * to the catalog and its own line of choice here, because an interface
 * language is decided by the app's strings and not by its content.
 */
fun packName(name: String, nameBn: String, languageTag: String): String =
    if (UiLanguage.of(languageTag) == UiLanguage.Bangla && nameBn.isNotBlank()) nameBn else name

/** [packName] for one pack. */
fun ContentPack.displayName(languageTag: String): String = packName(name, nameBn, languageTag)

/**
 * A pack's own line in the reader's language: the catalog's Bangla line when
 * the interface is Bangla and the catalog carries one, and the English line
 * otherwise, so no pack is ever credited in a language nobody wrote for it
 * (owner decision, 12.6).
 *
 * The line is the About page's, which reads the phone's own language for
 * these credits; the rule itself lives here, where the JVM suite can pin it.
 */
fun localizedPackLine(english: String, bangla: String, language: String): String =
    if (language.startsWith("bn") && bangla.isNotBlank()) bangla else english
