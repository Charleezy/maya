package com.charleezy.maya.service;

import com.charleezy.maya.config.DucklingConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DucklingNLPServiceTest {

    private WireMockServer wireMockServer;
    private WebClient webClient;
    private DucklingNLPService nlpService;
    private ObjectMapper objectMapper;
    private DucklingConfig ducklingConfig;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        objectMapper = new ObjectMapper();
        ducklingConfig = new DucklingConfig();
        ducklingConfig.setBaseUrl("http://localhost:" + wireMockServer.port());
        
        webClient = WebClient.builder()
            .baseUrl(ducklingConfig.getBaseUrl())
            .build();
            
        nlpService = new DucklingNLPService(webClient, ducklingConfig);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void analyzeText_TimerCommand_ExtractsTaskAndDuration() {
        // Given
        String input = "Set a timer for 25 minutes to reply to emails";
        
        // Mock Duckling response
        ArrayNode mockResponse = objectMapper.createArrayNode();
        ObjectNode timeEntity = objectMapper.createObjectNode()
            .put("dim", "time")
            .put("start", 15)
            .put("end", 25);
        timeEntity.putObject("value")
            .put("type", "duration")
            .put("normalized", "25 minutes");
        mockResponse.add(timeEntity);

        // Setup WireMock stub
        stubFor(post(urlEqualTo("/parse"))
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(mockResponse.toString())));

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
        
        // Mock Duckling response
        ArrayNode mockResponse = objectMapper.createArrayNode();
        ObjectNode timeEntity = objectMapper.createObjectNode()
            .put("dim", "time")
            .put("start", 12)
            .put("end", 19);
        timeEntity.putObject("value")
            .put("type", "duration")
            .put("normalized", "1 hour");
        mockResponse.add(timeEntity);

        // Setup WireMock stub
        stubFor(post(urlEqualTo("/parse"))
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(mockResponse.toString())));

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

    @Test
    @Disabled("TODO: Fix handling of complex commands with time ranges and type swapping")
    void analyzeText_ComplexCommand_ExtractsMultipleEntities() {
        // Given
        String input = "Set a reminder for next Friday between 2pm and 4pm to have a team meeting and prepare presentation";
        
        // Mock Duckling response
        ArrayNode mockResponse = objectMapper.createArrayNode();
        ObjectNode timeEntity = objectMapper.createObjectNode()
            .put("dim", "time")
            .put("start", 19)
            .put("end", 46);
        timeEntity.putObject("value")
            .put("type", "interval")
            .put("from", "next Friday 2pm")
            .put("to", "next Friday 4pm");
        mockResponse.add(timeEntity);

        // Setup WireMock stub
        stubFor(post(urlEqualTo("/parse"))
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(mockResponse.toString())));

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
            assertThat(entity.name()).isEqualTo("next Friday between 2pm and 4pm");
        });
    }

    @Test
    void analyzeText_RecurringEvent_ExtractsRecurrenceInfo() {
        // Given
        String input = "Set a reminder for every Tuesday at 3pm to review weekly metrics";
        
        // Mock Duckling response
        ArrayNode mockResponse = objectMapper.createArrayNode();
        ObjectNode timeEntity = objectMapper.createObjectNode()
            .put("dim", "time")
            .put("start", 19)
            .put("end", 37);
        ObjectNode value = timeEntity.putObject("value")
            .put("type", "time")
            .put("grain", "week");
        ArrayNode values = value.putArray("values");
        values.addObject().put("value", "Tuesday 3pm");
        values.addObject().put("value", "next Tuesday 3pm");
        mockResponse.add(timeEntity);

        // Setup WireMock stub
        stubFor(post(urlEqualTo("/parse"))
            .willReturn(aResponse()
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(mockResponse.toString())));

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