package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
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
    private final AdminAccessGuard adminAccessGuard;

    public DeadLetterEventAdminController(EventDeadLetterQueue deadLetterQueue, EventRetryService eventRetryService,
                                          AdminAccessGuard adminAccessGuard) {
        this.deadLetterQueue = deadLetterQueue;
        this.eventRetryService = eventRetryService;
        this.adminAccessGuard = adminAccessGuard;
    }

    @Operation(summary = "List all dead letter events")
    @GetMapping("/dead-letter-events")
    public ResponseEntity<List<DeadLetterEvent>> listDeadLetters() {
        adminAccessGuard.requireAdmin();
        return ResponseEntity.ok(deadLetterQueue.listAll());
    }

    @Operation(summary = "Replay a dead letter event")
    @PostMapping("/dead-letter-events/{id}/replay")
    public ResponseEntity<Map<String, String>> replayDeadLetter(@PathVariable String id) {
        adminAccessGuard.requireAdmin();
        eventRetryService.replayDeadLetter(id);
        return ResponseEntity.ok(Map.of("status", "replayed", "id", id));
    }
}