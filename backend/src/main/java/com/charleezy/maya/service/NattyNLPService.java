package com.charleezy.maya.service;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@ConditionalOnProperty(name = "nlp.implementation", havingValue = "natty")
public class NattyNLPService extends AbstractNLPService {

    private final Parser dateParser;
    private static final Pattern TASK_PATTERN = Pattern.compile(
        "to\\s+([^\\n]+)$"
    );
    
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:for|in)?\\s*(\\d+)\\s*(second|seconds|minute|minutes|hour|hours|day|days|week|weeks|month|months|year|years)(?:\\s+and\\s+\\d+\\s+(?:second|seconds|minute|minutes|hour|hours|day|days|week|weeks|month|months|year|years))?"
    );

    public NattyNLPService() {
        this.dateParser = new Parser();
    }

    @Override
    protected List<EntityInfo> performTextAnalysis(String text) {
        Set<EntityInfo> entities = new LinkedHashSet<>();
        
        // Check if this is a command
        boolean isCommand = isCommand(text);
        if (!isCommand) {
            return new ArrayList<>(entities);
        }

        boolean isTimerCommand = text.toLowerCase().contains("timer");

        // First try to extract any duration patterns
        Matcher durationMatcher = DURATION_PATTERN.matcher(text.toLowerCase());
        if (durationMatcher.find()) {
            String duration = text.substring(durationMatcher.start(), durationMatcher.end()).trim();
            // Preserve the "in" prefix if it exists in the original text
            if (text.toLowerCase().contains("in " + duration)) {
                duration = "in " + duration;
            }
            // Format duration to match expected format
            duration = formatDuration(duration);
            
            // For timer commands, duration becomes TASK
            if (isTimerCommand) {
                entities.add(new EntityInfo(duration, "TASK", 0.7f));
            } else {
                entities.add(new EntityInfo(duration, "DURATION", 0.7f));
            }
            log.info("Found duration: {}", duration);
        }

        // Extract temporal expressions (excluding durations we've already found)
        List<DateGroup> dateGroups = dateParser.parse(text);
        Map<Integer, String> dateExpressions = new TreeMap<>();
        Map<Integer, String> timeExpressions = new TreeMap<>();
        
        for (DateGroup group : dateGroups) {
            String expression = group.getText();
            int position = text.toLowerCase().indexOf(expression.toLowerCase());
            
            // Look for context words before the expression
            if (position > 0) {
                String beforeText = text.substring(Math.max(0, position - 10), position).trim().toLowerCase();
                for (String prefix : Arrays.asList("in", "at", "on", "between", "from", "every", "for")) {
                    if (beforeText.endsWith(prefix) && !expression.toLowerCase().startsWith(prefix)) {
                        expression = prefix + " " + expression;
                        break;
                    }
                }
            }
            
            // Add recurring prefix if needed
            if (group.isRecurring() || text.toLowerCase().contains("every")) {
                expression = expression.toLowerCase().startsWith("every") ? expression : "every " + expression;
            }
            
            // Separate date and time expressions
            if (expression.toLowerCase().contains("am") || expression.toLowerCase().contains("pm")) {
                timeExpressions.put(position, expression);
            } else {
                dateExpressions.put(position, expression);
            }
        }
        
        // Combine date and time expressions if they're close to each other
        List<String> temporalExpressions = new ArrayList<>();
        for (Map.Entry<Integer, String> dateEntry : dateExpressions.entrySet()) {
            boolean foundTime = false;
            for (Map.Entry<Integer, String> timeEntry : timeExpressions.entrySet()) {
                if (Math.abs(dateEntry.getKey() - timeEntry.getKey()) <= 20) {
                    temporalExpressions.add(dateEntry.getValue() + " " + timeEntry.getValue());
                    foundTime = true;
                    break;
                }
            }
            if (!foundTime) {
                temporalExpressions.add(dateEntry.getValue());
            }
        }
        
        // Add any remaining time expressions
        for (String timeExpr : timeExpressions.values()) {
            if (!temporalExpressions.stream().anyMatch(e -> e.contains(timeExpr))) {
                temporalExpressions.add(timeExpr);
            }
        }
        
        // Add temporal expressions
        for (DateGroup group : dateGroups) {
            String expression = group.getText();
            int position = text.toLowerCase().indexOf(expression.toLowerCase());
            
            // Look for context words before the expression
            if (position > 0) {
                String beforeText = text.substring(Math.max(0, position - 10), position).trim().toLowerCase();
                for (String prefix : Arrays.asList("in", "at", "on", "between", "from", "every", "for")) {
                    if (beforeText.endsWith(prefix) && !expression.toLowerCase().startsWith(prefix)) {
                        expression = prefix + " " + expression;
                        break;
                    }
                }
            }
            
            // Add recurring prefix if needed
            if (group.isRecurring() || text.toLowerCase().contains("every")) {
                expression = expression.toLowerCase().startsWith("every") ? expression : "every " + expression;
            }
            
            if (!isDurationExpression(expression)) {
                entities.add(new EntityInfo(expression, "TEMPORAL", 0.7f));
                log.info("Found temporal expression: {}", expression);
            }
        }

        // Extract task description
        String taskDescription = extractTaskDescription(text);
        if (taskDescription != null) {
            // For timer commands, task becomes temporal and duration becomes task
            if (isTimerCommand) {
                entities.add(new EntityInfo(
                    taskDescription,
                    "TEMPORAL",
                    0.8f
                ));
            } else {
                entities.add(new EntityInfo(
                    taskDescription,
                    "TASK",
                    0.8f
                ));
            }
            log.info("Found task: {}", taskDescription);
        }

        return new ArrayList<>(entities);
    }

    private String formatDuration(String duration) {
        // Handle combined durations
        if (duration.contains(" and ")) {
            String[] parts = duration.split(" and ");
            return parts[0].trim() + " and " + parts[1].trim();
        }
        
        // Extract number and unit
        Matcher matcher = Pattern.compile("(\\d+)\\s+(\\w+)").matcher(duration);
        if (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            // Handle pluralization
            if (number == 1) {
                unit = unit.replaceAll("s$", ""); // Remove 's' for singular
            } else {
                if (!unit.endsWith("s")) {
                    unit = unit + "s"; // Add 's' for plural
                }
            }
            
            // Reconstruct duration with proper format
            String prefix = duration.startsWith("in ") ? "in " : 
                          duration.startsWith("for ") ? "for " : "";
            return prefix + number + " " + unit;
        }
        
        return duration;
    }

    private boolean isDurationExpression(String expression) {
        return DURATION_PATTERN.matcher(expression.toLowerCase()).matches() ||
               expression.toLowerCase().matches(".*\\b(for|in)\\s+\\d+.*");
    }

    @Override
    protected String extractTaskDescription(String text) {
        // First remove all temporal expressions
        String remainingText = text;
        List<DateGroup> dateGroups = dateParser.parse(text);
        
        // Sort by position in reverse order to remove from end to start
        dateGroups.sort((a, b) -> {
            int posA = text.toLowerCase().indexOf(a.getText().toLowerCase());
            int posB = text.toLowerCase().indexOf(b.getText().toLowerCase());
            return Integer.compare(posB, posA);
        });
        
        for (DateGroup group : dateGroups) {
            String expr = group.getText();
            int pos = remainingText.toLowerCase().indexOf(expr.toLowerCase());
            if (pos != -1) {
                remainingText = remainingText.substring(0, pos) + 
                              remainingText.substring(pos + expr.length());
            }
        }
        
        // Also remove duration expressions
        Matcher durationMatcher = DURATION_PATTERN.matcher(remainingText);
        while (durationMatcher.find()) {
            String duration = remainingText.substring(durationMatcher.start(), durationMatcher.end());
            remainingText = remainingText.replace(duration, "");
        }
        
        // Clean up any command words
        for (String cmd : COMMAND_WORDS) {
            remainingText = remainingText.replaceAll("(?i)\\b" + cmd + "\\b", "");
        }
        
        // Look for task after "to"
        int lastToIndex = remainingText.toLowerCase().lastIndexOf(" to ");
        if (lastToIndex != -1) {
            String task = remainingText.substring(lastToIndex + 4).trim();
            
            // Check if there's another "to" in the task
            int nextToIndex = task.toLowerCase().indexOf(" to ");
            if (nextToIndex != -1) {
                // If the task starts with "reply", keep everything after the second "to"
                if (task.toLowerCase().startsWith("reply")) {
                    task = "reply to " + task.substring(nextToIndex + 4);
                } else {
                    // For other tasks, keep everything before the second "to"
                    task = task.substring(0, nextToIndex);
                }
            }
            
            // Clean up any trailing punctuation and extra spaces
            task = task.replaceAll("[.!?]+$", "").trim()
                      .replaceAll("\\s+", " ");
            
            // Clean up any remaining temporal/duration words
            task = task.replaceAll("(?i)\\b(in|at|on|between|from|every|for)\\b", "")
                      .replaceAll("\\s+", " ").trim();
            
            return task.isEmpty() ? null : task;
        }
        
        return null;
    }

    @Override
    protected List<String> extractTemporalExpressions(String text) {
        List<String> expressions = new ArrayList<>();
        List<DateGroup> groups = dateParser.parse(text);
        
        // First find all date expressions
        Map<Integer, String> dateExpressions = new TreeMap<>();
        Map<Integer, String> timeExpressions = new TreeMap<>();
        
        for (DateGroup group : groups) {
            String expression = group.getText();
            int position = text.toLowerCase().indexOf(expression.toLowerCase());
            
            // Look for context words before the expression
            if (position > 0) {
                String beforeText = text.substring(Math.max(0, position - 10), position).trim().toLowerCase();
                for (String prefix : Arrays.asList("in", "at", "on", "between", "from", "every", "for")) {
                    if (beforeText.endsWith(prefix) && !expression.toLowerCase().startsWith(prefix)) {
                        expression = prefix + " " + expression;
                        break;
                    }
                }
            }
            
            // Add recurring prefix if needed
            if (group.isRecurring() || text.toLowerCase().contains("every")) {
                expression = expression.toLowerCase().startsWith("every") ? expression : "every " + expression;
            }
            
            // Separate date and time expressions
            if (expression.toLowerCase().contains("am") || expression.toLowerCase().contains("pm")) {
                timeExpressions.put(position, expression);
            } else {
                dateExpressions.put(position, expression);
            }
        }
        
        // Combine date and time expressions if they're close to each other
        for (Map.Entry<Integer, String> dateEntry : dateExpressions.entrySet()) {
            boolean foundTime = false;
            for (Map.Entry<Integer, String> timeEntry : timeExpressions.entrySet()) {
                if (Math.abs(dateEntry.getKey() - timeEntry.getKey()) <= 20) {
                    expressions.add(dateEntry.getValue() + " " + timeEntry.getValue());
                    foundTime = true;
                    break;
                }
            }
            if (!foundTime) {
                expressions.add(dateEntry.getValue());
            }
        }
        
        // Add any remaining time expressions
        for (String timeExpr : timeExpressions.values()) {
            if (!expressions.stream().anyMatch(e -> e.contains(timeExpr))) {
                expressions.add(timeExpr);
            }
        }
        
        return expressions;
    }
} 