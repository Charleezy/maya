package com.charleezy.maya.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final NomiConfig nomiConfig;

    @Bean
    public WebClient nomiWebClient() {
        return WebClient.builder()
                .baseUrl(nomiConfig.getBaseUrl())
                .defaultHeader("Authorization", nomiConfig.getApiKey())
                .build();
    }
} 