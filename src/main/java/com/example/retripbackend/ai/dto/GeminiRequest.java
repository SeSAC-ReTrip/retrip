package com.example.retripbackend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {
    
    private List<Content> contents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
        
        @JsonProperty("inline_data")
        private InlineData inlineData;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineData {
        @JsonProperty("mime_type")
        private String mimeType;
        private String data;
    }
}

