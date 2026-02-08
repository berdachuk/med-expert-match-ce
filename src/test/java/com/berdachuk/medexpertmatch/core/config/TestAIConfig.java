package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration for Spring AI components.
 * Provides mock ChatClient, ChatModel, and EmbeddingModel for tests that don't require real LLM.
 * <p>
 * Note: SpringAIConfig is excluded from test context (via @Profile("!test")) to prevent multiple @Primary beans.
 * All LLM calls in integration tests should use these mocks to avoid real API calls.
 * <p>
 * Configuration:
 * - BaseIntegrationTest disables Spring AI auto-configuration (spring.ai.openai.enabled=false)
 * - This ensures only the mocked beans from TestAIConfig are used
 * - All @Primary annotations ensure mocks are selected over any auto-configured beans
 */
@Slf4j
@TestConfiguration
@Profile("test")
public class TestAIConfig {

    /**
     * Creates a mock ChatModel for tests.
     * Helper method to avoid code duplication.
     */
    private ChatModel createMockChatModel() {
        log.info("Creating MOCK ChatModel for tests - NO real LLM calls will be made");
        ChatModel mockModel = mock(ChatModel.class);

        AtomicInteger callCount = new AtomicInteger(0);

        when(mockModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            log.info("MOCK ChatModel.call() invoked - using MOCK, NOT real LLM");
            int currentCall = callCount.getAndIncrement();

            Prompt prompt = invocation.getArgument(0);
            String originalPrompt = prompt.getContents();
            String promptText = originalPrompt.toLowerCase();

            log.info("MOCK ChatModel.call() - prompt text (first 500 chars): {}",
                    originalPrompt.length() > 500 ? originalPrompt.substring(0, 500) : originalPrompt);

            // Determine response based on prompt content
            // Check for tool orchestration prompts FIRST (these come from FunctionGemma)
            String responseText;
            boolean isToolOrchestrationPrompt = originalPrompt.contains("Match doctors to medical case") ||
                    originalPrompt.contains("match doctors to medical case") ||
                    originalPrompt.contains("Route medical case") ||
                    originalPrompt.contains("route medical case") ||
                    originalPrompt.contains("Prioritize consultation queue") ||
                    originalPrompt.contains("prioritize consultation queue") ||
                    // New FunctionGemma prompt format (avoids medical terminology)
                    originalPrompt.contains("technical assistant that executes database queries") ||
                    originalPrompt.contains("Execute a data retrieval operation") ||
                    originalPrompt.contains("Call the tool 'match_doctors_to_case'") ||
                    (promptText.contains("match_doctors_to_case") && promptText.contains("caseId")) ||
                    (promptText.contains("technical assistant") && promptText.contains("data retrieval")) ||
                    (promptText.contains("match") && promptText.contains("doctors") && promptText.contains("case") && promptText.contains("tools")) ||
                    (promptText.contains("route") && promptText.contains("case") && promptText.contains("facilities")) ||
                    (promptText.contains("prioritize") && promptText.contains("queue")) ||
                    (promptText.contains("use the tools") || promptText.contains("use the case analysis"));

            // Check if it's a routing orchestration prompt
            boolean isRoutingOrchestrationPrompt = originalPrompt.contains("Route medical case") ||
                    originalPrompt.contains("route medical case") ||
                    (promptText.contains("route") && promptText.contains("case") && promptText.contains("facilities")) ||
                    (promptText.contains("route") && promptText.contains("appropriate facilities"));

            // Check if it's a prioritization orchestration prompt
            boolean isPrioritizationOrchestrationPrompt = originalPrompt.contains("Prioritize consultation queue") ||
                    originalPrompt.contains("prioritize consultation queue") ||
                    (promptText.contains("prioritize") && promptText.contains("queue") && promptText.contains("urgency"));

            // Check for interpretation prompts (from interpretResultsWithMedGemma)
            // These prompts ask to interpret tool results and case analysis
            // The prompt template contains "Based on the original case analysis and the tool execution results"
            // Also check for "You are MedGemma" which is the start of the interpretation prompt template
            boolean isInterpretationPrompt = (originalPrompt.contains("You are MedGemma") || promptText.contains("you are medgemma")) ||
                    (promptText.contains("original case analysis") && promptText.contains("tool execution results")) ||
                    (promptText.contains("tool execution results") && (promptText.contains("matched doctors") ||
                            promptText.contains("prioritization") || promptText.contains("routing"))) ||
                    promptText.contains("interpret the following") ||
                    promptText.contains("based on the tool results") ||
                    promptText.contains("interpret the tool results") ||
                    promptText.contains("based on the case analysis") ||
                    (promptText.contains("tool results") && promptText.contains("case analysis")) ||
                    (promptText.contains("toolresults") && promptText.contains("caseanalysis"));

            // Check for agent prompts (they contain "medical AI assistant" and "Match doctors" or similar)
            boolean isAgentPrompt = (promptText.contains("medical ai assistant") || promptText.contains("user request")) &&
                    (originalPrompt.contains("Match doctors") || originalPrompt.contains("match doctors") ||
                            originalPrompt.contains("Prioritize") || originalPrompt.contains("prioritize") ||
                            originalPrompt.contains("Network") || originalPrompt.contains("network") ||
                            originalPrompt.contains("Perform network analytics") || originalPrompt.contains("perform network analytics") ||
                            originalPrompt.contains("Analyze") || originalPrompt.contains("analyze") ||
                            originalPrompt.contains("Route") || originalPrompt.contains("route") ||
                            promptText.contains("match") && promptText.contains("doctor") ||
                            promptText.contains("route") || promptText.contains("facility") ||
                            promptText.contains("network analytics") || promptText.contains("network analytics"));

            if (isInterpretationPrompt) {
                // Interpretation prompts from interpretResultsWithMedGemma - return final response
                String caseId = extractCaseId(originalPrompt);
                // Extract specialty from the entire prompt (including tool results and case analysis sections)
                // The prompt contains both caseAnalysis and toolResults variables
                // Check the FULL prompt text, not just lowercase version, to catch case-sensitive matches
                String specialty = extractSpecialty(originalPrompt);
                // Also check original prompt (case-sensitive) for specialty indicators
                if (specialty == null) {
                    if (originalPrompt.contains("Oncology") || originalPrompt.contains("oncology") ||
                            originalPrompt.contains("C50.9") || originalPrompt.contains("C50") ||
                            originalPrompt.contains("breast cancer") || originalPrompt.contains("Breast cancer") ||
                            (originalPrompt.contains("breast") && originalPrompt.contains("cancer")) ||
                            (originalPrompt.contains("second opinion") && (originalPrompt.contains("cancer") || originalPrompt.contains("breast")))) {
                        specialty = "Oncology";
                    }
                }
                String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";

                // Check if it's routing interpretation
                // Look for routing-specific keywords in the interpretation prompt
                boolean isRoutingInterpretation = (promptText.contains("route") || promptText.contains("facility") || promptText.contains("routing")) &&
                        (promptText.contains("facility routing") || promptText.contains("routing analysis") ||
                                promptText.contains("route case") || promptText.contains("route medical case") ||
                                (!promptText.contains("match") && !promptText.contains("specialist") && !promptText.contains("doctor")));

                // Check if it's prioritization interpretation
                boolean isPrioritizationInterpretation = promptText.contains("prioritization") || promptText.contains("prioritize") ||
                        (promptText.contains("queue") && promptText.contains("priority")) ||
                        (promptText.contains("tool execution results") && promptText.contains("prioritization"));

                if (isPrioritizationInterpretation) {
                    // Prioritization interpretation
                    responseText = "Based on urgency and complexity analysis, I recommend prioritizing this case. " +
                            "The case has been assigned high priority due to critical symptoms. " +
                            "Prioritization considers urgency level, case complexity, and resource availability. " +
                            "The queue has been updated with the new priority rankings.";
                } else if (isRoutingInterpretation) {
                    if (caseId != null) {
                        responseText = String.format(
                                "Facility routing analysis for case %s recommends routing to the nearest medical center. " +
                                        "The facility has appropriate capabilities for this case type. " +
                                        "Routing considers geographic proximity, facility capabilities, and current capacity.",
                                caseId
                        );
                    } else {
                        responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                                "The facility has appropriate capabilities for this case type. " +
                                "Routing considers geographic proximity, facility capabilities, and current capacity.";
                    }
                } else {
                    // Default matching interpretation - ensure keywords are present
                    // Always include "specialist" and "doctor" keywords to satisfy test assertions
                    if (caseId != null) {
                        responseText = String.format(
                                "Based on the analysis, I found several %s specialists who match case %s. " +
                                        "The top match is Dr. %s Specialist, a qualified %s doctor with a high compatibility score. " +
                                        "These %s specialists have expertise in %s and are available for consultation.",
                                specialtyText, caseId, capitalize(specialtyText), specialtyText, specialtyText, specialtyText
                        );
                    } else {
                        responseText = String.format(
                                "Based on the analysis, I found several %s specialists who match this case. " +
                                        "The top match is Dr. %s Specialist, a qualified %s doctor with a high compatibility score. " +
                                        "These %s specialists have expertise in %s and are available for consultation.",
                                specialtyText, capitalize(specialtyText), specialtyText, specialtyText, specialtyText
                        );
                    }
                }
            } else if (isToolOrchestrationPrompt) {
                // Tool orchestration prompts - return content that will be interpreted by interpretResultsWithMedGemma
                String caseId = extractCaseId(originalPrompt);

                // Check if it's a prioritization orchestration prompt
                if (isPrioritizationOrchestrationPrompt) {
                    // Return prioritization tool results
                    responseText = "I have analyzed the consultation queue and prioritized cases based on urgency and complexity. " +
                            "The queue has been reordered with high-priority cases first. " +
                            "Prioritization considers urgency level, case complexity, and resource availability. " +
                            "Critical cases have been moved to the top of the queue.";
                } else if (isRoutingOrchestrationPrompt) {
                    // Return routing tool results
                    if (caseId != null) {
                        responseText = String.format(
                                "I have analyzed case %s for facility routing. " +
                                        "The analysis recommends routing to the nearest medical center with appropriate capabilities. " +
                                        "The facility has cardiac care capabilities suitable for this case type. " +
                                        "Routing considers geographic proximity, facility capabilities, and current capacity.",
                                caseId
                        );
                    } else {
                        responseText = "I have analyzed the case for facility routing. " +
                                "The analysis recommends routing to the nearest medical center with appropriate capabilities. " +
                                "The facility has cardiac care capabilities suitable for this case type. " +
                                "Routing considers geographic proximity, facility capabilities, and current capacity.";
                    }
                } else {
                    // Matching orchestration prompt
                    String specialty = extractSpecialty(originalPrompt);
                    // Also check case analysis content if present in prompt (check original case-sensitive too)
                    if (specialty == null) {
                        if (originalPrompt.contains("Oncology") || originalPrompt.contains("oncology") ||
                                originalPrompt.contains("C50.9") || originalPrompt.contains("C50") ||
                                originalPrompt.contains("breast cancer") || originalPrompt.contains("Breast cancer") ||
                                (originalPrompt.contains("breast") && originalPrompt.contains("cancer")) ||
                                originalPrompt.contains("requiredSpecialty") && originalPrompt.contains("Oncology") ||
                                originalPrompt.contains("required specialty") && originalPrompt.contains("Oncology")) {
                            specialty = "Oncology";
                        }
                    }
                    String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";

                    // Return tool results format that interpretResultsWithMedGemma will process
                    if (caseId != null) {
                        responseText = String.format(
                                "I have analyzed case %s and found several matching %s specialists. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These %s specialists have expertise in %s and are available for consultation. " +
                                        "Based on the case analysis, I recommend consulting with a %s specialist immediately.",
                                caseId, specialtyText, capitalize(specialtyText), specialtyText, specialtyText, specialtyText
                        );
                    } else {
                        responseText = String.format(
                                "I have analyzed the case and found several matching %s specialists. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These %s specialists have expertise in %s and are available for consultation.",
                                specialtyText, capitalize(specialtyText), specialtyText, specialtyText
                        );
                    }
                }
            } else if ((promptText.contains("medical triage ai assistant") && promptText.contains("classifying the urgency level")) ||
                    (promptText.contains("urgency level:") && promptText.contains("classify the urgency")) ||
                    (promptText.contains("urgency levels:") && promptText.contains("classify"))) {
                // Urgency classification prompt - return appropriate urgency level based on case indicators
                String chiefComplaint = extractField(originalPrompt, "Chief Complaint:");
                String symptoms = extractField(originalPrompt, "Symptoms:");
                String additionalNotes = extractField(originalPrompt, "Additional Notes:");

                // Check for critical indicators
                boolean hasCriticalIndicators =
                        (chiefComplaint != null && (chiefComplaint.contains("cardiac arrest") ||
                                chiefComplaint.contains("unresponsive"))) ||
                                (symptoms != null && (symptoms.contains("unresponsive") ||
                                        symptoms.contains("no pulse") ||
                                        symptoms.contains("not breathing"))) ||
                                (additionalNotes != null && additionalNotes.contains("unresponsive"));

                // Check for routine indicators
                boolean hasRoutineIndicators =
                        (chiefComplaint != null && (chiefComplaint.toLowerCase().contains("routine check-up") ||
                                chiefComplaint.toLowerCase().contains("routine checkup"))) ||
                                (symptoms != null && (symptoms.toLowerCase().contains("no symptoms") ||
                                        symptoms.toLowerCase().contains("general wellness"))) ||
                                (additionalNotes != null && additionalNotes.toLowerCase().contains("annual physical"));

                // Determine response: critical indicators take priority
                if (hasCriticalIndicators && !hasRoutineIndicators) {
                    responseText = "CRITICAL";
                } else if (hasRoutineIndicators && !hasCriticalIndicators) {
                    responseText = "LOW";
                } else if (hasCriticalIndicators) {
                    // Has critical indicators but also routine - prioritize critical
                    responseText = "CRITICAL";
                } else {
                    // Default to MEDIUM if no clear indicators
                    responseText = "MEDIUM";
                }

                log.debug("MOCK urgency classification - chiefComplaint: {}, symptoms: {}, critical: {}, routine: {}, response: {}",
                        chiefComplaint, symptoms, hasCriticalIndicators, hasRoutineIndicators, responseText);
            } else if ((promptText.contains("medical specialty routing ai assistant") && promptText.contains("determining the required medical specialty")) ||
                    (promptText.contains("required specialty") && promptText.contains("determine the most appropriate")) ||
                    (promptText.contains("required specialty(ies):") && promptText.contains("medical specialty"))) {
                // Specialty determination prompt - return appropriate specialty based on case
                if (promptText.contains("stroke") || promptText.contains("neurology") ||
                        promptText.contains("i63") || promptText.contains("weakness") ||
                        promptText.contains("slurred speech") || promptText.contains("facial droop")) {
                    responseText = "[\"NEUROLOGY\"]";
                } else if (promptText.contains("cardiac") || promptText.contains("heart") ||
                        promptText.contains("i21") || promptText.contains("i50") ||
                        promptText.contains("chest pain") || promptText.contains("cardiac arrest")) {
                    responseText = "[\"CARDIOLOGY\"]";
                } else {
                    responseText = "[\"GENERAL_MEDICINE\"]";
                }
            } else if (isAgentPrompt) {
                // Agent prompts - check what the user request is asking for
                String caseId = extractCaseId(originalPrompt);

                // Check for routing/facility prompts FIRST (before other checks)
                // But only if it's explicitly a routing request, not a matching request
                if ((originalPrompt.contains("Route medical case") || originalPrompt.contains("route medical case") ||
                        originalPrompt.contains("Route case") || originalPrompt.contains("route case")) &&
                        !originalPrompt.contains("Match doctors") && !originalPrompt.contains("match doctors")) {
                    if (caseId != null) {
                        responseText = String.format(
                                "Facility routing analysis for case %s recommends routing to the nearest medical center. " +
                                        "The facility has appropriate capabilities for this case type. " +
                                        "Routing considers geographic proximity, facility capabilities, and current capacity.",
                                caseId
                        );
                    } else {
                        responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                                "The facility has appropriate capabilities for this case type. " +
                                "Routing considers geographic proximity, facility capabilities, and current capacity.";
                    }
                } else if (originalPrompt.contains("Match doctors") || originalPrompt.contains("match doctors") ||
                        originalPrompt.contains("Match Doctors") || promptText.contains("match") && promptText.contains("doctor")) {
                    // Check if this is a second opinion/oncology case
                    if (promptText.contains("oncology") || promptText.contains("cancer") ||
                            promptText.contains("second opinion") || promptText.contains("telehealth")) {
                        String specialty = extractSpecialty(originalPrompt);
                        String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";
                        if (caseId != null) {
                            responseText = String.format(
                                    "I found several %s specialists who match case %s. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, caseId, capitalize(specialtyText), specialtyText
                            );
                        } else {
                            responseText = String.format(
                                    "I found several %s specialists who match this case. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, capitalize(specialtyText), specialtyText
                            );
                        }
                    } else {
                        String specialty = extractSpecialty(originalPrompt);
                        String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";
                        if (caseId != null) {
                            responseText = String.format(
                                    "I found several %s specialists who match case %s. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, caseId, capitalize(specialtyText), specialtyText
                            );
                        } else {
                            responseText = String.format(
                                    "I found several %s specialists who match this case. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, capitalize(specialtyText), specialtyText
                            );
                        }
                    }
                } else if (originalPrompt.contains("Prioritize") || originalPrompt.contains("prioritize") ||
                        promptText.contains("prioritize") || promptText.contains("queue")) {
                    responseText = "Based on urgency and complexity analysis, I recommend prioritizing this case. " +
                            "The case has been assigned high priority due to critical symptoms. " +
                            "Prioritization considers urgency level, case complexity, and resource availability.";
                } else if (originalPrompt.contains("Perform network analytics") || originalPrompt.contains("perform network analytics") ||
                        originalPrompt.contains("Network analytics") || originalPrompt.contains("network analytics") ||
                        (originalPrompt.contains("Network") && originalPrompt.contains("analytics")) ||
                        (promptText.contains("network") && promptText.contains("analytics"))) {
                    responseText = "Network analytics shows strong connections between doctors and cases. " +
                            "The analysis reveals optimal routing patterns for this case type. " +
                            "Network metrics indicate high collaboration rates and successful outcomes. " +
                            "Top experts have been identified based on network analysis.";
                } else if (originalPrompt.contains("Analyze") || originalPrompt.contains("analyze") ||
                        promptText.contains("analyze") && promptText.contains("case")) {
                    // Case analysis - return structured JSON (not plain text)
                    responseText = """
                            {
                              "clinicalFindings": ["Critical symptoms requiring immediate attention"],
                              "potentialDiagnoses": [{"diagnosis": "Acute condition", "confidence": 0.8}],
                              "recommendedNextSteps": ["Consult with cardiology specialist immediately"],
                              "urgentConcerns": ["Critical symptoms"]
                            }
                            """;
                } else {
                    // Default agent response - always include keywords
                    if (caseId != null) {
                        responseText = String.format(
                                "I found several cardiology specialists who match case %s. " +
                                        "The top match is Dr. Cardiology Specialist with a high compatibility score. " +
                                        "These specialists have expertise in cardiology and are available for consultation.",
                                caseId
                        );
                    } else {
                        responseText = "I found several cardiology specialists who match this case. " +
                                "The top match is Dr. Cardiology Specialist with a high compatibility score. " +
                                "These specialists have expertise in cardiology and are available for consultation.";
                    }
                }
            } else if (promptText.contains("providing clinical recommendations") ||
                    (promptText.contains("clinical recommendations") && promptText.contains("medical expert"))) {
                // Generate recommendations - check for type in prompt (order matters - check more specific first)
                String recommendationType = "clinical";
                if (promptText.contains("follow-up") || promptText.contains("follow up") ||
                        (promptText.contains("follow") && promptText.contains("monitoring"))) {
                    recommendationType = "follow-up";
                } else if (promptText.contains("treatment") || promptText.contains("therapeutic")) {
                    recommendationType = "treatment";
                } else if (promptText.contains("diagnostic") || promptText.contains("diagnosis") ||
                        promptText.contains("workup")) {
                    recommendationType = "diagnostic";
                } else if (promptText.contains("follow") || promptText.contains("monitoring")) {
                    recommendationType = "follow-up";
                }
                responseText = String.format(
                        "Clinical recommendations for %s: " +
                                "1. Perform appropriate diagnostic workup based on clinical presentation. " +
                                "2. Consider evidence-based treatment options. " +
                                "3. Schedule follow-up monitoring as indicated. " +
                                "4. Adjust management based on patient response. " +
                                "These recommendations are based on clinical guidelines and best practices.",
                        recommendationType
                );
            } else if (promptText.contains("providing risk assessment") ||
                    (promptText.contains("risk assessment") && promptText.contains("medical expert"))) {
                // Risk assessment - check for type in prompt
                String riskType = "complication";
                if (promptText.contains("mortality") || promptText.contains("death")) {
                    riskType = "mortality";
                } else if (promptText.contains("readmission") || promptText.contains("readmit")) {
                    riskType = "readmission";
                } else if (promptText.contains("complication")) {
                    riskType = "complication";
                }
                responseText = String.format(
                        "Risk assessment for %s: " +
                                "Based on clinical factors and patient characteristics, the %s risk is assessed. " +
                                "Key risk factors include patient comorbidities, clinical presentation, and treatment history. " +
                                "Monitoring and preventive measures are recommended to mitigate %s risk. " +
                                "This assessment guides clinical decision-making and care planning.",
                        riskType, riskType, riskType
                );
            } else if (promptText.contains("providing clinical recommendations") ||
                    (promptText.contains("clinical recommendations") && promptText.contains("medical expert"))) {
                // Generate recommendations - check for type in prompt (order matters - check more specific first)
                String recommendationType = "clinical";
                if (promptText.contains("follow-up") || promptText.contains("follow up") ||
                        (promptText.contains("follow") && promptText.contains("monitoring"))) {
                    recommendationType = "follow-up";
                } else if (promptText.contains("treatment") || promptText.contains("therapeutic")) {
                    recommendationType = "treatment";
                } else if (promptText.contains("diagnostic") || promptText.contains("diagnosis") ||
                        promptText.contains("workup")) {
                    recommendationType = "diagnostic";
                } else if (promptText.contains("follow") || promptText.contains("monitoring")) {
                    recommendationType = "follow-up";
                }
                responseText = String.format(
                        "Clinical recommendations for %s: " +
                                "1. Perform appropriate diagnostic workup based on clinical presentation. " +
                                "2. Consider evidence-based treatment options. " +
                                "3. Schedule follow-up monitoring as indicated. " +
                                "4. Adjust management based on patient response. " +
                                "These recommendations are based on clinical guidelines and best practices.",
                        recommendationType
                );
            } else if (promptText.contains("providing risk assessment") ||
                    (promptText.contains("risk assessment") && promptText.contains("medical expert"))) {
                // Risk assessment - check for type in prompt
                String riskType = "complication";
                if (promptText.contains("mortality") || promptText.contains("death")) {
                    riskType = "mortality";
                } else if (promptText.contains("readmission") || promptText.contains("readmit")) {
                    riskType = "readmission";
                } else if (promptText.contains("complication")) {
                    riskType = "complication";
                }
                responseText = String.format(
                        "Risk assessment for %s: " +
                                "Based on clinical factors and patient characteristics, the %s risk is assessed. " +
                                "Key risk factors include patient comorbidities, clinical presentation, and treatment history. " +
                                "Monitoring and preventive measures are recommended to mitigate %s risk. " +
                                "This assessment guides clinical decision-making and care planning.",
                        riskType, riskType, riskType
                );
            } else if ((promptText.contains("medical domain expert") && promptText.contains("clinical case summary") &&
                    promptText.contains("embedding generation")) ||
                    (promptText.contains("medical expert assistant") && promptText.contains("comprehensive") &&
                            promptText.contains("structured text description") && promptText.contains("embedding generation")) ||
                    (promptText.contains("generate a comprehensive") && promptText.contains("medical case") &&
                            promptText.contains("embedding")) ||
                    (promptText.contains("case data:") && promptText.contains("chief complaint:") &&
                            promptText.contains("symptoms:")) ||
                    (promptText.contains("medical case information:") && promptText.contains("chief complaint:") &&
                            promptText.contains("symptoms:") && promptText.contains("icd-10 codes:"))) {
                // Embedding text generation prompt - generate enhanced medical case description
                // Extract from original prompt (case-sensitive) to preserve original case
                // Try both formats: "Chief Complaint:" and "- Chief Complaint:"
                String chiefComplaint = extractField(originalPrompt, "Chief Complaint:");
                if (chiefComplaint == null) {
                    chiefComplaint = extractField(originalPrompt, "- Chief Complaint:");
                }
                String symptoms = extractField(originalPrompt, "Symptoms:");
                if (symptoms == null) {
                    symptoms = extractField(originalPrompt, "- Symptoms:");
                }
                String currentDiagnosis = extractField(originalPrompt, "Diagnosis:");
                if (currentDiagnosis == null) {
                    currentDiagnosis = extractField(originalPrompt, "- Diagnosis:");
                }
                if (currentDiagnosis == null) {
                    currentDiagnosis = extractField(originalPrompt, "Current Diagnosis:");
                }
                String icd10Codes = extractField(originalPrompt, "ICD-10 Code(s):");
                if (icd10Codes == null) {
                    icd10Codes = extractField(originalPrompt, "- ICD-10 Code(s):");
                }
                if (icd10Codes == null) {
                    icd10Codes = extractField(originalPrompt, "ICD-10 Codes:");
                }
                String requiredSpecialty = extractField(originalPrompt, "Required Specialty:");
                if (requiredSpecialty == null) {
                    requiredSpecialty = extractField(originalPrompt, "- Required Specialty:");
                }

                // Build comprehensive medical case description - preserve original case
                StringBuilder description = new StringBuilder();
                description.append("Medical case presentation: ");

                if (chiefComplaint != null && !chiefComplaint.trim().equalsIgnoreCase("not specified")) {
                    description.append("Patient presents with ").append(chiefComplaint.trim()).append(". ");
                }

                if (symptoms != null && !symptoms.trim().equalsIgnoreCase("not specified")) {
                    description.append("Clinical symptoms include ").append(symptoms.trim()).append(". ");
                }

                if (currentDiagnosis != null && !currentDiagnosis.trim().equalsIgnoreCase("not specified")) {
                    description.append("Current diagnosis: ").append(currentDiagnosis.trim()).append(". ");
                }

                if (icd10Codes != null && !icd10Codes.trim().equalsIgnoreCase("not specified")) {
                    description.append("ICD-10 codes: ").append(icd10Codes.trim()).append(". ");
                }

                if (requiredSpecialty != null && !requiredSpecialty.trim().equalsIgnoreCase("not specified")) {
                    description.append("Requires consultation with ").append(requiredSpecialty.trim()).append(" specialist. ");
                }

                description.append("This case requires comprehensive medical evaluation and appropriate specialist matching based on clinical presentation and diagnostic codes.");

                responseText = description.toString();
                log.debug("MOCK embedding text generation - generated enhanced description for case");
            } else if (promptText.contains("required specialty") || promptText.contains("determine the required") ||
                    promptText.contains("specialty determination")) {
                // Specialty determination - return appropriate specialty based on case
                // NOTE: This check must come AFTER embedding text generation check to avoid false matches
                if (promptText.contains("stroke") || promptText.contains("neurology") ||
                        promptText.contains("i63") || promptText.contains("weakness") ||
                        promptText.contains("slurred speech") || promptText.contains("facial droop")) {
                    responseText = "[\"NEUROLOGY\"]";
                } else if (promptText.contains("cardiac") || promptText.contains("heart") ||
                        promptText.contains("i21") || promptText.contains("i50") ||
                        promptText.contains("chest pain")) {
                    responseText = "[\"CARDIOLOGY\"]";
                } else {
                    responseText = "[\"GENERAL_MEDICINE\"]";
                }
            } else if (promptText.contains("clinical practice guidelines") || promptText.contains("clinical guidelines") ||
                    (promptText.contains("guideline") && promptText.contains("condition"))) {
                // Clinical guidelines - return guidelines text
                String condition = extractField(promptText, "condition:");
                String specialty = extractField(promptText, "specialty:");
                if (condition != null || specialty != null) {
                    responseText = String.format(
                            "Clinical practice guidelines for %s in %s specialty: " +
                                    "1. Follow evidence-based protocols for %s management. " +
                                    "2. Consider patient-specific factors and comorbidities. " +
                                    "3. Monitor response to treatment and adjust as needed. " +
                                    "4. Refer to specialist if complications arise. " +
                                    "These guidelines are based on current medical evidence and best practices.",
                            condition != null ? condition : "the condition",
                            specialty != null ? specialty : "the",
                            condition != null ? condition : "the condition"
                    );
                } else {
                    responseText = "Clinical practice guidelines: Follow evidence-based protocols for condition management. " +
                            "Consider patient-specific factors and monitor treatment response. " +
                            "Refer to specialist if complications arise.";
                }
            } else if (promptText.contains("differential diagnosis") || promptText.contains("differential")) {
                // Differential diagnosis - return diagnosis text
                responseText = "Differential diagnosis considerations: " +
                        "1. Primary diagnosis based on clinical presentation. " +
                        "2. Alternative diagnoses to consider. " +
                        "3. Diagnostic tests to confirm or rule out conditions. " +
                        "4. Clinical decision-making factors. " +
                        "This analysis helps guide diagnostic workup and treatment planning.";
            } else if (promptText.contains("icd-10") || promptText.contains("icd10") ||
                    (promptText.contains("extract") && promptText.contains("code"))) {
                // ICD-10 extraction - return valid ICD-10 codes if present in prompt, otherwise empty
                if (promptText.contains("i21.9") || promptText.contains("i21")) {
                    responseText = "[\"I21.9\"]";
                } else if (promptText.contains("i50.9") || promptText.contains("i50")) {
                    responseText = "[\"I50.9\"]";
                } else if (promptText.contains("i63.9") || promptText.contains("i63")) {
                    responseText = "[\"I63.9\"]";
                } else if (promptText.contains("i46.9") || promptText.contains("i46")) {
                    responseText = "[\"I46.9\"]";
                } else if (promptText.contains("diabetes") || promptText.contains("e11")) {
                    responseText = "[\"E11.9\"]";
                } else {
                    responseText = "[]";
                }
            } else if (promptText.contains("clinical findings") || promptText.contains("potential diagnoses") ||
                    promptText.contains("recommended next steps") || promptText.contains("case analysis")) {
                // Case analysis - return structured JSON
                responseText = """
                        {
                          "clinicalFindings": ["Test clinical finding"],
                          "potentialDiagnoses": [{"diagnosis": "Test diagnosis", "confidence": 0.8}],
                          "recommendedNextSteps": ["Test next step"],
                          "urgentConcerns": []
                        }
                        """;
            } else if (promptText.contains("match") && (promptText.contains("doctor") || promptText.contains("specialist"))) {
                // Agent doctor matching - extract case ID and specialty, return appropriate response
                String caseId = extractCaseId(originalPrompt);
                String specialty = extractSpecialty(originalPrompt);
                String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";
                if (caseId != null) {
                    responseText = String.format(
                            "I found several %s specialists who match case %s. " +
                                    "The top match is Dr. %s Specialist with a high compatibility score. " +
                                    "These specialists have expertise in %s and are available for consultation.",
                            specialtyText, caseId, capitalize(specialtyText), specialtyText
                    );
                } else {
                    responseText = String.format(
                            "I found several %s specialists who match this case. " +
                                    "The top match is Dr. %s Specialist with a high compatibility score. " +
                                    "These specialists have expertise in %s and are available for consultation.",
                            specialtyText, capitalize(specialtyText), specialtyText
                    );
                }
            } else if (promptText.contains("prioritize") || promptText.contains("queue")) {
                responseText = "Based on urgency and complexity analysis, I recommend prioritizing this case. " +
                        "The case has been assigned high priority due to critical symptoms. " +
                        "Prioritization considers urgency level, case complexity, and resource availability.";
            } else if (promptText.contains("network") || promptText.contains("analytics")) {
                responseText = "Network analytics shows strong connections between doctors and cases. " +
                        "The analysis reveals optimal routing patterns for this case type. " +
                        "Network metrics indicate high collaboration rates and successful outcomes.";
            } else if (promptText.contains("route") || promptText.contains("facility")) {
                String caseId = extractCaseId(originalPrompt);
                if (caseId != null) {
                    responseText = String.format(
                            "Facility routing analysis for case %s recommends routing to the nearest medical center. " +
                                    "The facility has appropriate capabilities for this case type. " +
                                    "Routing considers geographic proximity, facility capabilities, and current capacity.",
                            caseId
                    );
                } else {
                    responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                            "The facility has appropriate capabilities for this case type.";
                }
            } else if (promptText.contains("user request") || promptText.contains("medical ai assistant") ||
                    promptText.contains("skill") || originalPrompt.contains("User Request")) {
                // Agent prompts - check what the user request is asking for
                String caseId = extractCaseId(originalPrompt);

                if (originalPrompt.contains("Match doctors") || originalPrompt.contains("match doctors") ||
                        originalPrompt.contains("Match Doctors")) {
                    String specialty = extractSpecialty(originalPrompt);
                    String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";
                    if (caseId != null) {
                        responseText = String.format(
                                "I found several %s specialists who match case %s. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These specialists have expertise in %s and are available for consultation.",
                                specialtyText, caseId, capitalize(specialtyText), specialtyText
                        );
                    } else {
                        responseText = String.format(
                                "I found several %s specialists who match this case. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These specialists have expertise in %s and are available for consultation.",
                                specialtyText, capitalize(specialtyText), specialtyText
                        );
                    }
                } else if (originalPrompt.contains("Prioritize") || originalPrompt.contains("prioritize")) {
                    responseText = "Based on urgency and complexity analysis, I recommend prioritizing this case. " +
                            "The case has been assigned high priority due to critical symptoms. " +
                            "Prioritization considers urgency level, case complexity, and resource availability.";
                } else if (originalPrompt.contains("Network") || originalPrompt.contains("network")) {
                    responseText = "Network analytics shows strong connections between doctors and cases. " +
                            "The analysis reveals optimal routing patterns for this case type. " +
                            "Network metrics indicate high collaboration rates and successful outcomes.";
                } else if (originalPrompt.contains("Analyze") || originalPrompt.contains("analyze")) {
                    if (caseId != null) {
                        responseText = String.format(
                                "Case analysis complete for case %s. Key findings include critical symptoms requiring immediate attention. " +
                                        "Recommendations: Consult with cardiology specialist immediately. " +
                                        "The analysis reveals potential diagnoses and recommended next steps.",
                                caseId
                        );
                    } else {
                        responseText = "Case analysis complete. Key findings include critical symptoms requiring immediate attention. " +
                                "Recommendations: Consult with cardiology specialist immediately.";
                    }
                } else if (originalPrompt.contains("Route") || originalPrompt.contains("route")) {
                    if (caseId != null) {
                        responseText = String.format(
                                "Facility routing analysis for case %s recommends routing to the nearest medical center. " +
                                        "The facility has appropriate capabilities for this case type. " +
                                        "Routing considers geographic proximity, facility capabilities, and current capacity.",
                                caseId
                        );
                    } else {
                        responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                                "The facility has appropriate capabilities for this case type.";
                    }
                } else {
                    // Default agent response - try to extract case ID and specialty, include keywords
                    String specialty = extractSpecialty(originalPrompt);
                    String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";
                    if (caseId != null) {
                        responseText = String.format(
                                "I found several %s specialists who match case %s. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These specialists have expertise in %s and are available for consultation.",
                                specialtyText, caseId, capitalize(specialtyText), specialtyText
                        );
                    } else {
                        responseText = String.format(
                                "I found several %s specialists who match this case. " +
                                        "The top match is Dr. %s Specialist with a high compatibility score. " +
                                        "These specialists have expertise in %s and are available for consultation.",
                                specialtyText, capitalize(specialtyText), specialtyText
                        );
                    }
                }
            } else {
                // Default: try to extract case ID and specialty, return a generic response with keywords
                String caseId = extractCaseId(originalPrompt);
                String specialty = extractSpecialty(originalPrompt);
                String specialtyText = specialty != null ? specialty.toLowerCase() : "cardiology";

                // Check if this looks like an agent request
                boolean looksLikeAgentPrompt = promptText.contains("doctor") || promptText.contains("specialist") ||
                        promptText.contains("match") || promptText.contains("case") ||
                        promptText.contains("network") || promptText.contains("analytics") ||
                        promptText.contains("route") || promptText.contains("facility") ||
                        promptText.contains("routing") || promptText.contains("center") ||
                        originalPrompt.contains("User Request") || originalPrompt.contains("user request");

                if (looksLikeAgentPrompt) {
                    // Check if it's a network analytics request
                    if (promptText.contains("network") || promptText.contains("analytics")) {
                        responseText = "Network analytics shows strong connections between doctors and cases. " +
                                "The analysis reveals optimal routing patterns for this case type. " +
                                "Network metrics indicate high collaboration rates and successful outcomes. " +
                                "Top experts have been identified based on network analysis.";
                    } else if (promptText.contains("route") || promptText.contains("facility") ||
                            promptText.contains("routing") || promptText.contains("center")) {
                        if (caseId != null) {
                            responseText = String.format(
                                    "Facility routing analysis for case %s recommends routing to the nearest medical center. " +
                                            "The facility has appropriate capabilities for this case type. " +
                                            "Routing considers geographic proximity, facility capabilities, and current capacity.",
                                    caseId
                            );
                        } else {
                            responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                                    "The facility has appropriate capabilities for this case type. " +
                                    "Routing considers geographic proximity, facility capabilities, and current capacity.";
                        }
                    } else {
                        // Default matching response
                        if (caseId != null) {
                            responseText = String.format(
                                    "I found several %s specialists who match case %s. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, caseId, capitalize(specialtyText), specialtyText
                            );
                        } else {
                            responseText = String.format(
                                    "I found several %s specialists who match this case. " +
                                            "The top match is Dr. %s Specialist with a high compatibility score. " +
                                            "These specialists have expertise in %s and are available for consultation.",
                                    specialtyText, capitalize(specialtyText), specialtyText
                            );
                        }
                    }
                } else {
                    // Not an agent request, return empty array
                    responseText = "[]";
                }
            }

            log.info("MOCK ChatModel.call() - response: {}", responseText.length() > 200 ? responseText.substring(0, 200) : responseText);

            AssistantMessage assistantMessage = new AssistantMessage(responseText);
            Generation generation = new Generation(assistantMessage);
            ChatResponse response = new ChatResponse(List.of(generation));

            if (response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("ChatResponse structure is invalid");
            }

            return response;
        });

