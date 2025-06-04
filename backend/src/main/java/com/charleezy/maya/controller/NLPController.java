package com.charleezy.maya.controller;

import com.charleezy.maya.service.NLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
@Profile("local")  // Only active in local profile
@ConditionalOnProperty(name = "nlp.controller.enabled", havingValue = "true")  // Only active if explicitly enabled
public class NLPController {

    private final NLPService nlpService;

    @PostConstruct
    public void init() {
        log.info("NLP Controller initialized - DevTools tested 2");
    }

    /**
     * Direct NLP analysis endpoint - FOR LOCAL TESTING ONLY
     * WARNING: In production, NLP requests should go through a message queue
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeText(@RequestBody String text) {
        try {
            log.info("Received text analysis request");
            List<NLPService.EntityInfo> entities = nlpService.analyzeText(text);
            return ResponseEntity.ok(entities);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing request", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to analyze text: " + e.getMessage()));
        }
    }
} 