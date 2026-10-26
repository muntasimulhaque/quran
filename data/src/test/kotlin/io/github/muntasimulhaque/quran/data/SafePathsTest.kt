package io.github.muntasimulhaque.quran.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * The one gate between content names and the disk. A pack or a reciter that
 * names `..` is refused here before any file work starts.
 */
class SafePathsTest {

    private val root = Files.createTempDirectory("safe-paths").toFile()

    @After
    fun removeRoot() {
        root.deleteRecursively()
    }

    @Test
    fun catalogIdsAreLowercaseWords() {
        assertTrue(SafePaths.validId("translation-saheeh-en"))
        assertTrue(SafePaths.validId("reciter-minshawy"))
        assertFalse(SafePaths.validId("../translation"))
        assertFalse(SafePaths.validId("Translation"))
        assertFalse(SafePaths.validId("a/b"))
    }

    @Test
    fun aFolderIsOneSegment() {
        assertTrue(SafePaths.validFolder("minshawyMurattal"))
        assertFalse(SafePaths.validFolder(".."))
        assertFalse(SafePaths.validFolder("a/b"))
        assertFalse(SafePaths.validFolder("a..b"))
        assertNull(SafePaths.childUnder(root, "../escape"))
        assertNull(SafePaths.childUnder(root, ".."))
    }

    @Test
    fun anAudioPathIsOneFolderAndOneAyahFile() {
        assertTrue(SafePaths.validAudioPath("husary/001001.mp3"))
        assertFalse(SafePaths.validAudioPath("../../databases/saved.db"))
        assertFalse(SafePaths.validAudioPath("husary/../001001.mp3"))
        assertFalse(SafePaths.validAudioPath("/data/user/0/package/databases/saved.db"))
        assertEquals("husary", SafePaths.folderOf("husary/001001.mp3"))
        assertNull(SafePaths.folderOf("../../databases/saved.db"))
    }

    @Test
    fun anAudioFileNeverLeavesItsFolder() {
        val recitations = File(root, "recitations").apply { mkdirs() }
        val file = SafePaths.audioFile(recitations, "husary/001001.mp3")
        assertTrue(file != null && file.path.startsWith(recitations.canonicalPath))
        assertNull(SafePaths.audioFile(recitations, "../../databases/saved.db"))
    }

    @Test
    fun aChecksumIsSixtyFourHexCharacters() {
        assertTrue(SafePaths.validHash("a".repeat(64)))
        assertTrue(SafePaths.validHash("A".repeat(64)))
        assertFalse(SafePaths.validHash(""))
        assertFalse(SafePaths.validHash("abc"))
        assertFalse(SafePaths.validHash("g".repeat(64)))
    }

    @Test
    fun invalidSqlIdentifierCannotLeaveTheQuotes() {
        assertEquals("\"core\"", SafePaths.sqlIdentifier("core"))
        assertEquals("\"__invalid_pack__\"", SafePaths.sqlIdentifier("core\"; DROP TABLE ayah; --"))
    }
}
