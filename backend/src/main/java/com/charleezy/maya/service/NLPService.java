package com.charleezy.maya.service;

import com.google.cloud.language.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Natural Language Processing service for extracting structured information from text commands.
 * 
 * TODO: Enhancement Roadmap
 * 1. Timer Management
 *    - Implement timer creation and management system
 *    - Add timer status tracking (active, completed, cancelled)
 *    - Handle timer notifications/alerts
 * 
 * 2. Complex Time Expressions
 *    - Support multiple time units (e.g., "1 hour and 30 minutes")
 *    - Add recurring timer support (e.g., "every 2 hours")
 *    - Handle specific end times (e.g., "until 5 PM")
 *    - Support relative time expressions (e.g., "after lunch")
 * 
 * 3. Duration Processing
 *    - Add validation for duration values
 *    - Normalize durations to standard time units
 *    - Handle fuzzy time expressions (e.g., "a few minutes")
 *    - Convert text durations to actual timestamps
 * 
 * 4. Task Context
 *    - Add priority levels for tasks
 *    - Support task categories/tags
 *    - Handle task dependencies or sequences
 */
@Slf4j
@Service
public class NLPService {

    public record EntityInfo(String name, String type, float salience) {}

    private static final Set<String> TEMPORAL_WORDS = Set.of(
        "tomorrow", "today", "yesterday",
        "am", "pm", "morning", "afternoon", "evening", "night"
    );

    private static final Set<String> TIME_UNITS = Set.of(
        "second", "seconds", "minute", "minutes",
        "hour", "hours", "day", "days",
        "week", "weeks", "month", "months",
        "year", "years"
    );