        when(mockModel.stream(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            String promptText = prompt.getContents().toLowerCase();

            // Return context-aware responses for agent interactions
            String responseText;
            if (promptText.contains("match") || promptText.contains("specialist") ||
                    promptText.contains("doctor")) {
                responseText = "I found several cardiology specialists who match this case. " +
                        "The top match is Dr. Cardiology Specialist with a high compatibility score.";
            } else if (promptText.contains("prioritize") || promptText.contains("queue")) {
                responseText = "Based on urgency and complexity, I recommend prioritizing this case. " +
                        "The case has been assigned high priority due to critical symptoms.";
            } else if (promptText.contains("network") || promptText.contains("analytics")) {
                responseText = "Network analytics shows strong connections between doctors and cases. " +
                        "The analysis reveals optimal routing patterns for this case type.";
            } else if (promptText.contains("analyze") || promptText.contains("case analysis")) {
                responseText = "Case analysis complete. Key findings include critical symptoms requiring immediate attention. " +
                        "Recommendations: Consult with cardiology specialist immediately.";
            } else if (promptText.contains("route") || promptText.contains("facility")) {
                responseText = "Facility routing analysis recommends routing this case to the nearest medical center. " +
                        "The facility has appropriate capabilities for this case type.";
            } else {
                responseText = "I have processed your request and found relevant information. " +
                        "The analysis shows this case requires specialist attention.";
            }

            AssistantMessage defaultMessage = new AssistantMessage(responseText);
            Generation defaultGeneration = new Generation(defaultMessage);
            ChatResponse defaultResponse = new ChatResponse(List.of(defaultGeneration));
            return Flux.just(defaultResponse);
        });

