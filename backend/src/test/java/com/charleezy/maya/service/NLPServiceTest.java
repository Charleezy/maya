package com.charleezy.maya.service;

import com.google.cloud.language.v1.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NLPServiceTest {

    @Mock
    private LanguageServiceClient languageServiceClient;

    private NLPService nlpService;

    @BeforeEach
    void setUp() {
        nlpService = new NLPService(languageServiceClient);
    }

    @Test
    void analyzeText_TimerCommand_ExtractsTaskAndDuration() throws IOException {
        // Given
        String input = "Set a timer for 25 minutes to reply to emails";
        
        // Mock Google NLP responses
        Entity taskEntity = Entity.newBuilder()
            .setName("reply to emails")
            .setType(Entity.Type.OTHER)
            .setSalience(0.8f)
            .build();
        
        Entity timerEntity = Entity.newBuilder()
            .setName("timer")
            .setType(Entity.Type.OTHER)
            .setSalience(0.2f)
            .build();

        AnalyzeEntitiesResponse entityResponse = AnalyzeEntitiesResponse.newBuilder()
            .addEntities(taskEntity)
            .addEntities(timerEntity)
            .build();

        // Create tokens for "25 minutes"
        Token numberToken = Token.newBuilder()
            .setText(TextSpan.newBuilder().setContent("25").build())
            .setPartOfSpeech(PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.NUM).build())
            .build();
        
        Token minutesToken = Token.newBuilder()
            .setText(TextSpan.newBuilder().setContent("minutes").build())
            .setPartOfSpeech(PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.NOUN).build())
            .build();

        AnalyzeSyntaxResponse syntaxResponse = AnalyzeSyntaxResponse.newBuilder()
            .addTokens(numberToken)
            .addTokens(minutesToken)
            .build();

        when(languageServiceClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(entityResponse);
        when(languageServiceClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(syntaxResponse);

        // When
        List<NLPService.EntityInfo> result = nlpService.analyzeText(input);

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
    void analyzeText_ReminderCommand_ExtractsTaskAndDuration() throws IOException {
        // Given
        String input = "Remind me in 1 hour to check the project status";
        
        // Mock Google NLP responses
        Entity taskEntity = Entity.newBuilder()
            .setName("project status")
            .setType(Entity.Type.OTHER)
            .setSalience(1.0f)
            .build();

        AnalyzeEntitiesResponse entityResponse = AnalyzeEntitiesResponse.newBuilder()
            .addEntities(taskEntity)
            .build();

        // Create tokens for "in 1 hour"
        Token inToken = Token.newBuilder()
            .setText(TextSpan.newBuilder().setContent("in").build())
            .setPartOfSpeech(PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.ADP).build())
            .build();
            
        Token numberToken = Token.newBuilder()
            .setText(TextSpan.newBuilder().setContent("1").build())
            .setPartOfSpeech(PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.NUM).build())
            .build();
        
        Token hourToken = Token.newBuilder()
            .setText(TextSpan.newBuilder().setContent("hour").build())
            .setPartOfSpeech(PartOfSpeech.newBuilder().setTag(PartOfSpeech.Tag.NOUN).build())
            .build();

        AnalyzeSyntaxResponse syntaxResponse = AnalyzeSyntaxResponse.newBuilder()
            .addTokens(inToken)
            .addTokens(numberToken)
            .addTokens(hourToken)
            .build();

        when(languageServiceClient.analyzeEntities(any(AnalyzeEntitiesRequest.class))).thenReturn(entityResponse);
        when(languageServiceClient.analyzeSyntax(any(AnalyzeSyntaxRequest.class))).thenReturn(syntaxResponse);

        // When
        List<NLPService.EntityInfo> result = nlpService.analyzeText(input);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("TASK");
            assertThat(entity.name()).isEqualTo("check the project status");
        });
        assertThat(result).anySatisfy(entity -> {
            assertThat(entity.type()).isEqualTo("DURATION");
            assertThat(entity.name()).isEqualTo("1 hour");
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