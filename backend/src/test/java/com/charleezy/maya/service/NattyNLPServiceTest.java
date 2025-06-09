package com.charleezy.maya.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NattyNLPServiceTest {

    private NattyNLPService nlpService;

    @BeforeEach
    void setUp() {
        nlpService = new NattyNLPService();
    }

    @Test
    void analyzeText_TimerCommand_ExtractsTaskAndDuration() {
        // Given
        String input = "Set a timer for 25 minutes to reply to emails";

        // When
        List<AbstractNLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TASK");
            assertThat(entity.name()).isEqualTo("reply to emails");
        });
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("DURATION");
            assertThat(entity.name()).isEqualTo("25 minutes");
        });
    }

    @Test
    void analyzeText_ReminderCommand_ExtractsTaskAndDuration() {
        // Given
        String input = "Remind me in 1 hour to check the project status";

        // When
        List<AbstractNLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TASK");
            assertThat(entity.name()).isEqualTo("check the project status");
        });
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("DURATION");
            assertThat(entity.name()).isEqualTo("in 1 hour");
        });
    }

    @ParameterizedTest
    @CsvSource({
        "'Set a reminder for tomorrow at 2pm to call mom', TEMPORAL, tomorrow at 2pm",
        "'Remind me next Monday at 9am to review reports', TEMPORAL, next Monday at 9am",
        "'Set a timer for 30 minutes to take a break', DURATION, 30 minutes",
        "'Remind me in 2 hours and 30 minutes to check email', DURATION, in 2 hours and 30 minutes",
        "'Set an alarm for every Monday at 10am to team meeting', TEMPORAL, every Monday at 10am"
    })
    void analyzeText_VariousTimeFormats_ExtractsCorrectly(String input, String expectedType, String expectedExpression) {
        // When
        List<AbstractNLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).anyMatch(entity -> 
            entity.type().equals(expectedType) && 
            entity.name().toLowerCase().contains(expectedExpression.toLowerCase())
        );
    }

    @Test
    void analyzeText_ComplexCommand_ExtractsMultipleEntities() {
        // Given
        String input = "Set a reminder for next Friday between 2pm and 4pm to have a team meeting and prepare presentation";

        // When
        List<AbstractNLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TASK");
            assertThat(entity.name()).isEqualTo("have a team meeting and prepare presentation");
        });
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TEMPORAL");
            assertThat(entity.name().toLowerCase()).contains("next friday");
            assertThat(entity.name().toLowerCase()).contains("between 2pm and 4pm");
        });
    }

    @Test
    void analyzeText_RecurringEvent_ExtractsRecurrenceInfo() {
        // Given
        String input = "Set a reminder for every Tuesday at 3pm to review weekly metrics";

        // When
        List<AbstractNLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TASK");
            assertThat(entity.name()).isEqualTo("review weekly metrics");
        });
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TEMPORAL");
            assertThat(entity.name()).isEqualTo("every Tuesday at 3pm");
        });
    }

    @Test
    void analyzeText_EmptyInput_ThrowsException() {
        // Given
        String input = "";

        // When/Then
        assertThatThrownBy(() -> nlpService.analyzeText(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Text cannot be empty");
    }
}
