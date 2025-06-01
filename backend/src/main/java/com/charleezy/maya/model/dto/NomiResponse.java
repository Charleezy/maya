package com.charleezy.maya.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NomiResponse {
    private Message sentMessage;
    private Message replyMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String uuid;
        private String text;
        private Instant sent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Nomi {
        private String uuid;
        private String gender;
        private String name;
        private Instant created;
        private String relationshipType;
    }
} 