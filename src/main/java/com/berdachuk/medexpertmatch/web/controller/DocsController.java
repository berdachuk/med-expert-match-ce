package com.berdachuk.medexpertmatch.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Redirects /docs and /docs/... to the embedded documentation (same host and port).
 * Static content is served from classpath:static/docs/ (MkDocs build in Docker).
 */
@Controller
public class DocsController {

    @GetMapping("/docs")
    public RedirectView docsRedirect() {
        return new RedirectView("/docs/index.html", true);
    }

    @GetMapping("/docs/")
    public RedirectView docsSlashRedirect() {
        return new RedirectView("/docs/index.html", true);
    }

    @GetMapping("/docs/{page}/")
    public RedirectView docsPageWithSlash(@PathVariable String page) {
        return new RedirectView("/docs/" + page + "/index.html", true);
    }

    @GetMapping("/docs/{page:[^.]+}")
    public RedirectView docsPageNoSlash(@PathVariable String page) {
        return new RedirectView("/docs/" + page + "/index.html", true);
    }
}
