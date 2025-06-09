package com.charleezy.maya.service.impl;

import com.charleezy.maya.config.NomiConfig;
import com.charleezy.maya.model.dto.NomiMessage;
import com.charleezy.maya.model.dto.NomiResponse;
import com.charleezy.maya.service.NomiService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NomiServiceImpl implements NomiService {
    private final NomiConfig nomiConfig;
    @Qualifier("nomiWebClient")
    private final WebClient nomiWebClient;

    @Override
    public NomiResponse sendMessage(String nomiId, NomiMessage message) {
        return nomiWebClient.post()
            .uri(nomiConfig.getBaseUrl() + "/chat/completions")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-API-KEY", nomiConfig.getApiKey())
            .bodyValue(Map.of(
                "messages", List.of(Map.of(
                    "role", "user",
                    "content", message.getMessageText()
                )),
                "model", "gpt-3.5-turbo",
                "temperature", 0.7,
                "max_tokens", 150
            ))
            .retrieve()
            .bodyToMono(NomiResponse.class)
            .block();
    }

    @Override
    public List<NomiResponse.Nomi> listNomis() {
        return nomiWebClient.get()
            .uri("/nomis")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<NomiResponse.Nomi>>() {})
            .block();
    }

    @Override
    public NomiResponse.Nomi getNomi(String nomiId) {
        return nomiWebClient.get()
            .uri("/nomis/{id}", nomiId)
            .retrieve()
            .bodyToMono(NomiResponse.Nomi.class)
            .block();
    }
} 