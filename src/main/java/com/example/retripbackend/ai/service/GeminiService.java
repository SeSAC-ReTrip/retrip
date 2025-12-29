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

//    javadoc
//    api를 자동으로 만들때 유용
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

            // 요청 본문 구성 (이미지만)
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
     * Gemini API 요청 본문 구성 (이미지만 포함)
     * @param receiptImageBase64 영수증 이미지 (Base64 인코딩)
     * @return Gemini API 요청 객체
     */
    private GeminiRequest buildRequest(String receiptImageBase64) {
        List<GeminiRequest.Part> parts = new ArrayList<>();

        // 이미지만 분석
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