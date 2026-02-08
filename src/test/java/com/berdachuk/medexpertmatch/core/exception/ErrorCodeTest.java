package com.berdachuk.medexpertmatch.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorCode Enum Tests")
class ErrorCodeTest {

    @Test
    @DisplayName("Should have all expected error codes")
    void shouldHaveAllExpectedErrorCodes() {
        int expectedCount = 28;
        assertThat(ErrorCode.values()).hasSize(expectedCount);
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("All error codes should have non-empty code and message")
    void allErrorCodesShouldHaveNonEmptyCodeAndMessage(ErrorCode errorCode) {
        assertThat(errorCode.getCode()).isNotBlank();
        assertThat(errorCode.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("Should generate full message with context")
    void shouldGenerateFullMessageWithContext() {
        ErrorCode errorCode = ErrorCode.TOOL_CALL_FAILED;
        String context = "Failed to call tool: get_doctor_clinical_experience";

        String fullMessage = errorCode.getFullMessage(context);

        assertThat(fullMessage).contains(errorCode.getCode());
        assertThat(fullMessage).contains(errorCode.getMessage());
        assertThat(fullMessage).contains(context);
    }

    @Test
    @DisplayName("Should generate full message without context")
    void shouldGenerateFullMessageWithoutContext() {
        ErrorCode errorCode = ErrorCode.TOOL_CALL_FAILED;

        String fullMessage = errorCode.getFullMessage(null);

        assertThat(fullMessage).isEqualTo(errorCode.getCode() + ": " + errorCode.getMessage());
        assertThat(fullMessage).doesNotContain("null");
    }

    @Test
    @DisplayName("Should generate full message with blank context")
    void shouldGenerateFullMessageWithBlankContext() {
        ErrorCode errorCode = ErrorCode.TOOL_CALL_FAILED;

        String fullMessage = errorCode.getFullMessage("   ");

        assertThat(fullMessage).isEqualTo(errorCode.getCode() + ": " + errorCode.getMessage());
    }

    @Test
    @DisplayName("All tool calling errors should have TOOL prefix")
    void allToolCallingErrorsShouldHaveToolPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().startsWith("TOOL")) {
                assertThat(errorCode.getCode()).startsWith("TOOL");
            }
        }
    }

