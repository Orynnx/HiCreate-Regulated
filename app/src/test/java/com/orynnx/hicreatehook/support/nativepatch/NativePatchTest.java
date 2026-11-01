package com.orynnx.hicreatehook.support.nativepatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class NativePatchTest {

    @Test
    public void patchTableIsConsistent() {
        assertEquals(37, NativePatch.OFFSETS.length);
        assertEquals(NativePatch.OFFSETS.length, NativePatch.OLD_BYTES.length);
        assertEquals(NativePatch.OFFSETS.length, NativePatch.NEW_BYTES.length);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < NativePatch.OFFSETS.length; i++) {
            long offset = NativePatch.OFFSETS[i];
            assertTrue("offset " + i, offset > 0);
            assertTrue("duplicate offset " + offset, seen.add(offset));
            assertEquals("old/new size " + i,
                    NativePatch.OLD_BYTES[i].length(), NativePatch.NEW_BYTES[i].length());
            assertFalse("patch must change bytes " + i,
                    NativePatch.OLD_BYTES[i].equals(NativePatch.NEW_BYTES[i]));
        }
    }

    @Test
    public void hashesArePinned() {
        assertEquals(
                "3CE153F4901CD40B2B3D164B0D345997F688E9AB329B5498CC3BC1D16A285E48",
                NativePatch.ORIGINAL_SHA256);
        assertEquals(
                "B505EEA91BB28C171CCA626A544E9EE1010DC40900B0FC7EBAC76B6161934725",
                NativePatch.PATCHED_SHA256);
        assertNotEquals(NativePatch.ORIGINAL_SHA256, NativePatch.PATCHED_SHA256);
    }

    @Test
    public void nativeLibraryNamesAreExact() {
        assertEquals("libnativelib.so", NativePatch.NATIVELIB);
        assertTrue(NativePatch.LIBRARIES.length > 0);
        for (String library : NativePatch.LIBRARIES) {
            assertTrue(library, library.startsWith("lib") && library.endsWith(".so"));
        }
    }
}
