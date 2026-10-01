package io.github.muntasimulhaque.quran.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * The second look that keeps a pack current: a file whose bytes no longer
 * match the catalog is found and named, and a file that does match is left
 * alone. This is the detection behind the quiet refresh; the refresh
 * itself is a network path and is not tested here.
 */
@RunWith(AndroidJUnit4::class)
class PackVerifierTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val id = "translation-saheeh-en"
    private val store = PackStore(context)

    @After
    fun removeTheTestPack() {
        store.remove(id)
    }

    @Test
    fun aPackBehindTheCatalogIsFlaggedForRefresh() = runBlocking {
        write("the bytes of the old release")
        val stale = PackVerifier(context).needsRefresh(listOf(pack(hashOf("the bytes of the new release"))))
        assertEquals(1, stale.size)
        assertEquals(id, stale.first().id)
    }

    @Test
    fun aCurrentPackIsNotFlagged() = runBlocking {
        write("the bytes of the current release")
        val stale = PackVerifier(context).needsRefresh(listOf(pack(hashOf("the bytes of the current release"))))
        assertTrue(stale.isEmpty())
    }

    @Test
    fun aPackTheDeviceDoesNotHaveIsNotFlagged() = runBlocking {
        val stale = PackVerifier(context).needsRefresh(listOf(pack(hashOf("anything"), installed = false)))
        assertTrue(stale.isEmpty())
    }

    @Test
    fun theShippedPackIsNeverFlagged() = runBlocking {
        val core = pack(hashOf("anything")).copy(id = PackCatalog.CORE_ID, shipped = true)
        val stale = PackVerifier(context).needsRefresh(listOf(core))
        assertTrue(stale.isEmpty())
    }

    private fun pack(hash: String, installed: Boolean = true) = ContentPack(
        id = id,
        type = PackType.Translation,
        name = "Saheeh International",
        language = "en",
        credit = "",
        license = "",
        version = "1",
        shipped = false,
        ayahs = 6236,
        bytes = 0,
        sha256 = hash,
        installed = installed,
    )

    private fun write(text: String) {
        val file = store.fileFor(id)
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun hashOf(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
