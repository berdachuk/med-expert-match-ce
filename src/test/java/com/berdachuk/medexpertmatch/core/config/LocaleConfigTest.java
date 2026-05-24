package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class LocaleConfigTest {

    private final LocaleConfig config = new LocaleConfig();

    @Test
    void shouldCreateSessionLocaleResolver() {
        LocaleResolver resolver = config.localeResolver();
        assertNotNull(resolver);
        assertInstanceOf(SessionLocaleResolver.class, resolver);
    }

    @Test
    void shouldCreateLocaleChangeInterceptor() {
        LocaleChangeInterceptor interceptor = config.localeChangeInterceptor();
        assertNotNull(interceptor);
        assertEquals("lang", interceptor.getParamName());
    }
}
