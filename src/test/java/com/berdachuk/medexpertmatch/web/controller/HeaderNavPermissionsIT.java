package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract: the top navigation (header fragment) must show the admin-only items
 * (Synthetic Data, Graph) only to admin sessions. Regular users see
 * only the public items (Home, Find Specialist, Case Analysis, Queue, Analytics,
 * Routing, AI Chat).
 */
/**
 * REQ-125: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HeaderNavPermissionsIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String[] PUBLIC_LABELS = {
            "Find Specialist", "Case Analysis", "Queue", "Analytics", "Routing", "AI Chat"
    };
    private static final String[] ADMIN_LABELS = {
            "Synthetic Data", "Graph"
    };

    @Test
    void regularUserSeesOnlyPublicNavItems() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AI Chat")))
                .andExpect(content().string(containsString("Find Specialist")))
                .andExpect(content().string(not(containsString("nav.synthetic_data"))))
                .andExpect(content().string(not(containsString("nav.graph"))))
                .andExpect(content().string(not(containsString("nav.documents"))));
    }

    @Test
    void adminUserSeesAdminNavItems() throws Exception {
        mockMvc.perform(get("/").param("user", AdminAccessGuard.ADMIN_USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AI Chat")))
                .andExpect(content().string(containsString("Synthetic Data")))
                .andExpect(content().string(containsString("Graph")))
                .andExpect(content().string(not(containsString("Documents"))));
    }
}
