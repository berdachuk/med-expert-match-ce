package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.web.config.EmbeddedDocsUnavailableCondition;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * When MkDocs is not embedded in the JAR (local dev), redirect /docs to the MkDocs dev server.
 */
@Controller
@Conditional(EmbeddedDocsUnavailableCondition.class)
public class DevDocsRedirectController {

    private final String devDocsUrl;

    public DevDocsRedirectController(
            @Value("${medexpertmatch.docs.dev-url:http://localhost:8000}") String devDocsUrl) {
        String trimmed = devDocsUrl == null ? "" : devDocsUrl.trim();
        this.devDocsUrl = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    @GetMapping({"/docs", "/docs/", "/docs/**"})
    public RedirectView redirect(HttpServletRequest request) {
        String path = request.getRequestURI();
        String suffix = path.length() > 5 ? path.substring(5) : "";
        if (suffix.isEmpty() || "/".equals(suffix) || "/index.html".equals(suffix)) {
            return new RedirectView(devDocsUrl + "/", false);
        }
        return new RedirectView(devDocsUrl + suffix, false);
    }
}
