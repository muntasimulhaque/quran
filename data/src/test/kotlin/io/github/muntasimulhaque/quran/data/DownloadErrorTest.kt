package io.github.muntasimulhaque.quran.data

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The three answers a failed transfer can give: the network, the phone's
 * storage, or something the app cannot name.
 */
class DownloadErrorTest {

    @Test
    fun aDiskThatIsFullIsThePhonesStorage() {
        assertEquals(
            DownloadError.Storage,
            IOException("No space left on device").downloadError(),
        )
    }

    @Test
    fun aFailedTransferIsTheNetwork() {
        assertEquals(DownloadError.Network, IOException("connection reset").downloadError())
        assertEquals(
            DownloadError.Network,
            IllegalStateException("HTTP 404").downloadError(),
        )
    }

    @Test
    fun anUnnamedFailureIsKeptHonest() {
        assertEquals(
            DownloadError.Other,
            IllegalStateException("checksum mismatch").downloadError(),
        )
    }
}
