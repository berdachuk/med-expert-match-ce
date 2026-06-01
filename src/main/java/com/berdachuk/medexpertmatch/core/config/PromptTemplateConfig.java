package com.berdachuk.medexpertmatch.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configuration for Spring AI PromptTemplate with StringTemplate renderer.
 * Uses custom delimiters (< and >) to avoid conflicts with JSON syntax in templates.
 */
@Slf4j
@Configuration
public class PromptTemplateConfig {

    @Value("classpath:/prompts/case-analysis-system.st")
    private Resource caseAnalysisSystemResource;
    @Value("classpath:/prompts/case-analysis-user.st")
    private Resource caseAnalysisUserResource;
    @Value("classpath:/prompts/icd10-extraction-system.st")
    private Resource icd10ExtractionSystemResource;
    @Value("classpath:/prompts/icd10-extraction-user.st")
    private Resource icd10ExtractionUserResource;
    @Value("classpath:/prompts/urgency-classification-system.st")
    private Resource urgencyClassificationSystemResource;
    @Value("classpath:/prompts/urgency-classification-user.st")
    private Resource urgencyClassificationUserResource;
    @Value("classpath:/prompts/specialty-determination-system.st")
    private Resource specialtyDeterminationSystemResource;
    @Value("classpath:/prompts/specialty-determination-user.st")
    private Resource specialtyDeterminationUserResource;
    @Value("classpath:/prompts/medgemma-case-analysis-system.st")
    private Resource medgemmaCaseAnalysisSystemResource;
    @Value("classpath:/prompts/medgemma-case-analysis-user.st")
    private Resource medgemmaCaseAnalysisUserResource;
    @Value("classpath:/prompts/medgemma-result-interpretation-system.st")
    private Resource medgemmaResultInterpretationSystemResource;
    @Value("classpath:/prompts/medgemma-result-interpretation-user.st")
    private Resource medgemmaResultInterpretationUserResource;
    @Value("classpath:/prompts/embedding-text-generation.st")
    private Resource embeddingTextGenerationResource;
    @Value("classpath:/prompts/embedding-text-generation-system.st")
    private Resource embeddingTextGenerationSystemResource;
    @Value("classpath:/prompts/embedding-text-generation-user.st")
    private Resource embeddingTextGenerationUserResource;
    @Value("classpath:/prompts/routing-summarization.st")
    private Resource routingSummarizationResource;
    @Value("classpath:/prompts/network-analytics-summarization.st")
    private Resource networkAnalyticsSummarizationResource;
    @Value("classpath:/prompts/clinical-guidelines-search.st")
    private Resource clinicalGuidelinesSearchResource;
    @Value("classpath:/prompts/clinical-recommendations.st")
    private Resource clinicalRecommendationsResource;
    @Value("classpath:/prompts/clinical-recommendations-task-diagnostic.st")
    private Resource clinicalRecommendationsTaskDiagnosticResource;
    @Value("classpath:/prompts/clinical-recommendations-task-treatment.st")
    private Resource clinicalRecommendationsTaskTreatmentResource;
    @Value("classpath:/prompts/clinical-recommendations-task-follow-up.st")
    private Resource clinicalRecommendationsTaskFollowUpResource;
    @Value("classpath:/prompts/clinical-recommendations-task-default.st")
    private Resource clinicalRecommendationsTaskDefaultResource;
    @Value("classpath:/prompts/clinical-recommendations-evidence-instruction.st")
    private Resource clinicalRecommendationsEvidenceInstructionResource;
    @Value("classpath:/prompts/differential-diagnosis.st")
    private Resource differentialDiagnosisResource;
    @Value("classpath:/prompts/risk-assessment.st")
    private Resource riskAssessmentResource;
    @Value("classpath:/prompts/risk-assessment-task-complication.st")
    private Resource riskAssessmentTaskComplicationResource;
    @Value("classpath:/prompts/risk-assessment-task-mortality.st")
    private Resource riskAssessmentTaskMortalityResource;
    @Value("classpath:/prompts/risk-assessment-task-readmission.st")
    private Resource riskAssessmentTaskReadmissionResource;
    @Value("classpath:/prompts/risk-assessment-task-default.st")
    private Resource riskAssessmentTaskDefaultResource;
    @Value("classpath:/prompts/auto-memory-system.st")
    private Resource autoMemorySystemResource;
    @Value("classpath:/prompts/chat-agent-system.st")
    private Resource chatAgentSystemResource;
    @Value("classpath:/prompts/chat-agent-orchestrator-instructions.st")
    private Resource chatAgentOrchestratorInstructionsResource;
    @Value("classpath:/prompts/chat-case-id-hint.st")
    private Resource chatCaseIdHintResource;
    @Value("classpath:/prompts/chat-no-case-id-hint.st")
    private Resource chatNoCaseIdHintResource;
    @Value("classpath:/prompts/chat-user-message.st")
    private Resource chatUserMessageResource;
    @Value("classpath:/prompts/agent-matching-orchestration.st")
    private Resource agentMatchingOrchestrationResource;
    @Value("classpath:/prompts/goal-classification.st")
    private Resource goalClassificationResource;

