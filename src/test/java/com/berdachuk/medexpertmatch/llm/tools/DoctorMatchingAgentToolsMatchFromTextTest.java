package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseIntakeWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorMatchingAgentToolsMatchFromTextTest {

    private final MedicalCaseRepository medicalCaseRepository = mock(MedicalCaseRepository.class);
    private final DoctorRepository doctorRepository = mock(DoctorRepository.class);
    private final MatchingService matchingService = mock(MatchingService.class);
    private final SemanticGraphRetrievalService semanticGraphRetrievalService = mock(SemanticGraphRetrievalService.class);
    private final LogStreamService logStreamService = mock(LogStreamService.class);
    private final MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService =
            mock(MedicalAgentCaseIntakeWorkflowService.class);

    private final DoctorMatchingAgentTools tools = new DoctorMatchingAgentTools(
            medicalCaseRepository,
            doctorRepository,
            matchingService,
            semanticGraphRetrievalService,
            logStreamService,
            caseIntakeWorkflowService);

    @AfterEach
    void clearContext() {
        OrchestrationContextHolder.clear();
    }

    @Test
    @DisplayName("match_doctors_from_text delegates to case intake workflow")
    void matchDoctorsFromTextDelegates() {
        OrchestrationContextHolder.setSessionId("user-chat-1");
        when(caseIntakeWorkflowService.matchFromText(eq("90-year-old with chest pain"), any()))
                .thenReturn(new MedicalAgentService.AgentResponse(
                        "Top match: Cardiology",
                        Map.of("caseId", "6a1c68963a08e800010de68e")));

        String result = tools.match_doctors_from_text(
                "90-year-old with chest pain", 90, "SECOND_OPINION", "MEDIUM", 5);

        assertTrue(result.contains("Top match: Cardiology"));
        assertTrue(result.contains("6a1c68963a08e800010de68e"));
        verify(caseIntakeWorkflowService).matchFromText(eq("90-year-old with chest pain"), any());
    }

    @Test
    @DisplayName("match_doctors_from_text rejects blank case text")
    void rejectsBlankText() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> tools.match_doctors_from_text("  ", null, null, null, null));
    }
}