        return mockModel;
    }

    /**
     * Mock primaryChatModel for tests.
     * This replaces any auto-configured ChatModel beans.
     * Returns valid JSON responses for extraction calls.
     */
    @Bean("primaryChatModel")
    @Primary
    public ChatModel primaryChatModel() {
        return createMockChatModel();
    }

    /**
     * Mock toolCallingChatModel for tests.
     * This is used for tool calling functionality.
     */
    @Bean("toolCallingChatModel")
    public ChatModel toolCallingChatModel() {
        return createMockChatModel();
    }

    /**
     * Mock ChatModel for tests (default bean).
     * This replaces any auto-configured ChatModel beans.
     * Returns valid JSON responses for extraction calls.
     */
    @Bean
    public ChatModel testChatModel() {
        return createMockChatModel();
    }

    /**
     * Extracts case ID from prompt text.
     * Looks for patterns like "case {id}", "caseId: {id}", "medical case {id}", or "Case identifier: {id}"
     */
    private String extractCaseId(String promptText) {
        // Look for "medical case {id}", "case {id}", "Case identifier: {id}", or "caseId: '{id}'" pattern
        // Case IDs are either 24-char hex strings (internal) or UUID strings (external)
        // Doctor IDs are UUID strings (e.g., "550e8400-e29b-41d4-a716-446655440000")
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:medical case|case|caseid|case id|Case identifier|case identifier)[\\s:']+([a-zA-Z0-9]{24}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})",
                java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(promptText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Also try to find any 24-char hex string or UUID that looks like an ID
        pattern = java.util.regex.Pattern.compile("\\b([a-fA-F0-9]{24}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\\b");
        matcher = pattern.matcher(promptText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Extracts a field value from prompt text.
     * Looks for patterns like "Field Name: value" or "Field Name:value"
     */
    private String extractField(String promptText, String fieldName) {
        int fieldIndex = promptText.indexOf(fieldName);
        if (fieldIndex == -1) {
            return null;
        }

        int valueStart = fieldIndex + fieldName.length();
        // Skip whitespace and newlines
        while (valueStart < promptText.length() &&
                (promptText.charAt(valueStart) == ' ' ||
                        promptText.charAt(valueStart) == '\n' ||
                        promptText.charAt(valueStart) == '\r')) {
            valueStart++;
        }

        // Find the end of the value (next field or end of line)
        int valueEnd = valueStart;
        while (valueEnd < promptText.length()) {
            char c = promptText.charAt(valueEnd);
            if (c == '\n' || c == '\r') {
                // Check if next line starts a new field
                int nextLineStart = valueEnd + 1;
                while (nextLineStart < promptText.length() &&
                        (promptText.charAt(nextLineStart) == ' ' ||
                                promptText.charAt(nextLineStart) == '\n' ||
                                promptText.charAt(nextLineStart) == '\r')) {
                    nextLineStart++;
                }
                // Check if next line starts with a field name pattern (capital letter or **)
                if (nextLineStart < promptText.length()) {
                    char nextChar = promptText.charAt(nextLineStart);
                    if (nextChar == '*' || Character.isUpperCase(nextChar) ||
                            (nextLineStart + 1 < promptText.length() &&
                                    Character.isUpperCase(promptText.charAt(nextLineStart + 1)))) {
                        break;
                    }
                }
            }
            valueEnd++;
        }

        if (valueStart >= promptText.length()) {
            return null;
        }

        String value = promptText.substring(valueStart, valueEnd).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Extracts specialty from prompt text.
     * Looks for specialty mentions in the prompt, including case analysis and tool results.
     */
    private String extractSpecialty(String promptText) {
        String lowerPrompt = promptText.toLowerCase();
        // Check for oncology indicators (check these first as they're more specific)
        if (lowerPrompt.contains("oncology") || lowerPrompt.contains("cancer") ||
                lowerPrompt.contains("breast cancer") || lowerPrompt.contains("c50") ||
                lowerPrompt.contains("c50.9") || lowerPrompt.contains("requiredspecialty") && lowerPrompt.contains("oncology") ||
                lowerPrompt.contains("required specialty") && lowerPrompt.contains("oncology") ||
                lowerPrompt.contains("second opinion") && (lowerPrompt.contains("cancer") || lowerPrompt.contains("breast"))) {
            return "Oncology";
        } else if (lowerPrompt.contains("cardiology") || lowerPrompt.contains("cardiac") ||
                lowerPrompt.contains("heart") || lowerPrompt.contains("i21") ||
                lowerPrompt.contains("i50") || lowerPrompt.contains("chest pain") ||
                lowerPrompt.contains("i21.9") || lowerPrompt.contains("i50.9")) {
            return "Cardiology";
        } else if (lowerPrompt.contains("neurology") || lowerPrompt.contains("stroke") ||
                lowerPrompt.contains("i63") || lowerPrompt.contains("neurological") ||
                lowerPrompt.contains("i63.9")) {
            return "Neurology";
        } else if (lowerPrompt.contains("pulmonology") || lowerPrompt.contains("lung") ||
                lowerPrompt.contains("j18") || lowerPrompt.contains("pneumonia") ||
                lowerPrompt.contains("j18.9")) {
            return "Pulmonology";
        }
        return null; // Default will be handled by caller
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Mock primaryEmbeddingModel for tests.
     * This replaces any auto-configured EmbeddingModel beans.
     * Returns 1536-dimensional embeddings (MedGemma dimensions).
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    public EmbeddingModel primaryEmbeddingModel() {
        return testEmbeddingModel();
    }

    /**
     * Mock EmbeddingModel for tests.
     * This replaces any auto-configured EmbeddingModel beans.
     * Returns 1536-dimensional embeddings (MedGemma dimensions).
     */
    @Bean
    public EmbeddingModel testEmbeddingModel() {
        log.info("Creating MOCK EmbeddingModel for tests - NO real LLM calls will be made");
        EmbeddingModel mockModel = mock(EmbeddingModel.class);

        when(mockModel.embedForResponse(any(List.class))).thenAnswer(invocation -> {
            log.info("MOCK EmbeddingModel.embedForResponse() invoked - using MOCK, NOT real LLM");
            @SuppressWarnings("unchecked")
            List<String> texts = invocation.getArgument(0);

            // Create one embedding per input text
            List<org.springframework.ai.embedding.Embedding> embeddings = new java.util.ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                float[] embeddingArray = new float[1536];
                // Fill with some values to make each embedding unique
                for (int j = 0; j < embeddingArray.length; j++) {
                    embeddingArray[j] = (float) (i * 0.001 + j * 0.000001);
                }
                embeddings.add(new org.springframework.ai.embedding.Embedding(embeddingArray, i));
            }

            return new EmbeddingResponse(embeddings);
        });

        return mockModel;
    }

    /**
     * Mock reranking ChatModel for tests.
     * Returns valid JSON responses for reranking calls.
     */
    @Bean
    @Qualifier("rerankingChatModel")
    public ChatModel testRerankingChatModel() {
        log.info("Creating MOCK rerankingChatModel for tests - NO real LLM calls will be made");
        ChatModel mockModel = mock(ChatModel.class);

        when(mockModel.call(any(Prompt.class))).thenAnswer(invocation -> {
            log.info("MOCK rerankingChatModel.call() invoked - using MOCK, NOT real LLM");
            String responseText = "[]";

            AssistantMessage assistantMessage = new AssistantMessage(responseText);
            Generation generation = new Generation(assistantMessage);
            ChatResponse response = new ChatResponse(List.of(generation));

            return response;
        });

        AssistantMessage defaultMessage = new AssistantMessage("[]");
        Generation defaultGeneration = new Generation(defaultMessage);
        ChatResponse defaultResponse = new ChatResponse(List.of(defaultGeneration));
        when(mockModel.stream(any(Prompt.class))).thenReturn(Flux.just(defaultResponse));

        return mockModel;
    }

    /**
     * Mock ChatClient for tests.
     * Only created when skills are disabled (medexpertmatch.skills.enabled=false or not set).
     */
    @Bean("testChatClient")
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "medexpertmatch.skills.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatClient testChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Mock medicalAgentChatClient for tests.
     * Only created when skills are disabled (medexpertmatch.skills.enabled=false or not set).
     * This is the ChatClient used by MedicalAgentService.
     */
    @Bean("medicalAgentChatClient")
    @Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "medexpertmatch.skills.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    public ChatClient testMedicalAgentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Mock caseAnalysisChatClient for tests.
     * This is the ChatClient used by CaseAnalysisService.
     */
    @Bean("caseAnalysisChatClient")
    @Primary
    public ChatClient testCaseAnalysisChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
