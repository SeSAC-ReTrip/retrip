package com.example.retripbackend.ai.service;

import com.example.retripbackend.config.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 전체 spring 컨텍스트 로드
// 테스트용 설정
@TestPropertySource(properties = {
    "gemini.api.key=${GEMINI_API_KEY:}",
    "gemini.model.name=gemini-2.5-flash",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@DisplayName("GeminiService 테스트")   // 테스트 이름
class GeminiServiceTest {

    @Autowired
    private GeminiService geminiService;    // 테스트 대상 서비스

    @Autowired
    private GeminiConfig geminiConfig;  // API 키/모델 설정

    private final ObjectMapper objectMapper = new ObjectMapper();   // JSON 파싱용

    @BeforeEach
    void setUp() {
        // API 키가 설정되어 있는지 확인
        // null 이면 메세지 반환
        if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isEmpty()) {
            Assumptions.assumeTrue(
                false,
                "GEMINI_API_KEY 환경변수가 설정되지 않아 테스트를 건너뜁니다."
            );
        }
    }

    @Test
    @DisplayName("API 키가 없을 때 경고 메시지 반환")
    void analyzeReceipt_WhenApiKeyIsNull_ReturnsErrorMessage() {
        // given
        String originalApiKey = geminiConfig.getApiKey();
        try {
            // Reflection을 사용하여 API 키를 null로 설정
            java.lang.reflect.Field field = GeminiConfig.class.getDeclaredField("apiKey");
            field.setAccessible(true);
            field.set(geminiConfig, null);

            String base64Image = "testImageBase64";

            // when
            String result = geminiService.analyzeReceipt(base64Image);

            // then
            assertThat(result).contains("AI 서비스가 설정되지 않았습니다");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 원래 값으로 복원
            try {
                java.lang.reflect.Field field = GeminiConfig.class.getDeclaredField("apiKey");
                field.setAccessible(true);
                field.set(geminiConfig, originalApiKey);
            } catch (Exception e) {
                // 복원 실패는 무시
            }
        }
    }

    @Test
    @DisplayName("정상적인 영수증 분석 성공 - JapanReceipt 이미지 사용")
    void analyzeReceipt_WithJapanReceipt_ReturnsValidJson() throws IOException {
        // given
        Path imagePath = Paths.get("src/test/resources/testImage/JapanReceipt.png");
        
        // 테스트 이미지가 없으면 스킵
        if (!Files.exists(imagePath)) {
            Assumptions.assumeTrue(
                false,
                "테스트 이미지 파일이 없어 테스트를 건너뜁니다: " + imagePath
            );
        }

        byte[] imageBytes = Files.readAllBytes(imagePath);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        // when
        String result = geminiService.analyzeReceipt(base64Image);

        // Gemini API 응답 출력
        System.out.println("========================================");
        System.out.println("Gemini API 응답:");
        System.out.println("========================================");
        System.out.println(result);
        System.out.println("========================================");
        System.out.println("응답 길이: " + result.length() + " 문자");
        System.out.println("========================================");

        // then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        
        // 에러 메시지가 아닌지 확인
        assertThat(result).doesNotContain("AI 응답 생성 중 오류가 발생했습니다");
        assertThat(result).doesNotContain("AI 서비스가 설정되지 않았습니다");
        
        // JSON 형식 검증 (응답이 JSON인지 확인)
        try {
            // JSON 시작 부분 찾기 (마크다운 코드 블록 제거)
            String jsonString = result.trim();
            if (jsonString.startsWith("```json")) {
                jsonString = jsonString.substring(7);
            }
            if (jsonString.startsWith("```")) {
                jsonString = jsonString.substring(3);
            }
            if (jsonString.endsWith("```")) {
                jsonString = jsonString.substring(0, jsonString.length() - 3);
            }
            jsonString = jsonString.trim();
            
            System.out.println("정제된 JSON 문자열:");
            System.out.println(jsonString);
            System.out.println("========================================");
            
            // JSON 파싱
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            assertThat(jsonNode).isNotNull();
            
            // 필수 필드 검증
            assertThat(jsonNode.has("placeName")).as("placeName 필드 존재").isTrue();
            assertThat(jsonNode.has("amount")).as("amount 필드 존재").isTrue();
            assertThat(jsonNode.has("currency")).as("currency 필드 존재").isTrue();
            assertThat(jsonNode.has("paidAt")).as("paidAt 필드 존재").isTrue();
            assertThat(jsonNode.has("address")).as("address 필드 존재").isTrue();
            
            // 필드 값 검증
            assertThat(jsonNode.get("placeName").asText()).isNotEmpty();
            assertThat(jsonNode.get("amount").asDouble()).isGreaterThan(0);
            assertThat(jsonNode.get("currency").asText()).matches("[A-Z]{3}"); // ISO 4217 형식
            assertThat(jsonNode.get("address").asText()).isNotEmpty();
            
            // 파싱된 JSON 출력
            System.out.println("파싱된 JSON 내용:");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));
            System.out.println("========================================");
            
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            // JSON 파싱 실패 시 응답 내용 출력
            System.err.println("JSON 파싱 실패. 응답 내용:");
            System.err.println(result);
            throw new AssertionError("응답이 유효한 JSON 형식이 아닙니다: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("API 호출 실패 시 에러 메시지 반환")
    void analyzeReceipt_WhenApiCallFails_ReturnsErrorMessage() {
        // given
        String originalApiKey = geminiConfig.getApiKey();
        try {
            // 잘못된 API 키로 설정하여 에러 발생시키기
            java.lang.reflect.Field field = GeminiConfig.class.getDeclaredField("apiKey");
            field.setAccessible(true);
            field.set(geminiConfig, "invalid-api-key");

            String base64Image = "testImageBase64";

            // when
            String result = geminiService.analyzeReceipt(base64Image);

            // then
            assertThat(result).contains("AI 응답 생성 중 오류가 발생했습니다");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 원래 값으로 복원
            try {
                java.lang.reflect.Field field = GeminiConfig.class.getDeclaredField("apiKey");
                field.setAccessible(true);
                field.set(geminiConfig, originalApiKey);
            } catch (Exception e) {
                // 복원 실패는 무시
            }
        }
    }
}

