package io.github.muntasimulhaque.quran.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The catalog the view model keeps must carry the installed flags.
 *
 * It did not once: `openLibrary` loaded the catalog unmarked and only the
 * thrown-away `ContentDatabase` copy was marked, so `installPack` believed a
 * pack already on the device was missing. Turning word by word on, or changing
 * the interface language with it on, then re-fetched a pack that was already
 * there and reopened the library, which closed the old database under any
 * reader still using it. The reader met that as the app closing.
 *
 * This runs as an instrumented test because `org.json` is not mocked on the
 * unit test classpath, and `PackCatalog.parse` reads the catalog with it.
 */
@RunWith(AndroidJUnit4::class)
class PackCatalogTest {

    private val json = """
        {"packs":[
          {"id":"core","type":"script","name":"Quran","language":"ar","shipped":true,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"},
          {"id":"words-en","type":"words","name":"Word by word","language":"en","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"},
          {"id":"translation-saheeh-en","type":"translation","name":"Saheeh International","language":"en","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}
        ]}
    """.trimIndent()

    @Test
    fun aLoadedPackIsNotInstalledUntilTheDeviceSaysSo() {
        val catalog = PackCatalog.parse(json)
        assertFalse(catalog.get("words-en")!!.installed)
    }

    @Test
    fun markingTheDeviceLeavesPresentPacksInstalled() {
        val marked = PackCatalog.parse(json).withInstalled(setOf("words-en"))
        assertTrue("a pack on the device must never read as missing", marked.get("words-en")!!.installed)
        assertFalse(marked.get("translation-saheeh-en")!!.installed)
    }

    @Test
    fun aPackWithoutAChecksumIsNotInTheCatalog() {
        val catalog = PackCatalog.parse(
            """{"packs":[{"id":"words-en","type":"words","name":"Word by word","language":"en"}]}""",
        )
        assertTrue("a missing checksum is a refusal", catalog.get("words-en") == null)
    }

    @Test
    fun aPackIdThatCouldEscapeItsFolderIsNotInTheCatalog() {
        val catalog = PackCatalog.parse(
            """{"packs":[{"id":"../words-en","type":"words","name":"Word by word","language":"en","sha256":"${"a".repeat(64)}"}]}""",
        )
        assertTrue("an unsafe id is a refusal", catalog.all().isEmpty())
    }

    @Test
    fun theCorePackIsAlwaysCarried() {
        val marked = PackCatalog.parse(json).withInstalled(emptySet())
        assertTrue(marked.get(PackCatalog.CORE_ID) != null)
    }

    /**
     * A translation the build carries is a shipped pack like the core, not a
     * download the reader has to make before the first page. The catalog
     * keeps it, and the store can place it from the app's own assets.
     */
    @Test
    fun aShippedTranslationIsCarried() {
        val catalog = PackCatalog.parse(
            """{"packs":[{"id":"translation-saheeh-en","type":"translation","name":"Saheeh International","language":"en","shipped":true,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","bytes":1}]}""",
        )
        assertTrue(catalog.get("translation-saheeh-en")?.shipped == true)
    }

    /**
     * A pack's Bangla credit and license, when the catalog carries them, are
     * read beside the English pair: the About page shows them in a Bangla
     * interface, and an older catalog with no Bangla line leaves them empty so
     * the page falls back to English rather than to nothing
     * (owner decision, 8.8).
     */
    @Test
    fun aPacksBanglaCreditAndLicenseAreRead() {
        val parsed = PackCatalog.parse(
            """
            {"packs":[{"id":"core","type":"script","name":"Quran text","language":"ar","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "credit":"English credit","license":"English license",
              "creditBn":"\u09ac\u09be\u0982\u09b2\u09be \u0995\u09c3\u09a4\u09bf\u09a4\u09cd\u09ac",
              "licenseBn":"\u09ac\u09be\u0982\u09b2\u09be \u09b2\u09be\u0987\u09b8\u09c7\u09a8\u09cd\u09b8"}]}
            """.trimIndent(),
        )
        val core = parsed.get(PackCatalog.CORE_ID)!!
        assertEquals("English credit", core.credit)
        assertEquals("\u09ac\u09be\u0982\u09b2\u09be \u0995\u09c3\u09a4\u09bf\u09a4\u09cd\u09ac", core.creditBn)
        assertEquals("\u09ac\u09be\u0982\u09b2\u09be \u09b2\u09be\u0987\u09b8\u09c7\u09a8\u09cd\u09b8", core.licenseBn)
    }

    @Test
    fun aCatalogWithoutBanglaLinesLeavesThemEmpty() {
        val core = PackCatalog.parse(json).get(PackCatalog.CORE_ID)!!
        assertEquals("", core.creditBn)
        assertEquals("", core.licenseBn)
    }

    /**
     * A pack's Bangla name, when the catalog carries it, is read beside the
     * English one: the interface draws it and no table in the app names a
     * pack (owner decision, 20.2).
     */
    @Test
    fun aPacksBanglaNameIsRead() {
        val parsed = PackCatalog.parse(
            """
            {"packs":[{"id":"reciter-minshawi","type":"recitation","name":"Minshawi","language":"ar",
              "sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
              "nameBn":"\u09ae\u09bf\u09a8\u09b6\u09be\u09ac\u09bf"}]}
            """.trimIndent(),
        )
        val pack = parsed.get("reciter-minshawi")!!
        assertEquals("Minshawi", pack.name)
        assertEquals("\u09ae\u09bf\u09a8\u09b6\u09be\u09ac\u09bf", pack.nameBn)
        assertEquals("\u09ae\u09bf\u09a8\u09b6\u09be\u09ac\u09bf", pack.displayName("bn"))
    }

    /** A pack whose entry carries no Bangla name is named in English, not blank. */
    @Test
    fun aCatalogWithoutABanglaNameNamesThePackInEnglish() {
        val core = PackCatalog.parse(json).get(PackCatalog.CORE_ID)!!
        assertEquals("", core.nameBn)
        assertEquals("Quran", core.displayName("bn"))
    }

    /** A blank Bangla name is not a name: the English one stands for it. */
    @Test
    fun aBlankBanglaNameFallsBackToTheEnglishOne() {
        val parsed = PackCatalog.parse(
            """{"packs":[{"id":"words-en","type":"words","name":"Word meanings","nameBn":"  ","language":"en","sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]}""",
        )
        assertEquals("Word meanings", parsed.get("words-en")!!.displayName("bn"))
    }
}
