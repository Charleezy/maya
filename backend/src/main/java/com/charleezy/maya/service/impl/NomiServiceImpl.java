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
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NomiServiceImpl implements NomiService {
    private final NomiConfig nomiConfig;
    private final WebClient webClient;

    /*TODO can't be blocking, need to use reactive programming */
    @Override
    public NomiResponse sendMessage(String nomiId, NomiMessage message) {
        return webClient.post()
                .uri("/nomis/{id}/chat", nomiId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(NomiResponse.class)
                .block();
    }

    @Override
    public List<NomiResponse.Nomi> listNomis() {
        return webClient.get()
                .uri("/nomis")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<NomiResponse.Nomi>>>() {})
                .map(response -> response.get("nomis"))
                .block();
    }

    @Override
    public NomiResponse.Nomi getNomi(String nomiId) {
        return webClient.get()
                .uri("/nomis/{id}", nomiId)
                .retrieve()
                .bodyToMono(NomiResponse.Nomi.class)
                .block();
    }
} 