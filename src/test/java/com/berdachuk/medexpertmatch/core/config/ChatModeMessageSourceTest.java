package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ChatModeMessageSourceTest extends BaseIntegrationTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    void shouldResolveChatModeLabelInEnglish() {
        assertLabel("chat.mode.expert", Locale.ENGLISH, "Expert match");
        assertLabel("chat.mode.cost.expert", Locale.ENGLISH, "token budget");
    }

    private void assertLabel(String code, Locale locale, String expectedFragment) {
        String message = messageSource.getMessage(code, null, locale);
        assertFalse(message.startsWith("??"), "Unresolved message for " + code + ": " + message);
        assertTrue(message.contains(expectedFragment), message);
    }
}