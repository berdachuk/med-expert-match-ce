package com.berdachuk.medexpertmatch.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevDocsRedirectControllerTest {

    private DevDocsRedirectController controller;

    @BeforeEach
    void setUp() {
        controller = new DevDocsRedirectController("http://localhost:8000");
    }

    @Test
    void shouldRedirectDocsRootToMkDocsDevServer() {
        RedirectView view = controller.redirect(new MockHttpServletRequest("GET", "/docs"));

        assertEquals("http://localhost:8000/", view.getUrl());
    }

    @Test
    void shouldRedirectDocsIndexHtmlToMkDocsRoot() {
        RedirectView view = controller.redirect(new MockHttpServletRequest("GET", "/docs/index.html"));

        assertEquals("http://localhost:8000/", view.getUrl());
    }

    @Test
    void shouldRedirectNestedDocsPathPreservingSuffix() {
        RedirectView view = controller.redirect(
                new MockHttpServletRequest("GET", "/docs/DEVELOPMENT_GUIDE/"));

        assertEquals("http://localhost:8000/DEVELOPMENT_GUIDE/", view.getUrl());
    }

    @Test
    void shouldStripTrailingSlashFromConfiguredDevUrl() {
        DevDocsRedirectController trailing = new DevDocsRedirectController("http://localhost:8000/");
        RedirectView view = trailing.redirect(new MockHttpServletRequest("GET", "/docs/"));

        assertEquals("http://localhost:8000/", view.getUrl());
    }
}
