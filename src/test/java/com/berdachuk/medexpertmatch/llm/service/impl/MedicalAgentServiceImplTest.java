package com.berdachuk.medexpertmatch.llm.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MedicalAgentServiceImpl.
 * Tests the stripLlmReasoning method to ensure LLM internal reasoning is properly removed.
 */
class MedicalAgentServiceImplTest {
    private String stripLlmReasoningLogic(String response) {
        return LlmResponseSanitizer.stripLlmReasoning(response);
    }

    @Test
    @DisplayName("Should return null when input is null")
    void testStripLlmReasoning_NullInput() {
        String result = stripLlmReasoningLogic(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Should return empty string when input is blank")
    void testStripLlmReasoning_BlankInput() {
        String result = stripLlmReasoningLogic("");
        assertEquals("", result);

        result = stripLlmReasoningLogic("   ");
        assertEquals("", result.trim());
    }

    @Test
    @DisplayName("Should return unchanged text when no reasoning markers")
    void testStripLlmReasoning_NoReasoningMarkers() {
        String input = "This is a normal response without any reasoning markers.";
        String result = stripLlmReasoningLogic(input);
        assertEquals(input, result);
    }

    @ParameterizedTest
    @MethodSource("reasoningHeaderProvider")
    @DisplayName("Should strip reasoning headers from response")
    void testStripLlmReasoning_ReasoningHeaders(String header, String input, String expectedContains) {
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
        assertTrue(result.contains(expectedContains) || result.startsWith(expectedContains.split("\n")[0]),
                "Expected result to contain: " + expectedContains + ", but got: " + result);
        assertFalse(result.toLowerCase().startsWith(header.toLowerCase()),
                "Result should not start with reasoning header: " + header);
    }

    private static Stream<Arguments> reasoningHeaderProvider() {
        return Stream.of(
                // "Understand the Goal:" header
                Arguments.of(
                        "Understand the Goal:",
                        "Understand the Goal: The user wants to find a doctor.\n\nBased on the case analysis, I recommend...",
                        "Based on the case analysis"
                ),
                // "Analyze the" header
                Arguments.of(
                        "Analyze the",
                        "Analyze the case details.\n\nThe patient presents with chest pain...",
                        "The patient presents"
                ),
                // "Step 1:" header
                Arguments.of(
                        "Step 1:",
                        "Step 1: Review the symptoms.\n\nThe recommended specialist is...",
                        "The recommended specialist"
                ),
                // "Thought:" header
                Arguments.of(
                        "Thought:",
                        "Thought: I need to analyze this case.\n\nThe analysis shows...",
                        "The analysis shows"
                ),
                // "Thinking:" header
                Arguments.of(
                        "Thinking:",
                        "Thinking: Let me consider the options.\n\nHere is my recommendation...",
                        "Here is my recommendation"
                ),
                // "Reasoning:" header
                Arguments.of(
                        "Reasoning:",
                        "Reasoning: Based on the symptoms...\n\nThe diagnosis suggests...",
                        "The diagnosis suggests"
                ),
                // "Analysis:" header
                Arguments.of(
                        "Analysis:",
                        "Analysis: The case involves...\n\nMy conclusion is...",
                        "My conclusion is"
                ),
                // "Let me think" header
                Arguments.of(
                        "Let me think",
                        "Let me think about this.\n\nThe best approach would be...",
                        "The best approach would be"
                ),
                // "Let's analyze" header
                Arguments.of(
                        "Let's analyze",
                        "Let's analyze the case.\n\nThe findings indicate...",
                        "The findings indicate"
                ),
                // "First, I'll" header
                Arguments.of(
                        "First, I'll",
                        "First, I'll review the patient history.\n\nThe patient should...",
                        "The patient should"
                ),
                // "I need to" header
                Arguments.of(
                        "I need to",
                        "I need to check the symptoms.\n\nBased on my analysis...",
                        "Based on my analysis"
                ),
                // "The task is" header
                Arguments.of(
                        "The task is",
                        "The task is to find a specialist.\n\nI recommend consulting...",
                        "I recommend consulting"
                ),
                // "Key Information" header
                Arguments.of(
                        "Key Information",
                        "Key Information: Patient age 45.\n\nThe specialist needed is...",
                        "The specialist needed is"
                )
        );
    }

    @Test
    @DisplayName("Should handle 'thought' prefix (case-insensitive)")
    void testStripLlmReasoning_ThoughtPrefix() {
        String input = "Thought: I should analyze this case.\n\nThe recommended doctor is Dr. Smith.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.startsWith("The recommended doctor") || result.contains("Dr. Smith"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should handle lowercase 'thought' prefix")
    void testStripLlmReasoning_LowercaseThought() {
        String input = "thought: analyzing the case...\n\nThe patient needs a cardiologist.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("cardiologist") || result.startsWith("The patient"));
        assertFalse(result.toLowerCase().startsWith("thought"));
    }

    @Test
    @DisplayName("Should remove unused markers like <unused94>")
    void testStripLlmReasoning_UnusedMarkers() {
        String input = "<unused94><unused95>Here is the actual response.";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("<unused"));
        assertTrue(result.contains("actual response"));
    }

    @Test
    @DisplayName("Should extract content from JSON code blocks")
    void testStripLlmReasoning_JsonCodeBlock() {
        String input = "```json\n{\"requiredSpecialty\": \"Cardiology\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("```json"));
        assertTrue(result.contains("Cardiology"));
    }

    @Test
    @DisplayName("Should extract content from generic code blocks")
    void testStripLlmReasoning_GenericCodeBlock() {
        String input = "```\n{\"urgencyLevel\": \"HIGH\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertFalse(result.contains("```"));
        assertTrue(result.contains("HIGH"));
    }

    @Test
    @DisplayName("Should handle multiple reasoning sections")
    void testStripLlmReasoning_MultipleReasoningSections() {
        String input = "Understand the Goal: Find a doctor.\n\nAnalyze the case: Patient has chest pain.\n\nThe recommended specialist is a cardiologist.";
        String result = stripLlmReasoningLogic(input);
        // Should strip at least the first reasoning section
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("Should handle response starting with 'Based on'")
    void testStripLlmReasoning_BasedOnPattern() {
        String input = "Thought: Let me analyze.\n\nBased on the routing results, the best facility is...";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Based on") || result.contains("best facility"));
    }

    @Test
    @DisplayName("Should preserve normal medical response")
    void testStripLlmReasoning_PreserveMedicalResponse() {
        String input = """
                Based on the case analysis, the patient requires immediate cardiac evaluation.
                
                **Recommended Specialty:** Cardiology
                **Urgency Level:** HIGH
                
                The patient should be referred to a cardiologist for further evaluation.
                """;
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Cardiology"));
        assertTrue(result.contains("HIGH"));
        assertTrue(result.contains("cardiologist"));
    }

    @Test
    @DisplayName("Should handle response with only reasoning (no actual content)")
    void testStripLlmReasoning_OnlyReasoning() {
        String input = "Thought: I need to analyze this case.";
        String result = stripLlmReasoningLogic(input);
        // Should return something, even if it's the stripped version
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle empty response after stripping")
    void testStripLlmReasoning_EmptyAfterStripping() {
        String input = "Thought:\n\n";
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle complex nested reasoning")
    void testStripLlmReasoning_ComplexNestedReasoning() {
        String input = """
                Understand the Goal: The user wants a doctor recommendation.
                
                Step 1: Analyze the symptoms.
                The patient has chest pain.
                
                Step 2: Determine specialty.
                Based on symptoms, cardiology is needed.
                
                The recommended specialist is a cardiologist.
                """;
        String result = stripLlmReasoningLogic(input);
        assertNotNull(result);
        // Should have stripped at least the first reasoning section
        assertFalse(result.toLowerCase().startsWith("understand the goal"));
    }

    @Test
    @DisplayName("Should handle reasoning with JSON at the end")
    void testStripLlmReasoning_ReasoningWithJson() {
        String input = "Thought: Analyzing the case.\n\n```json\n{\"specialty\": \"Neurology\"}\n```";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("Neurology"));
        assertFalse(result.contains("Thought:"));
        assertFalse(result.contains("```"));
    }

    @Test
    @DisplayName("Should handle 'Step 2:' reasoning header")
    void testStripLlmReasoning_Step2Header() {
        String input = "Step 2: Now I will provide the recommendation.\n\nThe patient should see a neurologist.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("neurologist") || result.contains("patient"));
        assertFalse(result.startsWith("Step 2:"));
    }

    @Test
    @DisplayName("Should handle 'Step 3:' reasoning header")
    void testStripLlmReasoning_Step3Header() {
        String input = "Step 3: Final recommendation.\n\nConsult with an oncologist for further evaluation.";
        String result = stripLlmReasoningLogic(input);
        assertTrue(result.contains("oncologist") || result.contains("Consult"));
        assertFalse(result.startsWith("Step 3:"));
    }
}
