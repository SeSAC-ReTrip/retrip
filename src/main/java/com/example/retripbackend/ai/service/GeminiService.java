package com.example.retripbackend.ai.service;

import com.example.retripbackend.config.GeminiConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 영수증 이미지를 분석하여 JSON 형식의 구조화된 정보 추출
     * @param receiptImageBase64 영수증 이미지 (Base64 인코딩)
     * @param address 영수증에 기재된 주소 (선택, 이미지에서 추출 가능)
     * @return 영수증 분석 결과 (JSON 문자열)
     */
    public String analyzeReceipt(String receiptImageBase64, String address) {
        try {
            // API 키 확인
            if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isEmpty()) {
                log.warn("Gemini API 키가 설정되지 않았습니다.");
                return "AI 서비스가 설정되지 않았습니다. API 키를 확인해주세요.";
            }

            // 요청 URL 생성
            String url = String.format(
                "https://generativelanguage.googleapis.com/v1/models/%s:generateContent?key=%s",
                geminiConfig.getModelName(),
                geminiConfig.getApiKey()
            );

            // 요청 본문 구성
            Map<String, Object> requestBody = new HashMap<>();

            // contents 배열
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();

            // 텍스트 프롬프트
            String prompt = """
                다음 해외 여행 영수증 이미지를 분석하여 다음 정보를 JSON 형식으로 정확히 추출해주세요:
                
                필수 정보:
                1. placeName: 영수증에 기재된 상호명, 가게명, 또는 장소명 (예: "Starbucks Tokyo", "McDonald's Paris", "7-Eleven Bangkok")
                2. amount: 영수증의 총 결제 금액 (소수점 포함 가능, 숫자만 추출)
                   - 예: $12.50 → 12.50, €9.99 → 9.99, ¥1,500 → 1500, £25.75 → 25.75
                   - 통화 기호나 쉼표는 제거하고 숫자만 추출
                3. currency: 통화 코드 (USD, EUR, JPY, CNY, GBP, THB, KRW, SGD, HKD 등)
                   - 영수증에 표시된 통화 기호나 코드를 ISO 4217 형식으로 추출
                   - 예: $ → USD, € → EUR, ¥ → JPY, £ → GBP, ฿ → THB
                4. paidAt: 결제 일시 (ISO 8601 형식: "yyyy-MM-ddTHH:mm:ss" 또는 "yyyy-MM-dd")
                   - 영수증에 기재된 날짜와 시간을 정확히 추출
                   - 시간이 없으면 날짜만 추출 (예: "2024-01-15")
                   - 다양한 날짜 형식 지원 (MM/DD/YYYY, DD/MM/YYYY, YYYY-MM-DD 등)
                5. address: 영수증에 기재된 주소 (전체 주소 또는 가능한 상세 주소)
                   - 해외 주소 형식 지원 (영문 주소, 현지 언어 주소 등)
                
                선택 정보 (가능한 경우에만):
                6. latitude: 위도 (주소를 기반으로 추정 가능한 경우)
                7. longitude: 경도 (주소를 기반으로 추정 가능한 경우)
                8. expenseItems: 소비 항목 리스트 (각 항목의 itemName, amount, quantity)
                   - amount는 소수점 포함 가능 (예: 12.50, 9.99)
                
                주의사항:
                - placeName은 영수증 상단의 상호명이나 가게명을 우선적으로 추출 (영문 또는 현지 언어)
                - amount는 소수점을 포함하여 정확히 추출 (예: 12.50, 9.99, 1500.00)
                - currency는 반드시 ISO 4217 통화 코드 형식으로 추출 (3자리 영문 대문자)
                - paidAt은 영수증의 날짜 형식을 파악하여 ISO 8601 형식으로 변환
                - address는 가능한 한 상세하게 추출 (도로명, 도시명, 국가명 포함)
                - latitude와 longitude는 주소가 명확한 경우에만 포함 (없으면 null)
                - 해외 영수증의 다양한 형식과 언어를 지원해야 함
                
                JSON 형식:
                {
                  "placeName": "장소명",
                  "amount": 12.50,
                  "currency": "USD",
                  "paidAt": "2024-01-15T14:30:00",
                  "address": "주소",
                  "latitude": 위도값_또는_null,
                  "longitude": 경도값_또는_null,
                  "expenseItems": [
                    {
                      "itemName": "항목명",
                      "amount": 12.50,
                      "quantity": 1
                    }
                  ]
                }
                
                반드시 유효한 JSON 형식으로만 응답해주세요. 다른 설명이나 텍스트는 포함하지 마세요.
                """;

            part.put("text", prompt);

            // 이미지가 있는 경우 추가
            if (receiptImageBase64 != null && !receiptImageBase64.isEmpty()) {
                Map<String, Object> inlineData = new HashMap<>();
                inlineData.put("mime_type", "image/jpeg");
                inlineData.put("data", receiptImageBase64);
                part.put("inline_data", inlineData);
            }

            content.put("parts", new Object[] { part });
            requestBody.put("contents", new Object[] { content });

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // HTTP 요청 생성
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            // 응답 파싱
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String result = jsonNode
                .path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

            log.debug("Gemini 응답 생성 완료");
            return result;

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: ", e);
            return "AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}