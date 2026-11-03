package com.boss.ai.validator;

import com.boss.ai.dto.ChatResponseDto;
import com.boss.ai.dto.RecommendationDto;
import com.boss.common.exception.BossException;
import com.boss.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Guards the contract between {@code AiClient} and the frontend: a malformed or
 * out-of-range response never reaches the browser, no matter which provider produced
 * it (section 15). This is where a real LLM's occasional malformed output gets caught.
 */
@Component
public class ResponseValidator {

    public ChatResponseDto validate(ChatResponseDto response) {
        if (response == null || response.summary() == null || response.summary().isBlank()) {
            throw new AiResponseValidationException("AI response was missing a summary");
        }
        for (RecommendationDto recommendation : response.recommendations()) {
            if (recommendation.confidence() < 0.0 || recommendation.confidence() > 1.0) {
                throw new AiResponseValidationException("Recommendation confidence out of range: "
                        + recommendation.confidence());
            }
        }
        return response;
    }

    public static final class AiResponseValidationException extends BossException {
        public AiResponseValidationException(String message) {
            super(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
    }
}