    @Test
    @DisplayName("All graph query errors should have GRAPH prefix")
    void allGraphQueryErrorsShouldHaveGraphPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("GRAPH")) {
                assertThat(errorCode.getCode()).startsWith("GRAPH");
            }
        }
    }

    @Test
    @DisplayName("All database errors should have DATABASE or DATA prefix")
    void allDatabaseErrorsShouldHaveDatabasePrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("DATABASE") || errorCode.name().contains("DATA")) {
                boolean startsWithDatabase = errorCode.getCode().startsWith("DATABASE");
                boolean startsWithData = errorCode.getCode().startsWith("DATA");
                assertThat(startsWithDatabase || startsWithData).isTrue();
            }
        }
    }

    @Test
    @DisplayName("All embedding errors should have EMBEDDING prefix")
    void allEmbeddingErrorsShouldHaveEmbeddingPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("EMBEDDING")) {
                assertThat(errorCode.getCode()).startsWith("EMBEDDING");
            }
        }
    }

    @Test
    @DisplayName("All validation errors should have VALIDATION prefix")
    void allValidationErrorsShouldHaveValidationPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("VALIDATION")) {
                assertThat(errorCode.getCode()).startsWith("VALIDATION");
            }
        }
    }

    @Test
    @DisplayName("All configuration errors should have CONFIG prefix")
    void allConfigurationErrorsShouldHaveConfigPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("CONFIG")) {
                assertThat(errorCode.getCode()).startsWith("CONFIG");
            }
        }
    }

    @Test
    @DisplayName("All network errors should have NETWORK prefix")
    void allNetworkErrorsShouldHaveNetworkPrefix() {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.name().contains("NETWORK")) {
                assertThat(errorCode.getCode()).startsWith("NETWORK");
            }
        }
    }

    @Test
    @DisplayName("Should have unique error codes")
    void shouldHaveUniqueErrorCodes() {
        long uniqueCodes = java.util.Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .distinct()
                .count();

        assertThat(uniqueCodes).isEqualTo(ErrorCode.values().length);
    }

    @Test
    @DisplayName("Should have unique error messages")
    void shouldHaveUniqueErrorMessages() {
        long uniqueMessages = java.util.Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getMessage)
                .distinct()
                .count();

        assertThat(uniqueMessages).isEqualTo(ErrorCode.values().length);
    }

    @Test
    @DisplayName("Tool calling errors should include expected codes")
    void toolCallingErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.TOOL_CALL_FAILED).isNotNull();
        assertThat(ErrorCode.TOOL_EMPTY_RESPONSE).isNotNull();
        assertThat(ErrorCode.TOOL_INVALID_RESPONSE).isNotNull();
        assertThat(ErrorCode.TOOL_REFUSED).isNotNull();
        assertThat(ErrorCode.TOOL_TIMEOUT).isNotNull();
    }

    @Test
    @DisplayName("Graph query errors should include expected codes")
    void graphQueryErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.GRAPH_QUERY_FAILED).isNotNull();
        assertThat(ErrorCode.GRAPH_NOT_EXISTS).isNotNull();
        assertThat(ErrorCode.GRAPH_CONNECTION_ERROR).isNotNull();
        assertThat(ErrorCode.GRAPH_INVALID_QUERY).isNotNull();
    }

    @Test
    @DisplayName("Database errors should include expected codes")
    void databaseErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.DATABASE_CONNECTION_FAILED).isNotNull();
        assertThat(ErrorCode.DATABASE_QUERY_FAILED).isNotNull();
        assertThat(ErrorCode.DATABASE_CONSTRAINT_VIOLATION).isNotNull();
        assertThat(ErrorCode.DATA_NOT_FOUND).isNotNull();
    }

    @Test
    @DisplayName("Embedding errors should include expected codes")
    void embeddingErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.EMBEDDING_GENERATION_FAILED).isNotNull();
        assertThat(ErrorCode.EMBEDDING_SERVICE_UNAVAILABLE).isNotNull();
        assertThat(ErrorCode.EMBEDDING_TIMEOUT).isNotNull();
    }

    @Test
    @DisplayName("Validation errors should include expected codes")
    void validationErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.VALIDATION_FAILED).isNotNull();
        assertThat(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD).isNotNull();
        assertThat(ErrorCode.VALIDATION_INVALID_FORMAT).isNotNull();
        assertThat(ErrorCode.VALIDATION_OUT_OF_RANGE).isNotNull();
    }

    @Test
    @DisplayName("Configuration errors should include expected codes")
    void configurationErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.CONFIG_INVALID).isNotNull();
        assertThat(ErrorCode.CONFIG_MISSING_REQUIRED).isNotNull();
        assertThat(ErrorCode.CONFIG_INVALID_VALUE).isNotNull();
    }

    @Test
    @DisplayName("Network errors should include expected codes")
    void networkErrorsShouldIncludeExpectedCodes() {
        assertThat(ErrorCode.NETWORK_CONNECTION_FAILED).isNotNull();
        assertThat(ErrorCode.NETWORK_TIMEOUT).isNotNull();
        assertThat(ErrorCode.NETWORK_UNREACHABLE).isNotNull();
    }

    @Test
    @DisplayName("Should override toString correctly")
    void shouldOverrideToStringCorrectly() {
        ErrorCode errorCode = ErrorCode.TOOL_CALL_FAILED;

        String toString = errorCode.toString();

        assertThat(toString).isEqualTo(errorCode.getCode() + ": " + errorCode.getMessage());
    }

    @Test
    @DisplayName("Should have ERROR_INVALID_CATEGORY and ERROR_MISSING_CONTEXT for error handling")
    void shouldHaveErrorHandlingCodes() {
        assertThat(ErrorCode.ERROR_INVALID_CATEGORY).isNotNull();
        assertThat(ErrorCode.ERROR_MISSING_CONTEXT).isNotNull();
    }

    @Test
    @DisplayName("Full message context should be properly formatted")
    void fullMessageContextShouldBeProperlyFormatted() {
        ErrorCode errorCode = ErrorCode.TOOL_CALL_FAILED;
        String context = "Additional context here";

        String fullMessage = errorCode.getFullMessage(context);

        assertThat(fullMessage).matches("^.+: .+ - .+$");
    }
}