package com.charleezy.maya.controller;

import com.charleezy.maya.model.dto.NomiMessage;
import com.charleezy.maya.model.dto.NomiResponse;
import com.charleezy.maya.service.NomiService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/nomi")
@RequiredArgsConstructor
@Profile("local")
public class NomiController {
    private final NomiService nomiService;

    @PostMapping("/{nomiId}/chat")
    public ResponseEntity<NomiResponse> sendMessage(
            @PathVariable String nomiId,
            @RequestBody NomiMessage message) {
        return ResponseEntity.ok(nomiService.sendMessage(nomiId, message));
    }
} 