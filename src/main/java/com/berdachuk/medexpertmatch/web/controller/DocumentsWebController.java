package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.documents.DocumentSearchApi;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/documents")
public class DocumentsWebController {

    private final DocumentSearchApi documentSearchApi;

    public DocumentsWebController(DocumentSearchApi documentSearchApi) {
        this.documentSearchApi = documentSearchApi;
    }

    @GetMapping
    public String documentsPage(Model model) {
        model.addAttribute("currentPage", "documents");
        return "documents";
    }

    @GetMapping("/search")
    public String searchDocuments(@RequestParam String q,
                                  @RequestParam(defaultValue = "10") int limit,
                                  Model model) {
        model.addAttribute("currentPage", "documents");
        model.addAttribute("query", q);
        model.addAttribute("limit", limit);
        model.addAttribute("results", documentSearchApi.searchChunks(q, limit));
        return "documents";
    }
}
