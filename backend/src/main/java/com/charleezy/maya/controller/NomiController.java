package com.charleezy.maya.controller;

import com.charleezy.maya.model.dto.NomiMessage;
import com.charleezy.maya.model.dto.NomiResponse;
import com.charleezy.maya.service.NomiService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for direct Nomi API testing - FOR LOCAL DEVELOPMENT ONLY
 * This controller is only available when:
 * 1. The 'local' profile is active
 * 2. The 'nomi.controller.enabled' property is set to true
 */
@RestController
@RequestMapping("/api/v1/nomi")
@RequiredArgsConstructor
@Profile("local")
@ConditionalOnProperty(name = "nomi.controller.enabled", havingValue = "true")
public class NomiController {
    private final NomiService nomiService;

    @PostMapping("/{nomiId}/chat")
    public ResponseEntity<NomiResponse> sendMessage(
            @PathVariable String nomiId,
            @RequestBody NomiMessage message) {
        return ResponseEntity.ok(nomiService.sendMessage(nomiId, message));
    }
} 