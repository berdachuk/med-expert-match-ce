package com.berdachuk.medexpertmatch.chat.service.impl;

import com.berdachuk.medexpertmatch.chat.domain.Chat;
import com.berdachuk.medexpertmatch.chat.domain.ChatMessage;
import com.berdachuk.medexpertmatch.chat.service.ChatExportAuditor;
import com.berdachuk.medexpertmatch.chat.service.ChatService;
import com.berdachuk.medexpertmatch.core.service.HarnessPlanExportQuery;
import com.berdachuk.medexpertmatch.llm.harness.HarnessPlanExportQueryImpl;
import com.berdachuk.medexpertmatch.llm.harness.AgentPlanArtefact;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import com.berdachuk.medexpertmatch.llm.harness.impl.InMemoryAgentPlanArtefactStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatDataLifecycleServicePlanExportTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatExportAuditor chatExportAuditor;

    private InMemoryAgentPlanArtefactStore planStore;
    private HarnessPlanExportQuery planExportQuery;
    private ChatDataLifecycleServiceImpl lifecycleService;

    @BeforeEach
    void setUp() {
        planStore = new InMemoryAgentPlanArtefactStore();
        planExportQuery = new HarnessPlanExportQueryImpl(planStore);
        lifecycleService = new ChatDataLifecycleServiceImpl(
                null,
                null,
                chatService,
                chatExportAuditor,
                planExportQuery);
    }

    @Test
    @DisplayName("export bundle includes persisted harness plan keyed by userId-chatId session")
    void exportBundleIncludesPlan() {
        String userId = "user-a";
        String chatId = "chat-b";
        String sessionId = userId + "-" + chatId;
        planStore.save(new AgentPlanArtefact(
                sessionId,
                HarnessWorkflowType.DOCTOR_MATCH,
                "6a1c68963a08e800010de68e",
                List.of("analyze", "match"),
                List.of("min 1 match"),
                Instant.parse("2026-05-31T12:00:00Z")));

        Chat chat = new Chat(chatId, userId, "Plan chat", "auto", false,
                Instant.now(), Instant.now(), Instant.now(), 1);
        when(chatService.listChats(userId)).thenReturn(List.of(chat));
        when(chatService.getHistory(chatId, userId, 10_000, 0)).thenReturn(List.of());
        when(chatExportAuditor.recordExportBundle(userId, 1, 0)).thenReturn("audit-hash");

        Map<String, Object> bundle = lifecycleService.exportUserBundle(userId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chats = (List<Map<String, Object>>) bundle.get("chats");
        @SuppressWarnings("unchecked")
        Map<String, Object> plan = (Map<String, Object>) chats.getFirst().get("plan");

        assertEquals("DOCTOR_MATCH", plan.get("workflowType"));
        assertEquals("6a1c68963a08e800010de68e", plan.get("caseId"));
        assertEquals(2, ((List<?>) plan.get("steps")).size());
    }
}
