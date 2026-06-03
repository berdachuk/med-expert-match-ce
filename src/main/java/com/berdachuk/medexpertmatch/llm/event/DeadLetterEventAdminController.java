package com.berdachuk.medexpertmatch.llm.event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Admin", description = "Simulated admin APIs (requires X-User-Id: admin)")
@RestController
@RequestMapping("/api/v1/admin")
public class DeadLetterEventAdminController {

    private final EventDeadLetterQueue deadLetterQueue;
    private final EventRetryService eventRetryService;

    public DeadLetterEventAdminController(EventDeadLetterQueue deadLetterQueue, EventRetryService eventRetryService) {
        this.deadLetterQueue = deadLetterQueue;
        this.eventRetryService = eventRetryService;
    }

    @Operation(summary = "List all dead letter events")
    @GetMapping("/dead-letter-events")
    public ResponseEntity<List<DeadLetterEvent>> listDeadLetters() {
        return ResponseEntity.ok(deadLetterQueue.listAll());
    }

    @Operation(summary = "Replay a dead letter event")
    @PostMapping("/dead-letter-events/{id}/replay")
    public ResponseEntity<Map<String, String>> replayDeadLetter(@PathVariable String id) {
        eventRetryService.replayDeadLetter(id);
        return ResponseEntity.ok(Map.of("status", "replayed", "id", id));
    }
}