package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.service.RerankingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class RerankingServiceImpl implements RerankingService {

    private final ChatClient rerankingChatClient;
    private final MedicalCaseRepository medicalCaseRepository;
    private final DoctorRepository doctorRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public RerankingServiceImpl(
            @Nullable @Qualifier("rerankingChatModel") ChatModel rerankingChatModel,
            MedicalCaseRepository medicalCaseRepository,
            DoctorRepository doctorRepository,
            ObjectMapper objectMapper,
            @Value("${medexpertmatch.retrieval.reranking.enabled:false}") boolean enabled) {
        this.rerankingChatClient = rerankingChatModel != null ? ChatClient.builder(rerankingChatModel).build() : null;
        this.medicalCaseRepository = medicalCaseRepository;
        this.doctorRepository = doctorRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    public List<DoctorMatch> rerank(String caseId, List<DoctorMatch> candidates, int topK) {
        if (!enabled || candidates == null || candidates.isEmpty() || rerankingChatClient == null) {
            return candidates;
        }
        if (candidates.size() <= topK) {
            return candidates;
        }

        try {
            Optional<MedicalCase> medicalCaseOpt = medicalCaseRepository.findById(caseId);
            if (medicalCaseOpt.isEmpty()) {
                log.warn("Case {} not found, skipping re-ranking", caseId);
                return candidates;
            }
            MedicalCase medicalCase = medicalCaseOpt.get();

            List<DoctorMatch> topCandidates = candidates.stream()
                    .sorted(Comparator.comparing(DoctorMatch::matchScore).reversed())
                    .limit(Math.min(candidates.size(), 50))
                    .toList();

            StringBuilder candidateList = new StringBuilder();
            for (int i = 0; i < topCandidates.size(); i++) {
                DoctorMatch match = topCandidates.get(i);
                Doctor doctor = match.doctor();
                candidateList.append(String.format("%d. Dr. %s (ID: %s, Score: %.1f, Specialty: %s)%n",
                        i + 1, doctor.name(), doctor.id(), match.matchScore(),
                        doctor.specialties() != null ? String.join(", ", doctor.specialties()) : "N/A"));
            }

            String promptText = String.format(
                    """
                    You are a medical expert ranking system. Re-rank the following doctors based on their suitability for this case.

                    Case details:
                    - Chief complaint: %s
                    - Symptoms: %s
                    - Diagnosis: %s
                    - ICD-10 Codes: %s
                    - Required specialty: %s
                    - Urgency: %s

                    Current top candidates (re-rank these, return indices in order of best fit):

                    %s

                    Return a JSON array of indices (0-based) representing the new ranking order. Example: [3, 0, 5, 1, 2, 4]
                    Return ONLY the JSON array, no other text.
                    """,
                    nullToEmpty(medicalCase.chiefComplaint()),
                    nullToEmpty(medicalCase.symptoms()),
                    nullToEmpty(medicalCase.currentDiagnosis()),
                    medicalCase.icd10Codes() != null ? String.join(", ", medicalCase.icd10Codes()) : "N/A",
                    nullToEmpty(medicalCase.requiredSpecialty()),
                    medicalCase.urgencyLevel() != null ? medicalCase.urgencyLevel().name() : "N/A",
                    candidateList.toString());

            String response = rerankingChatClient.prompt().user(promptText).call().content();
            if (response == null || response.isBlank()) {
                log.warn("Empty reranking response, returning original candidates");
                return candidates;
            }

            String jsonText = response.trim();
            if (jsonText.contains("```")) {
                int start = jsonText.indexOf("[");
                int end = jsonText.lastIndexOf("]");
                if (start >= 0 && end > start) {
                    jsonText = jsonText.substring(start, end + 1);
                }
            }

            List<Integer> ranking = objectMapper.readValue(jsonText, new TypeReference<List<Integer>>() {});
            if (ranking == null || ranking.isEmpty()) {
                return candidates;
            }

            List<DoctorMatch> reranked = new ArrayList<>();
            for (int idx : ranking) {
                if (idx >= 0 && idx < topCandidates.size()) {
                    reranked.add(topCandidates.get(idx));
                }
            }

            for (DoctorMatch match : topCandidates) {
                if (!reranked.contains(match)) {
                    reranked.add(match);
                }
            }

            log.debug("Re-ranked {} candidates for case {}", reranked.size(), caseId);
            return reranked;
        } catch (Exception e) {
            log.warn("Re-ranking failed for case {}: {}", caseId, e.getMessage());
            return candidates;
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
