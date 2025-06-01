package com.charleezy.maya.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "nomi")
public class NomiConfig {
    private String apiKey;
    private String baseUrl = "https://api.nomi.ai/v1";
    private int messageTimeout = 30; // seconds, based on Nomi's docs
} 