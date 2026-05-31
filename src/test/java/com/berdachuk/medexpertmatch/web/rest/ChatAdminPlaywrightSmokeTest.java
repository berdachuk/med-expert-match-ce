package com.berdachuk.medexpertmatch.web.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional admin and lifecycle browser smoke (M25). Enable with {@code mvn test -Pplaywright -Dplaywright.enabled=true}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "playwright.enabled", matches = "true")
class ChatAdminPlaywrightSmokeTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Admin session tokens and chat lifecycle controls are reachable")
    void adminAndLifecycleSmoke() {
        String baseUrl = "http://localhost:" + port;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            var context = browser.newContext();
            Page page = context.newPage();

            page.navigate(baseUrl + "/admin?user=admin");
            page.waitForSelector("text=Admin Operations");
            assertTrue(page.locator("a:has-text('Session Tokens')").count() > 0);
            assertTrue(page.locator("a:has-text('Chat Export Audit')").count() > 0);
            assertTrue(page.locator("text=Chat Retention").count() > 0);
            assertTrue(page.locator("a:has-text('Open Runbook')").count() > 0);

            page.navigate(baseUrl + "/admin/session-tokens?user=admin");
            page.waitForSelector("text=API Session Tokens");

            page.navigate(baseUrl + "/chat");
            page.waitForSelector("#exportBundleBtn");
            page.waitForSelector("#deleteAllDataBtn");

            browser.close();
        }
    }
}
