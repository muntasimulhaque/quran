package io.github.muntasimulhaque.quran.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseHostsTest {

    @Test
    fun `the project release page and its storage are allowed`() {
        assertTrue(ReleaseHosts.isAllowed("https://github.com/muntasimulhaque/quran/releases/download/x/core.db"))
        assertTrue(ReleaseHosts.isAllowed("https://release-assets.githubusercontent.com/github-production-release-asset/x"))
        assertTrue(ReleaseHosts.isAllowed("https://objects.githubusercontent.com/github-production-release-asset/x"))
    }

    @Test
    fun `any other host is refused`() {
        assertFalse(ReleaseHosts.isAllowed("https://example.org/core.db"))
        assertFalse(ReleaseHosts.isAllowed("https://github.com.evil.example/core.db"))
        assertFalse(ReleaseHosts.isAllowed("http://github.com/core.db"))
        assertFalse(ReleaseHosts.isAllowed("not a url"))
    }
}
