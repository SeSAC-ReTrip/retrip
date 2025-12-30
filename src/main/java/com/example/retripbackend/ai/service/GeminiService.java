package com.example.retripbackend.ai.service;

import com.example.retripbackend.ai.dto.GeminiRequest;
import com.example.retripbackend.ai.dto.GeminiResponse;
import com.example.retripbackend.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient webClient;
    private final GeminiConfig geminiConfig;

    /**
     * 영수증 이미지를 분석하여 JSON 형식의 구조화된 정보 추출
     * @param receiptImageBase64 영수증 이미지 (Base64 인코딩)
     * @return 영수증 분석 결과 (JSON 문자열)
     */
    public String analyzeReceipt(String receiptImageBase64) {
        try {
            // API 키 확인
            if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isEmpty()) {
                log.warn("Gemini API 키가 설정되지 않았습니다.");
                return "AI 서비스가 설정되지 않았습니다. API 키를 확인해주세요.";
            }

            // 요청 본문 구성 (프롬프트 + 이미지)
            GeminiRequest request = buildRequest(receiptImageBase64);

            // API 호출
            String url = String.format("/v1/models/%s:generateContent?key=%s",
                geminiConfig.getModelName(),
                geminiConfig.getApiKey());

            GeminiResponse response = webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> {
                        if (throwable instanceof WebClientResponseException ex) {
                            return ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
                        }
                        return false;
                    })
                    .doBeforeRetry(retrySignal -> {
                        log.warn("429 Too Many Requests 발생. 재시도 중... (시도 횟수: {})",
                            retrySignal.totalRetries() + 1);
                    }))
                .block();

            // 응답에서 텍스트 추출
            if (response != null &&
                response.getCandidates() != null &&
                !response.getCandidates().isEmpty()) {

                String result = response.getCandidates().get(0)
                    .getContent()
                    .getParts()
                    .get(0)
                    .getText();

                log.debug("Gemini 응답 생성 완료");
                return result;
            }

            return "응답을 받을 수 없습니다.";

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: ", e);
            return "AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * Gemini API 요청 본문 구성 (프롬프트 + 이미지)
     * @param receiptImageBase64 영수증 이미지 (Base64 인코딩)
     * @return Gemini API 요청 객체
     */
    private GeminiRequest buildRequest(String receiptImageBase64) {
        List<GeminiRequest.Part> parts = new ArrayList<>();

        // 프롬프트 추가 (이미지 분석 지시사항)
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
               - 영수증에 주소가 없으면 placeName과 영수증의 도시/국가 정보를 결합하여 주소를 유추
               - 예: "Starbucks Tokyo" + 영수증의 도시 정보 → "Tokyo, Japan" 또는 더 상세한 주소
            
            선택 정보 (가능한 경우에만):
            6. latitude: 위도 (주소 또는 placeName을 기반으로 추정 가능한 경우)
               - 영수증에 주소가 있으면 주소를 기반으로 추정
               - 주소가 없으면 placeName과 도시/국가 정보를 결합하여 추정
            7. longitude: 경도 (주소 또는 placeName을 기반으로 추정 가능한 경우)
               - 영수증에 주소가 있으면 주소를 기반으로 추정
               - 주소가 없으면 placeName과 도시/국가 정보를 결합하여 추정
            
            언어 규칙:
            - 영수증이 한국 영수증(한국어로 작성된 영수증, 통화가 KRW인 경우 등)이면 모든 텍스트 필드를 한글로 응답
            - 한국 영수증이 아닌 경우(해외 영수증)에는 모든 텍스트 필드를 영어로 응답
            - placeName, address 필드는 위 언어 규칙에 따라 응답하되, 원본 영수증의 언어도 고려하여 적절히 변환
            
            주의사항:
            - placeName은 영수증 상단의 상호명이나 가게명을 우선적으로 추출
            - 한국 영수증이면 한글로, 해외 영수증이면 영어로 placeName을 제공
            - amount는 소수점을 포함하여 정확히 추출 (예: 12.50, 9.99, 1500.00)
            - currency는 반드시 ISO 4217 통화 코드 형식으로 추출 (3자리 영문 대문자)
            - paidAt은 영수증의 날짜 형식을 파악하여 ISO 8601 형식으로 변환
            - address는 가능한 한 상세하게 추출 (도로명, 도시명, 국가명 포함)
            - 영수증에 주소가 없어도 placeName과 영수증의 도시/국가 정보를 활용하여 주소를 유추해주세요
            - latitude와 longitude는 주소 또는 placeName을 기반으로 추정 가능한 경우 반드시 포함
            - placeName만으로도 해당 가게의 대략적인 위치(도시 중심지 등)를 추정하여 좌표를 제공해주세요
            - 해외 영수증의 다양한 형식과 언어를 지원해야 함
            
            JSON 형식:
            {
              "placeName": "장소명",
              "amount": 12.50,
              "currency": "USD",
              "paidAt": "2024-01-15T14:30:00",
              "address": "주소",
              "latitude": 위도값_또는_null,
              "longitude": 경도값_또는_null
            }
            
            반드시 유효한 JSON 형식으로만 응답해주세요. 다른 설명이나 텍스트는 포함하지 마세요.
            """;

        // 텍스트 프롬프트 추가
        parts.add(GeminiRequest.Part.builder()
            .text(prompt)
            .build());

        // 이미지 추가
        if (receiptImageBase64 != null && !receiptImageBase64.isEmpty()) {
            parts.add(GeminiRequest.Part.builder()
                .inlineData(GeminiRequest.InlineData.builder()
                    .mimeType("image/jpeg")
                    .data(receiptImageBase64)
                    .build())
                .build());
        }

        GeminiRequest.Content content = GeminiRequest.Content.builder()
            .parts(parts)
            .build();

        return GeminiRequest.builder()
            .contents(List.of(content))
            .build();
    }
}