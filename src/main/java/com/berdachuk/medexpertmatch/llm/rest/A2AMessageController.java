package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "A2A Bridge", description = "A2A sendMessage endpoint (JSON-RPC at /a2a/v1/jsonrpc)")
@RestController
@RequestMapping("/a2a/v1")
public class A2AMessageController {

    private final A2AMessageService a2aMessageService;

    public A2AMessageController(A2AMessageService a2aMessageService) {
        this.a2aMessageService = a2aMessageService;
    }

    @Operation(summary = "Send a message to a MedExpertMatch skill (PHI-safe stub)")
    @PostMapping("/sendMessage")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> body) {
        return a2aMessageService.sendMessage(body);
    }
}
