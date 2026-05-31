package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.A2AMessageService;
import com.berdachuk.medexpertmatch.llm.service.A2aSkillRegistryService;
import com.berdachuk.medexpertmatch.llm.domain.A2aSkillDescriptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Tag(name = "A2A Bridge", description = "JSON-RPC 2.0 A2A message endpoint")
@RestController
@RequestMapping("/a2a/v1")
public class A2aJsonRpcController {

    private final A2AMessageService a2aMessageService;
    private final A2aSkillRegistryService a2aSkillRegistryService;

    public A2aJsonRpcController(A2AMessageService a2aMessageService,
                                  A2aSkillRegistryService a2aSkillRegistryService) {
        this.a2aMessageService = a2aMessageService;
        this.a2aSkillRegistryService = a2aSkillRegistryService;
    }

    @Operation(summary = "List supported A2A bridge skills with input schema hints")
    @GetMapping("/skills")
    public List<A2aSkillDescriptor> listSkills() {
        return a2aSkillRegistryService.listSkills();
    }

    @Operation(summary = "JSON-RPC 2.0 sendMessage (PHI-safe, routes to domain skills)")
    @PostMapping("/jsonrpc")
    public Map<String, Object> jsonRpc(@RequestBody Map<String, Object> body) {
        return a2aMessageService.handleJsonRpc(body);
    }

    @Operation(summary = "Stream skill result with chat-compatible SSE token envelope")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        return a2aMessageService.streamMessage(body);
    }
}
