# LLM Prompts

## Description
Spring AI prompt template creation, management, and configuration. Covers external `.st` (StringTemplate) files, `PromptTemplateConfig` bean wiring, and medical compliance requirements.

## When to use
- Creating or modifying LLM prompt templates
- Configuring Spring AI `PromptTemplate` beans
- Adding medical disclaimers to AI-generated content
- Debugging prompt rendering or template resolution
- Answering: "How do I add a new prompt?"

## Instructions

### Template Location and Format

- All prompt files: `src/main/resources/prompts/` directory
- File extension: `.st` (StringTemplate format)
- Template file organization (existing examples):
  ```
  prompts/analysis.st        — Case analysis prompts
  prompts/matching.st        — Doctor matching prompts
  prompts/routing.st         — Facility routing prompts
  prompts/recommendation.st  — Recommendation prompts
  ```

### Creating a New Prompt

1. Create `prompts/{purpose}.st` with StringTemplate syntax
2. Declare a `PromptTemplate` bean in `core/config/PromptTemplateConfig.java`:
   ```java
   @Bean
   @Qualifier("matchingPromptTemplate")
   PromptTemplate matchingPromptTemplate() {
       return new PromptTemplate(
           new ClassPathResource("prompts/matching.st")
       );
   }
   ```
3. Inject via constructor using `@Qualifier`:
   ```java
   public MatchingServiceImpl(
       @Qualifier("matchingPromptTemplate") PromptTemplate matchingPrompt
   ) { ... }
   ```

### Template Variables

- Use standard StringTemplate syntax: `$variableName$` for substitutions
- Never embed sensitive data in templates — pass as runtime variables only
- Medical case data passed as template variables must be anonymized

### Medical Compliance (mandatory)

- ALL medical AI prompts MUST include a medical disclaimer
- Standard disclaimer:
  ```
  The following is AI-generated content for research and educational purposes only.
  It is not intended for diagnostic decisions without human clinical review.
  ```
- Never hardcode prompt strings in Java code — always external `.st` files
- Never embed patient identifiers in prompts

### Invalid Patterns

- Hardcoded prompt strings in Java: `new PromptTemplate("Analyze: " + input)`
- `StringBuilder`-based prompt construction
- Inline prompt text in service methods

## Boundaries
- Do NOT delete or modify medical disclaimers without human approval
- Do NOT reference patient data or PHI in prompt templates
- Do NOT switch from `.st` to another template format without human approval
