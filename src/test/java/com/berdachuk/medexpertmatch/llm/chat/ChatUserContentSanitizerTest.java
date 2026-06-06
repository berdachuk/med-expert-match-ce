package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatUserContentSanitizerTest {

    @Test
    void shouldOmitThoughtOnlyAbstractFromPastedCaseBlock() {
        String pasted = """
                Find Specialist Case Information
                Case ID: 6a23f05200155d711484cf69
                Type: CONSULT_REQUEST
                Abstract: thought The user wants a clinical case summary based on the provided data for embedding generation and specialist matching.""";

        String sanitized = ChatUserContentSanitizer.sanitize(pasted);

        assertTrue(sanitized.contains("Case ID: 6a23f05200155d711484cf69"));
        assertFalse(sanitized.toLowerCase().contains("thought"));
        assertFalse(sanitized.toLowerCase().contains("embedding generation"));
    }

    @Test
    void shouldStripChainOfThoughtFromPastedCaseAbstract() {
        String pasted = """
                Find Specialist Case Information
                Case ID: 6a23f05200155d711484cf64
                Type: CONSULT_REQUEST
                Abstract: thought The user wants a clinical case summary
                Mental Sandbox: Attempt 4...
                A 77-year-old patient presents with Chronic Ischemic Heart Disease (I25.9). The patient's chief complaint includes a cough.""";

        String sanitized = ChatUserContentSanitizer.sanitize(pasted);

        assertTrue(sanitized.contains("Case ID: 6a23f05200155d711484cf64"));
        assertFalse(sanitized.toLowerCase().contains("mental sandbox"));
        assertTrue(sanitized.contains("77-year-old"));
    }

    @Test
    void shouldLeaveSimpleMatchRequestUnchanged() {
        String request = "Find specialist for case 6a23f05200155d711484cf64";
        assertEquals(request, ChatUserContentSanitizer.sanitize(request));
    }

}
