package com.berdachuk.medexpertmatch.integration.usecase;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REQ-016: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 * SCN-007: executable scenario coverage.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UseCase6DocumentSearchE2EIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldSearchDocumentsEndToEnd() throws Exception {
        mockMvc.perform(get("/documents/search")
                        .param("q", "clinical guidelines")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attribute("query", "clinical guidelines"))
                .andExpect(model().attribute("limit", 5))
                .andExpect(model().attributeExists("results"));
    }
}
