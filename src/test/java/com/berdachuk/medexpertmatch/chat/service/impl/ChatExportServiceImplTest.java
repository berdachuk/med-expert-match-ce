package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatExportAuditor;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.service.HarnessPlanExportQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatExportServiceImplTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatExportAuditor chatExportAuditor;

    @Mock
    private HarnessPlanExportQuery harnessPlanExportQuery;

    private ChatExportServiceImpl exportService;

    @BeforeEach
    void setUp() {
        exportService = new ChatExportServiceImpl(chatService, chatExportAuditor, harnessPlanExportQuery);
    }

    @Test
    @DisplayName("Export redacts PHI from message content and records audit")
    void redactsPhi() {
        Chat chat = new Chat("c1", "u1", "Test", "auto", false,
                Instant.now(), Instant.now(), Instant.now(), 1);
        ChatMessage message = new ChatMessage("m1", "c1", "user", "Patient name: John Smith", 1, null, Instant.now());
        when(chatService.requireOwnedChat("c1", "u1")).thenReturn(chat);
        when(chatService.getHistory("c1", "u1", 10_000, 0)).thenReturn(List.of(message));

        Map<String, Object> export = exportService.exportTranscript("c1", "u1");
        assertEquals(true, export.get("phiRedacted"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) export.get("messages");
        assertTrue(messages.getFirst().get("content").toString().contains("[REDACTED]"));
        verify(chatExportAuditor).recordExport("u1", "c1", 1);
    }
}
