package com.charleezy.maya.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "duckling")
public class DucklingConfig {
    private String baseUrl = "http://localhost:8000";
    private String parseEndpoint = "/parse";

    @Bean
    public WebClient ducklingWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
} 