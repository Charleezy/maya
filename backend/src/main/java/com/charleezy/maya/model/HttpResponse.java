package com.charleezy.maya.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponse {
    private String body;
    private int statusCode;
    private String error;
} 