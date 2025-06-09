package com.charleezy.maya.service;

import com.google.cloud.language.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@ConditionalOnProperty(name = "nlp.implementation", havingValue = "google", matchIfMissing = true)
public class GoogleCloudNLPService extends AbstractNLPService {

    private final LanguageServiceClient languageServiceClient;
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:in\\s+)?(\\d+)\\s*(second|seconds|minute|minutes|hour|hours|day|days|week|weeks|month|months|year|years)"
    );

    public GoogleCloudNLPService() throws IOException {
        this(LanguageServiceClient.create());
    }

    // Constructor for testing
    GoogleCloudNLPService(LanguageServiceClient languageServiceClient) {
        this.languageServiceClient = languageServiceClient;
    }

    @Override
    protected List<EntityInfo> performTextAnalysis(String text) {
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

        AnalyzeEntitiesResponse entityResponse = languageServiceClient.analyzeEntities(entityRequest);
        AnalyzeSyntaxResponse syntaxResponse = languageServiceClient.analyzeSyntax(syntaxRequest);

        Set<EntityInfo> entities = new LinkedHashSet<>();
        
        // Check if this is a command
        boolean isTimerCommand = isCommand(text);

        // Extract task description first if it's a command
        if (isTimerCommand) {
            String taskDescription = extractTaskDescription(text);
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
        List<String> temporalExpressions = extractTemporalExpressions(text);
        for (String expression : temporalExpressions) {
            String type = "TEMPORAL";
            // Check for duration patterns including "in X hours" format
            if (isTimerCommand && (isDuration(expression) || isDuration("in " + expression))) {
                type = "DURATION";
            }
            entities.add(new EntityInfo(expression, type, 0.5f));
            log.info("Found temporal expression: {} ({})", expression, type);
        }

        return new ArrayList<>(entities);
    }

    private boolean isDuration(String text) {
        return DURATION_PATTERN.matcher(text).matches();
    }

    @Override
    protected String extractTaskDescription(String text) {
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

    @Override
    protected List<String> extractTemporalExpressions(String text) {
        List<String> expressions = new ArrayList<>();
        List<Token> tokens = languageServiceClient.analyzeSyntax(
            AnalyzeSyntaxRequest.newBuilder()
                .setDocument(Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build())
                .setEncodingType(EncodingType.UTF8)
                .build()
        ).getTokensList();

        for (int i = 0; i < tokens.size(); i++) {
            if (isStartOfTemporalExpression(tokens, i)) {
                String temporalExpression = extractTemporalExpression(tokens, i);
                if (temporalExpression != null) {
                    expressions.add(temporalExpression);
                    i += temporalExpression.split("\\s+").length - 1;
                }
            }
        }
        
        return expressions;
    }

    private boolean isStartOfTemporalExpression(List<Token> tokens, int index) {
        if (index >= tokens.size()) return false;
        
        Token token = tokens.get(index);
        String text = token.getText().getContent().toLowerCase();
        String tag = token.getPartOfSpeech().getTag().name();
        
        // Check if this token starts a temporal expression
        return text.matches("\\d+") || // Numbers
               TIME_UNITS.contains(text) || // Time units
               text.matches("(?i)(today|tomorrow|yesterday|morning|afternoon|evening|night|noon|midnight|am|pm)");
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
                TIME_UNITS.contains(text.toLowerCase()) ||
                text.toLowerCase().matches("(?i)(today|tomorrow|yesterday|morning|afternoon|evening|night|noon|midnight|am|pm)")) {
                
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