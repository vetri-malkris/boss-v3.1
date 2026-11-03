package com.boss.ai.orchestration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntentDetectorTest {

    private final IntentDetector detector = new IntentDetector();

    @Test
    void detectsProfitIntent() {
        assertThat(detector.detect("Why did profit decrease?")).isEqualTo(Intent.PROFIT);
    }

    @Test
    void detectsRunwayIntent() {
        assertThat(detector.detect("How many months of cash do we have left?")).isEqualTo(Intent.RUNWAY);
    }

    @Test
    void detectsRisksIntentOverGeneral() {
        assertThat(detector.detect("Are there any risks I should worry about?")).isEqualTo(Intent.RISKS);
    }

    @Test
    void fallsBackToGeneralForUnrecognizedQuestions() {
        assertThat(detector.detect("What's the weather like today?")).isEqualTo(Intent.GENERAL);
    }
}
