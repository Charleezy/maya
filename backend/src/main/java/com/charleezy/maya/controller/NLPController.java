package com.charleezy.maya.controller;

import com.charleezy.maya.service.NLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
public class NLPController {

    private final NLPService nlpService;

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