package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.AgentCardService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A2A agent discovery card at the well-known path (M15 Step 1).
 */
@Hidden
@RestController
public class AgentCardController {

    private final AgentCardService agentCardService;

    public AgentCardController(AgentCardService agentCardService) {
        this.agentCardService = agentCardService;
    }

    @GetMapping(value = "/.well-known/agent-card.json", produces = "application/json")
    public Map<String, Object> agentCard(HttpServletRequest request) {
        String baseUrl = request.getRequestURL().toString()
                .replace("/.well-known/agent-card.json", "");
        return agentCardService.buildAgentCard(baseUrl);
    }
}
