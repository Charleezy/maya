package com.charleezy.maya.service;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Abstract base class for NLP services that process text commands and extract structured information.
 * This class provides common functionality and interfaces for different NLP implementations.
 */
@Slf4j
public abstract class AbstractNLPService {
    
    public record EntityInfo(String name, String type, float salience) {}

    protected static final Set<String> COMMAND_WORDS = Set.of(
        "timer", "remind", "reminder", "alarm", "set", "create"
    );

    protected static final Set<String> TIME_UNITS = Set.of(
        "second", "seconds", "minute", "minutes",
        "hour", "hours", "day", "days",
        "week", "weeks", "month", "months",
        "year", "years"
    );

    /**
     * Analyzes text input to extract structured information about tasks, times, and other entities.
     * @param text The input text to analyze
     * @return List of extracted entities with their types and salience scores
     * @throws IllegalArgumentException if the input text is null or empty
     */
    public List<EntityInfo> analyzeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.error("Received empty or null text");
            throw new IllegalArgumentException("Text cannot be empty");
        }

        log.info("Analyzing text: {}", text);
        
        try {
            return performTextAnalysis(text);
        } catch (Exception e) {
            log.error("Unexpected error during text analysis", e);
            throw new RuntimeException("Failed to analyze text: " + e.getMessage(), e);
        }
    }

    /**
     * Performs the actual text analysis. This method must be implemented by concrete subclasses
     * to provide their specific NLP implementation.
     * 
     * @param text The input text to analyze
     * @return List of extracted entities
     */
    protected abstract List<EntityInfo> performTextAnalysis(String text);

    /**
     * Extracts task description from the input text.
     * @param text The input text
     * @return The extracted task description, or null if none found
     */
    protected abstract String extractTaskDescription(String text);

    /**
     * Extracts temporal expressions (dates, times, durations) from the input text.
     * @param text The input text
     * @return List of temporal expressions found in the text
     */
    protected abstract List<String> extractTemporalExpressions(String text);

    /**
     * Determines if the input text represents a command (e.g., timer, reminder).
     * @param text The input text
     * @return true if the text contains a command word
     */
    protected boolean isCommand(String text) {
        String lowerText = text.toLowerCase();
        return COMMAND_WORDS.stream().anyMatch(lowerText::contains);
    }

    /**
     * Determines if the input text represents a timer command.
     * @param text The input text
     * @return true if the text is a timer command
     */
    protected boolean isTimerCommand(String text) {
        String lowerText = text.toLowerCase();
        return lowerText.contains("set a timer") || lowerText.contains("set timer");
    }
} 