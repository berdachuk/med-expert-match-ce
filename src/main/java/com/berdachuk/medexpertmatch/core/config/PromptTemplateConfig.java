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
     * Creates a PromptTemplate for system instructions in MedGemma case analysis.
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
     * Creates a PromptTemplate for user data in MedGemma case analysis.
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
     * Creates a PromptTemplate for system instructions in MedGemma result interpretation.
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
     * Creates a PromptTemplate for user data in MedGemma result interpretation.
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
}
