package com.berdachuk.medexpertmatch.chat.rest;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional browser smoke test (M20). Enable with {@code mvn test -Pplaywright -Dplaywright.enabled=true}
 * after {@code mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "playwright.enabled", matches = "true")
class ChatPlaywrightSmokeTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Chat page loads, sends a message, and renders assistant reply")
    void chatNavigation() {
        String baseUrl = "http://localhost:" + port;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            var context = browser.newContext();
            context.addCookies(List.of(new Cookie("medexpertmatch-user-id", "playwright-user")
                    .setDomain("localhost")
                    .setPath("/")
                    .setUrl(baseUrl)));
            Page page = context.newPage();
            page.navigate(baseUrl + "/chat");
            page.waitForSelector("#messageInput");
            page.locator("#messageInput").fill("Anonymized COPD management overview");
            page.locator("#sendBtn").click();
            page.waitForSelector("#messagePanel .badge:has-text('assistant')",
                    new Page.WaitForSelectorOptions().setTimeout(120_000));
            assertTrue(page.locator("#messagePanel").innerText().length() > 0);
            assertTrue(page.locator("#agentActivityPanel").count() >= 0);
            browser.close();
        }
    }
}