    @Bean
    public StTemplateRenderer stTemplateRenderer() {
        return StTemplateRenderer.builder()
                .startDelimiterToken('<')
                .endDelimiterToken('>')
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in case analysis.
     * Contains role definition, disclaimer, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("caseAnalysisSystemPromptTemplate")
    public PromptTemplate caseAnalysisSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(caseAnalysisSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in case analysis.
     * Contains case-specific variables (chief complaint, symptoms, additional notes).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("caseAnalysisUserPromptTemplate")
    public PromptTemplate caseAnalysisUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(caseAnalysisUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in ICD-10 extraction.
     * Contains role definition, disclaimer, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("icd10ExtractionSystemPromptTemplate")
    public PromptTemplate icd10ExtractionSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(icd10ExtractionSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in ICD-10 extraction.
     * Contains case-specific variables (chief complaint, symptoms, diagnosis, additional notes).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("icd10ExtractionUserPromptTemplate")
    public PromptTemplate icd10ExtractionUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(icd10ExtractionUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in urgency classification.
     * Contains role definition, disclaimer, urgency levels reference, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("urgencyClassificationSystemPromptTemplate")
    public PromptTemplate urgencyClassificationSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(urgencyClassificationSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in urgency classification.
     * Contains case-specific variables (chief complaint, symptoms, diagnosis, additional notes).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("urgencyClassificationUserPromptTemplate")
    public PromptTemplate urgencyClassificationUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(urgencyClassificationUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in specialty determination.
     * Contains role definition, disclaimer, available specialties list, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("specialtyDeterminationSystemPromptTemplate")
    public PromptTemplate specialtyDeterminationSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(specialtyDeterminationSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in specialty determination.
     * Contains case-specific variables (chief complaint, symptoms, diagnosis, ICD-10 codes, additional notes).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("specialtyDeterminationUserPromptTemplate")
    public PromptTemplate specialtyDeterminationUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(specialtyDeterminationUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in LLM case analysis.
     * Contains role definition, disclaimer, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("medgemmaCaseAnalysisSystemPromptTemplate")
    public PromptTemplate medgemmaCaseAnalysisSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(medgemmaCaseAnalysisSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in LLM case analysis.
     * Contains case-specific variables (case ID, chief complaint, symptoms, additional notes).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("medgemmaCaseAnalysisUserPromptTemplate")
    public PromptTemplate medgemmaCaseAnalysisUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(medgemmaCaseAnalysisUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in LLM result interpretation.
     * Contains role definition, disclaimer, task, response format, and output limits.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("medgemmaResultInterpretationSystemPromptTemplate")
    public PromptTemplate medgemmaResultInterpretationSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(medgemmaResultInterpretationSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in LLM result interpretation.
     * Contains case analysis and tool execution results.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("medgemmaResultInterpretationUserPromptTemplate")
    public PromptTemplate medgemmaResultInterpretationUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(medgemmaResultInterpretationUserResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for generating enhanced embedding text using LLM.
     * Used to create comprehensive medical case descriptions optimized for embedding generation.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("descriptionGenerationPromptTemplate")
    public PromptTemplate descriptionGenerationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(embeddingTextGenerationResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for system instructions in embedding text generation.
     * Contains role definition, goals, and disclaimer.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("descriptionGenerationSystemPromptTemplate")
    public PromptTemplate descriptionGenerationSystemPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(embeddingTextGenerationSystemResource)
                .build();
    }

    /**
     * Creates a PromptTemplate for user data in embedding text generation.
     * Contains case-specific variables (chief complaint, symptoms, diagnosis, etc.).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("descriptionGenerationUserPromptTemplate")
    public PromptTemplate descriptionGenerationUserPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(embeddingTextGenerationUserResource)
                .build();
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("routingSummarizationPromptTemplate")
    public PromptTemplate routingSummarizationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(routingSummarizationResource)
                .build();
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("networkAnalyticsSummarizationPromptTemplate")
    public PromptTemplate networkAnalyticsSummarizationPromptTemplate(StTemplateRenderer renderer) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(networkAnalyticsSummarizationResource)
                .build();
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalGuidelinesSearchPromptTemplate")
    public PromptTemplate clinicalGuidelinesSearchPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalGuidelinesSearchResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsPromptTemplate")
    public PromptTemplate clinicalRecommendationsPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsTaskDiagnosticPromptTemplate")
    public PromptTemplate clinicalRecommendationsTaskDiagnosticPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsTaskDiagnosticResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsTaskTreatmentPromptTemplate")
    public PromptTemplate clinicalRecommendationsTaskTreatmentPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsTaskTreatmentResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsTaskFollowUpPromptTemplate")
    public PromptTemplate clinicalRecommendationsTaskFollowUpPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsTaskFollowUpResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsTaskDefaultPromptTemplate")
    public PromptTemplate clinicalRecommendationsTaskDefaultPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsTaskDefaultResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("clinicalRecommendationsEvidenceInstructionPromptTemplate")
    public PromptTemplate clinicalRecommendationsEvidenceInstructionPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, clinicalRecommendationsEvidenceInstructionResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("differentialDiagnosisPromptTemplate")
    public PromptTemplate differentialDiagnosisPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, differentialDiagnosisResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("riskAssessmentPromptTemplate")
    public PromptTemplate riskAssessmentPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, riskAssessmentResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("riskAssessmentTaskComplicationPromptTemplate")
    public PromptTemplate riskAssessmentTaskComplicationPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, riskAssessmentTaskComplicationResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("riskAssessmentTaskMortalityPromptTemplate")
    public PromptTemplate riskAssessmentTaskMortalityPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, riskAssessmentTaskMortalityResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("riskAssessmentTaskReadmissionPromptTemplate")
    public PromptTemplate riskAssessmentTaskReadmissionPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, riskAssessmentTaskReadmissionResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("riskAssessmentTaskDefaultPromptTemplate")
    public PromptTemplate riskAssessmentTaskDefaultPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, riskAssessmentTaskDefaultResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("autoMemorySystemPromptTemplate")
    public PromptTemplate autoMemorySystemPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, autoMemorySystemResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("chatAgentSystemPromptTemplate")
    public PromptTemplate chatAgentSystemPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, chatAgentSystemResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("chatAgentOrchestratorInstructionsPromptTemplate")
    public PromptTemplate chatAgentOrchestratorInstructionsPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, chatAgentOrchestratorInstructionsResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("chatCaseIdHintPromptTemplate")
    public PromptTemplate chatCaseIdHintPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, chatCaseIdHintResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("chatNoCaseIdHintPromptTemplate")
    public PromptTemplate chatNoCaseIdHintPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, chatNoCaseIdHintResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("chatUserMessagePromptTemplate")
    public PromptTemplate chatUserMessagePromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, chatUserMessageResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("agentMatchingOrchestrationPromptTemplate")
    public PromptTemplate agentMatchingOrchestrationPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, agentMatchingOrchestrationResource);
    }

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("goalClassificationPromptTemplate")
    public PromptTemplate goalClassificationPromptTemplate(StTemplateRenderer renderer) {
        return promptTemplate(renderer, goalClassificationResource);
    }

    private static PromptTemplate promptTemplate(StTemplateRenderer renderer, Resource resource) {
        return PromptTemplate.builder()
                .renderer(renderer)
                .resource(resource)
                .build();
    }
}
