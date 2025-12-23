package com.example.retripbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 클래스를 Spring 설정 클래스로 표시
@Configuration
// application.properties의 gemini.* 속성을 자동으로 바인딩
@ConfigurationProperties(prefix = "gemini")
@Getter
@Setter
public class GeminiConfig {
    private String apiKey;
    private String modelName = "gemini-pro";

}
