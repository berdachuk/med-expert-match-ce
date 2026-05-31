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
- Use Spring AI **resource-backed** `PromptTemplate` beans (not raw strings in Java). Reference: [Using resources instead of raw Strings](https://docs.spring.io/spring-ai/reference/2.0/api/prompt.html#_using_resources_instead_of_raw_strings)
- Template file organization (existing examples):
  ```
  prompts/analysis.st        — Case analysis prompts
  prompts/matching.st        — Doctor matching prompts
  prompts/routing.st         — Facility routing prompts
  prompts/recommendation.st  — Recommendation prompts
  ```

### Creating a New Prompt

1. Create `prompts/{purpose}.st` with StringTemplate syntax (`<variableName>` placeholders — project uses `<`/`>` delimiters via `StTemplateRenderer`)
2. Declare a `@Value("classpath:/prompts/{purpose}.st") Resource` field and a `PromptTemplate` bean in `core/config/PromptTemplateConfig.java`:
   ```java
   @Value("classpath:/prompts/matching.st")
   private Resource matchingResource;

   @Bean
   @Qualifier("matchingPromptTemplate")
   PromptTemplate matchingPromptTemplate(StTemplateRenderer renderer) {
       return PromptTemplate.builder()
               .renderer(renderer)
               .resource(matchingResource)
               .build();
   }
   ```
3. Inject via constructor using `@Qualifier`:
   ```java
   public MatchingServiceImpl(
       @Qualifier("matchingPromptTemplate") PromptTemplate matchingPrompt
   ) { ... }
   ```
4. Render at runtime: `matchingPrompt.render(Map.of("caseId", caseId))`

### Preferred vs. Allowed Patterns

| Pattern | Status |
|---------|--------|
| `.st` file + `PromptTemplate.builder().resource(...)` bean | **Required** for new prompts |
| `@Value("classpath:/prompts/foo.st") Resource` + `PromptTemplate` | **Preferred** wiring style |
| `new PromptTemplate(new ClassPathResource(...))` in service code | Avoid — register bean in config instead |
| `PromptTemplate.builder().template("...")` inline text block | Avoid — use `.resource(...)` |
| Dynamic data sections (case fields, tool results) assembled in Java | OK — pass as template variables, not prompt prose |

### Template Variables

- Use project StringTemplate syntax: `<variableName>` for substitutions (configured in `PromptTemplateConfig.stTemplateRenderer()`)
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

- Hardcoded prompt strings in Java: `new PromptTemplate("Analyze: " + input)` or text blocks in service methods
- `StringBuilder`-based prompt prose construction (assemble dynamic **data** in Java, keep wording in `.st` files)
- Inline `new PromptTemplate(new ClassPathResource(...))` in services — register beans in `PromptTemplateConfig` instead
- Inline prompt text in service methods

## Boundaries
- Do NOT delete or modify medical disclaimers without human approval
- Do NOT reference patient data or PHI in prompt templates
- Do NOT switch from `.st` to another template format without human approval
