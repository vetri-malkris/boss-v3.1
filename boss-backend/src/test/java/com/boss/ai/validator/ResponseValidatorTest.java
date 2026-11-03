package com.boss.ai.validator;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.dto.RecommendationDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseValidatorTest {

    private final ResponseValidator validator = new ResponseValidator();

    @Test
    void acceptsAWellFormedResponse() {
        ChatResponseDto response = new ChatResponseDto("Summary", List.of(), null, List.of());

        assertThat(validator.validate(response)).isSameAs(response);
    }

    @Test
    void rejectsAMissingSummary() {
        ChatResponseDto response = new ChatResponseDto("", List.of(), null, List.of());

        assertThatThrownBy(() -> validator.validate(response))
                .isInstanceOf(ResponseValidator.AiResponseValidationException.class);
    }

    @Test
    void rejectsAnOutOfRangeConfidence() {
        RecommendationDto recommendation = new RecommendationDto("do X", "outcome", 1.5, List.of(), List.of(), List.of());
        ChatResponseDto response = new ChatResponseDto("Summary", List.of(), null, List.of(recommendation));

        assertThatThrownBy(() -> validator.validate(response))
                .isInstanceOf(ResponseValidator.AiResponseValidationException.class);
    }
}
