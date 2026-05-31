package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminUserCookieFilterIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("?user=admin sets medexpertmatch-user-id cookie for admin REST access")
    void setsAdminCookie() throws Exception {
        var result = mockMvc.perform(get("/admin/session-tokens").param("user", "admin"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("medexpertmatch-user-id", AdminAccessGuard.ADMIN_USER_ID))
                .andReturn();

        mockMvc.perform(get("/api/v1/admin/session-tokens")
                        .cookie(result.getResponse().getCookie("medexpertmatch-user-id")))
                .andExpect(status().isOk());
    }
}
