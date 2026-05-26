package com.maritime.platform.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceIdNormalizer")
class TraceIdNormalizerTest {

    @Nested
    @DisplayName("valid client trace IDs")
    class Valid {

        @ParameterizedTest
        @ValueSource(strings = {
                "a",
                "abc123def",
                "trace-id-with-dashes",
                "trace.id.with.dots",
                "trace_id_with_underscores",
                "Alphanumeric.And-Special_chars"
        })
        @DisplayName("preserves valid trace IDs within character set and length")
        void preservesValidTraceId(String input) {
            String result = TraceIdNormalizer.normalize(input);

            assertThat(result).isEqualTo(input);
        }

        @Test
        @DisplayName("preserves trace ID at max length of 128")
        void preservesMaxLengthTraceId() {
            String input = "a".repeat(128);

            String result = TraceIdNormalizer.normalize(input);

            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("blank or null trace IDs")
    class Blank {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "\t", "\n"})
        @DisplayName("generates new UUID when input is blank or null")
        void generatesNewUuidForBlankInput(String input) {
            String result = TraceIdNormalizer.normalize(input);

            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result).doesNotContain("-");
            assertThat(result).hasSize(32);
            assertThat(result).matches("[a-f0-9]{32}");
        }
    }

    @Nested
    @DisplayName("exceeds max length")
    class TooLong {

        @Test
        @DisplayName("generates new UUID when input exceeds 128 characters")
        void generatesNewUuidForTooLongInput() {
            String longInput = "a".repeat(129);

            String result = TraceIdNormalizer.normalize(longInput);

            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result).doesNotContain("-");
            assertThat(result).hasSize(32);
            assertThat(result).matches("[a-f0-9]{32}");
        }
    }

    @Nested
    @DisplayName("illegal characters")
    class IllegalChars {

        @ParameterizedTest
        @ValueSource(strings = {
                "trace with spaces",
                "trace@symbol",
                "trace#hash",
                "trace$dollar",
                "trace%percent",
                "trace^caret",
                "trace&ampersand",
                "trace*star",
                "trace(paren)",
                "trace+plus",
                "trace=equals",
                "trace{bracket}",
                "trace[box]",
                "trace|pipe",
                "trace\\backslash",
                "trace:colon",
                "trace;semicolon",
                "trace\"quote",
                "trace'apostrophe",
                "trace<angle>",
                "trace?question",
                "trace/slash",
                "trace~tilde",
                "trace`backtick",
                "trace!exclamation",
                "trace,comma",
                "trace-control-char",
                "中文-trace-id"
        })
        @DisplayName("generates new UUID when input contains illegal characters")
        void generatesNewUuidForIllegalChars(String input) {
            String result = TraceIdNormalizer.normalize(input);

            assertThat(result).isNotNull().isNotEmpty();
            assertThat(result).doesNotContain("-");
            assertThat(result).hasSize(32);
            assertThat(result).matches("[a-f0-9]{32}");
        }
    }

    @Test
    @DisplayName("produces unique IDs for different invalid inputs")
    void producesUniqueIds() {
        String id1 = TraceIdNormalizer.normalize(null);
        String id2 = TraceIdNormalizer.normalize("");
        String id3 = TraceIdNormalizer.normalize("invalid chars!");

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
    }
}
