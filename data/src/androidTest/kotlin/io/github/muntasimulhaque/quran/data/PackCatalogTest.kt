package io.github.muntasimulhaque.quran.data

import androidx.test.ext.junit.runners.AndroidJUnit4
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
          {"id":"core","type":"script","name":"Quran","language":"ar","shipped":true},
          {"id":"words-en","type":"words","name":"Word by word","language":"en"},
          {"id":"translation-saheeh-en","type":"translation","name":"Saheeh International","language":"en"}
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
    fun theCorePackIsAlwaysCarried() {
        val marked = PackCatalog.parse(json).withInstalled(emptySet())
        assertTrue(marked.get(PackCatalog.CORE_ID) != null)
    }
}
