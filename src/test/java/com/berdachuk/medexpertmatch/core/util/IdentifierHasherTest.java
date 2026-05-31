package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class IdentifierHasherTest {

    @Test
    @DisplayName("sha256Hex produces stable non-reversible digests")
    void stableDigest() {
        String first = IdentifierHasher.sha256Hex("user-123");
        String second = IdentifierHasher.sha256Hex("user-123");
        assertEquals(first, second);
        assertFalse(first.contains("user"));
        assertNotEquals(IdentifierHasher.sha256Hex("user-456"), first);
    }

    @Test
    @DisplayName("Blank values hash to empty string")
    void blankSafe() {
        assertEquals("", IdentifierHasher.sha256Hex(null));
        assertEquals("", IdentifierHasher.sha256Hex("   "));
    }
}
