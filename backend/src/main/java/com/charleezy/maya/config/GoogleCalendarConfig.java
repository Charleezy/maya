package com.charleezy.maya.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google.calendar")
public class GoogleCalendarConfig {
    private String applicationName = "Maya AI Task Scheduler";
    private String credentialsPath = "/credentials.json";
    private String tokensDirectoryPath = "tokens";
    private String[] scopes = {"https://www.googleapis.com/auth/calendar"};
} 