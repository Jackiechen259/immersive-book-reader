package com.immersive.reader.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun leadingVPrefixIsIgnored() {
        assertEquals(0, VersionComparator.compare("v1.2.3", "1.2.3"))
        assertEquals(0, VersionComparator.compare("V0.1.0", "0.1.0"))
    }

    @Test
    fun equalVersionsAreNotNewer() {
        assertFalse(VersionComparator.isNewer("0.1.0", "0.1.0"))
        assertFalse(VersionComparator.isNewer("v0.1.0", "0.1.0"))
    }

    @Test
    fun remotePatchBumpIsNewer() {
        assertTrue(VersionComparator.isNewer("0.1.1", "0.1.0"))
        assertTrue(VersionComparator.isNewer("v0.2.0", "0.1.9"))
    }

    @Test
    fun olderRemoteIsNotNewer() {
        assertFalse(VersionComparator.isNewer("0.1.0", "0.2.0"))
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun missingPatchEqualsZero() {
        assertEquals(0, VersionComparator.compare("1.2", "1.2.0"))
        assertTrue(VersionComparator.isNewer("1.2.1", "1.2"))
    }

    @Test
    fun preReleaseIsOlderThanReleaseWithSameCore() {
        assertTrue(VersionComparator.isNewer("1.2.0", "1.2.0-beta"))
        assertFalse(VersionComparator.isNewer("1.2.0-beta", "1.2.0"))
        assertTrue(VersionComparator.isNewer("1.2.1-beta", "1.2.0"))
    }
}
