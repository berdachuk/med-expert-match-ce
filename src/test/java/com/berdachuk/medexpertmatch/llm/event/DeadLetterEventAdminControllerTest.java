package com.berdachuk.medexpertmatch.llm.event;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeadLetterEventAdminControllerTest {

    @Test
    @DisplayName("listDeadLetters requires admin access")
    void listRequiresAdmin() {
        var guard = mock(AdminAccessGuard.class);
        var controller = new DeadLetterEventAdminController(
                mock(EventDeadLetterQueue.class), mock(EventRetryService.class), guard);
        controller.listDeadLetters();
        verify(guard).requireAdmin();
    }

    @Test
    @DisplayName("replayDeadLetter requires admin access")
    void replayRequiresAdmin() {
        var guard = mock(AdminAccessGuard.class);
        var retry = mock(EventRetryService.class);
        var controller = new DeadLetterEventAdminController(
                mock(EventDeadLetterQueue.class), retry, guard);
        controller.replayDeadLetter("dlq-1");
        verify(guard).requireAdmin();
    }
}