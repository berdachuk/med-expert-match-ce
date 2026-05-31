package com.berdachuk.medexpertmatch.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApiKeyMaskerTest {

    @Test
    void masksLongKeys() {
        String masked = ApiKeyMasker.prefix("abcdefgh12345678");
        assertEquals("abcdefgh…", masked);
        assertFalse(masked.contains("12345678"));
    }
}
