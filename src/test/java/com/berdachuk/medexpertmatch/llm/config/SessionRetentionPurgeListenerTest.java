package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.SessionService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionRetentionPurgeListenerTest {

    @Test
    @DisplayName("Deletes Spring AI session using userId-chatId convention")
    void deletesSessionOnChatPurge() {
        SessionService sessionService = mock(SessionService.class);
        SessionRetentionPurgeListener listener = new SessionRetentionPurgeListener(sessionService);

        listener.onChatPurged("user-1", "chat-9");

        verify(sessionService).delete("user-1-chat-9");
    }
}
