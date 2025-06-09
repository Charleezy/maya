package com.charleezy.maya.controller;

import com.charleezy.maya.service.DucklingNLPService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
@Profile("local")  // Only active in local profile
@ConditionalOnProperty(name = "nlp.controller.enabled", havingValue = "true")  // Only active if explicitly enabled
public class NLPController {

    private final DucklingNLPService ducklingNLPService;

    @PostConstruct
    public void init() {
        log.info("NLP Controller initialized - DevTools tested 2");
    }

    /**
     * Test endpoint for Duckling NLP service - FOR LOCAL TESTING ONLY
     */
    @PostMapping("/duckling/test")
    public ResponseEntity<?> testDuckling(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Text field is required"));
            }

            text = URLEncoder.encode(text, StandardCharsets.UTF_8);
            log.info("Testing Duckling with text: {}", text);
            String response = ducklingNLPService.getDucklingResponse(text);
            return ResponseEntity.ok()
                .body(Map.of(
                    "body", response
                ));
        } catch (IOException | InterruptedException e) {
            log.error("Error calling Duckling service", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to call Duckling: " + e.getMessage()));
        }
    }
} 