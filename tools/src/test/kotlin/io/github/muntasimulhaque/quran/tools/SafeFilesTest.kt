package io.github.muntasimulhaque.quran.tools

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The build tools must not let a ZIP entry or a manifest path write outside
 * the folder it names. These are the cases a build machine has to refuse.
 */
class SafeFilesTest {

    private val root = Files.createTempDirectory("safe-files").toFile()

    @After
    fun removeRoot() {
        root.deleteRecursively()
    }

    @Test
    fun aManifestPathStaysUnderTheRoot() {
        assertTrue(SafeFiles.resolveUnder(root, "content/raw/qul/font.zip") != null)
        assertNull(SafeFiles.resolveUnder(root, "../escape.zip"))
        assertNull(SafeFiles.resolveUnder(root, "/etc/passwd"))
        assertNull(SafeFiles.resolveUnder(root, "content/../../escape.zip"))
        assertNull(SafeFiles.resolveUnder(root, "C:/escape.zip"))
    }

    @Test
    fun aZippedEntryStaysUnderTheDestination() {
        assertTrue(SafeFiles.resolveUnder(root, "fonts/UthmanicHafs.ttf") != null)
        assertNull(SafeFiles.resolveUnder(root, "../../app/build.gradle.kts"))
        assertNull(SafeFiles.resolveUnder(root, "fonts/../../escape"))
    }

    @Test
    fun anAudioPathIsOneFolderAndOneFile() {
        assertTrue(SafeFiles.audioFile(root, "husary/001001.mp3") != null)
        assertNull(SafeFiles.audioFile(root, "../../databases/saved.db"))
        assertNull(SafeFiles.audioFile(root, "husary/../001001.mp3"))
        assertNull(SafeFiles.childUnder(root, ".."))
        assertFalse(SafeFiles.under(root, File(root, "../outside")))
    }
}
