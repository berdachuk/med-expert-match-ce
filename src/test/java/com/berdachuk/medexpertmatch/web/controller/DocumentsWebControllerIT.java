package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REQ-125: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DocumentsWebControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoadDocumentsPage() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attribute("currentPage", "documents"));
    }

    @Test
    void shouldSearchDocuments() throws Exception {
        mockMvc.perform(get("/documents/search")
                        .param("q", "cardiology")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attribute("query", "cardiology"))
                .andExpect(model().attribute("limit", 5))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    void shouldSearchDocumentsWithDefaultLimit() throws Exception {
        mockMvc.perform(get("/documents/search")
                        .param("q", "clinical guidelines"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attribute("query", "clinical guidelines"))
                .andExpect(model().attributeExists("results"));
    }
}