    private static final Set<String> COMMAND_WORDS = Set.of(
        "timer", "remind", "reminder", "alarm", "set", "create"
    );

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:in\\s+)?(\\d+)\\s*(second|seconds|minute|minutes|hour|hours|day|days|week|weeks|month|months|year|years)"
    );

    public List<EntityInfo> analyzeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.error("Received empty or null text");
            throw new IllegalArgumentException("Text cannot be empty");
        }

        log.info("Analyzing text: {}", text);
        
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();

            // Entity Analysis
            AnalyzeEntitiesRequest entityRequest = AnalyzeEntitiesRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF8)
                    .build();

            // Syntax Analysis
            AnalyzeSyntaxRequest syntaxRequest = AnalyzeSyntaxRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF8)
                    .build();

            AnalyzeEntitiesResponse entityResponse = language.analyzeEntities(entityRequest);
            AnalyzeSyntaxResponse syntaxResponse = language.analyzeSyntax(syntaxRequest);

            Set<EntityInfo> entities = new LinkedHashSet<>();
            
            // Check if this is a timer command
            boolean isTimerCommand = text.toLowerCase().contains("timer") || 
                                   text.toLowerCase().contains("remind") ||
                                   text.toLowerCase().contains("alarm");

            // Extract task description first if it's a timer command
            if (isTimerCommand) {
                String taskDescription = extractTaskDescription(text, syntaxResponse.getTokensList());
                if (taskDescription != null) {
                    // Find the highest salience for task-related entities
                    float maxSalience = entityResponse.getEntitiesList().stream()
                        .filter(e -> e.getName().toLowerCase().contains(taskDescription.toLowerCase()) ||
                                   taskDescription.toLowerCase().contains(e.getName().toLowerCase()))
                        .map(Entity::getSalience)
                        .max(Float::compare)
                        .orElse(0.5f);

                    entities.add(new EntityInfo(
                        taskDescription,
                        "TASK",
                        maxSalience
                    ));
                    log.info("Found task: {}", taskDescription);
                }
            }

            // Process standard entities (skip if they're part of the task or command words)
            for (Entity entity : entityResponse.getEntitiesList()) {
                String entityName = entity.getName().toLowerCase();
                // Skip numbers, task-related entities, and command words
                if (!entity.getType().name().equals("NUMBER") && 
                    !COMMAND_WORDS.contains(entityName) &&
                    (entities.isEmpty() || entities.stream()
                        .noneMatch(e -> e.type().equals("TASK") && 
                                 (e.name().toLowerCase().contains(entityName) ||
                                  entityName.contains(e.name().toLowerCase()))))) {
                    
                    entities.add(new EntityInfo(
                        entity.getName(),
                        entity.getType().name(),
                        entity.getSalience()
                    ));
                    log.info("Found entity: {} ({})", entity.getName(), entity.getType());
                }
            }

            // Process temporal expressions
            List<Token> tokens = syntaxResponse.getTokensList();
            Set<String> processedTemporals = new HashSet<>();
            
            for (int i = 0; i < tokens.size(); i++) {
                if (isStartOfTemporalExpression(tokens, i)) {
                    String temporalExpression = extractTemporalExpression(tokens, i);
                    if (temporalExpression != null && !processedTemporals.contains(temporalExpression)) {
                        String type = "TEMPORAL";
                        // Check for duration patterns including "in X hours" format
                        if (isTimerCommand && (isDuration(temporalExpression) || 
                            isDuration("in " + temporalExpression))) {
                            type = "DURATION";
                        }
                        entities.add(new EntityInfo(
                            temporalExpression,
                            type,
                            0.5f
                        ));
                        processedTemporals.add(temporalExpression);
                        log.info("Found temporal expression: {} ({})", temporalExpression, type);
                        i += temporalExpression.split("\\s+").length - 1;
                    }
                }
            }

            return new ArrayList<>(entities);
        } catch (IOException e) {
            log.error("Failed to create LanguageServiceClient", e);
            throw new RuntimeException("Failed to initialize NLP service", e);
        } catch (Exception e) {
            log.error("Unexpected error during text analysis", e);
            throw new RuntimeException("Failed to analyze text: " + e.getMessage(), e);
        }
    }

    private boolean isDuration(String text) {
        return DURATION_PATTERN.matcher(text).matches();
    }

    private String extractTaskDescription(String text, List<Token> tokens) {
        String lowerText = text.toLowerCase();
        
        // Find the starting point for task description
        int startIndex = -1;
        String[] startMarkers = {"to", "and", "."};
        
        // First try to find task after explicit markers
        for (String marker : startMarkers) {
            int idx = lowerText.indexOf(marker);
            while (idx != -1) {
                // Check if this marker is not part of another word
                if ((idx == 0 || !Character.isLetterOrDigit(lowerText.charAt(idx - 1))) &&
                    (idx + marker.length() >= lowerText.length() || 
                     !Character.isLetterOrDigit(lowerText.charAt(idx + marker.length())))) {
                    startIndex = idx + marker.length();
                    break;
                }
                idx = lowerText.indexOf(marker, idx + 1);
            }
            if (startIndex != -1) break;
        }
        
        // If no explicit marker found, look for position after duration
        if (startIndex == -1) {
            for (String unit : TIME_UNITS) {
                int idx = lowerText.indexOf(unit);
                if (idx != -1) {
                    startIndex = idx + unit.length();
                    break;
                }
            }
        }
        
        if (startIndex != -1) {
            String taskPart = text.substring(startIndex).trim();
            // Clean up the task description
            taskPart = taskPart.replaceAll("^[\\s.,]+", "").trim(); // Remove leading punctuation
            return taskPart.isEmpty() ? null : taskPart;
        }
        
        return null;
    }

    private boolean isStartOfTemporalExpression(List<Token> tokens, int index) {
        if (index >= tokens.size()) return false;
        
        Token token = tokens.get(index);
        String text = token.getText().getContent().toLowerCase();
        String tag = token.getPartOfSpeech().getTag().name();
        
        // Check if this token starts a temporal expression
        return TEMPORAL_WORDS.contains(text) ||
               (tag.equals("NUM") && hasTemporalContext(tokens, index)) ||
               TIME_UNITS.contains(text);
    }

    private boolean hasTemporalContext(List<Token> tokens, int index) {
        // Look ahead for temporal indicators
        for (int i = index + 1; i < tokens.size() && i < index + 3; i++) {
            String nextText = tokens.get(i).getText().getContent().toLowerCase();
            if (TEMPORAL_WORDS.contains(nextText) || TIME_UNITS.contains(nextText)) {
                return true;
            }
        }
        // Look behind for temporal indicators
        for (int i = index - 1; i >= 0 && i > index - 3; i--) {
            String prevText = tokens.get(i).getText().getContent().toLowerCase();
            if (prevText.equals("at") || prevText.equals("in")) {
                return true;
            }
        }
        return false;
    }

    private String extractTemporalExpression(List<Token> tokens, int startIndex) {
        StringBuilder expression = new StringBuilder();
        int i = startIndex;
        int maxLookAhead = Math.min(startIndex + 4, tokens.size());
        
        // Look behind for "in" preposition
        if (startIndex > 0) {
            Token prevToken = tokens.get(startIndex - 1);
            String prevText = prevToken.getText().getContent().toLowerCase();
            if (prevText.equals("in")) {
                expression.append(prevText).append(" ");
                // Include any additional prepositions
                if (startIndex > 1) {
                    Token prePrevToken = tokens.get(startIndex - 2);
                    String prePrevText = prePrevToken.getText().getContent().toLowerCase();
                    if (prePrevText.equals("for") || prePrevText.equals("at")) {
                        expression.insert(0, prePrevText + " ");
                    }
                }
            }
        }
        
        while (i < maxLookAhead) {
            Token token = tokens.get(i);
            String text = token.getText().getContent();
            String tag = token.getPartOfSpeech().getTag().name();
            
            if (tag.equals("NUM") || 
                TEMPORAL_WORDS.contains(text.toLowerCase()) ||
                TIME_UNITS.contains(text.toLowerCase())) {
                
                if (expression.length() > 0 && !expression.toString().endsWith(" ")) {
                    expression.append(" ");
                }
                expression.append(text);
                i++;
                continue;
            }
            
            // Stop if we hit a non-temporal word (except for certain connectors)
            if (!text.equalsIgnoreCase("at") && 
                !text.equalsIgnoreCase("in") && 
                !text.equals(":")) {
                break;
            }
            
            if (expression.length() > 0) {
                expression.append(" ");
            }
            expression.append(text);
            i++;
        }
        
        String result = expression.toString().trim();
        return result.isEmpty() ? null : result;
    }
} 