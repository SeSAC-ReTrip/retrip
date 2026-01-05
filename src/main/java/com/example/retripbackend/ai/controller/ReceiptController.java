package com.example.retripbackend.ai.controller;

import com.example.retripbackend.ai.service.GeminiService;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/receipt")
@RequiredArgsConstructor
public class ReceiptController {

    private final GeminiService geminiService;

    /**
     * 영수증 분석 페이지
     */
    @GetMapping("/analyze")
    public String analyzePage() {
        return "receipt/analyze";
    }

    /**
     * 영수증 분석 처리
     * @param file 업로드된 영수증 이미지 파일
     * @param model 뷰에 전달할 데이터
     * @return 결과 페이지
     */
    @PostMapping("/analyze")
    public String analyzeReceipt(@RequestParam("file") MultipartFile file, Model model) {
        try {
            // 파일 검증
            if (file.isEmpty()) {
                model.addAttribute("error", "파일을 선택해주세요.");
                return "receipt/analyze";
            }

            // 파일을 Base64로 변환
            // Base64: 바이너리 데이터(이미지, 파일)를 텍스트로 인코딩하는 방식
            String base64Image = Base64.getEncoder()
                .encodeToString(file.getBytes());

            // GeminiService를 통해 영수증 분석
            String result = geminiService.analyzeReceipt(base64Image);

            model.addAttribute("result", result);
            model.addAttribute("fileName", file.getOriginalFilename());

            return "receipt/result";

        } catch (Exception e) {
            log.error("영수증 분석 중 오류 발생: ", e);
            model.addAttribute("error", "영수증 분석 중 오류가 발생했습니다: " + e.getMessage());
            return "receipt/analyze";
        }
    }

    /**
     * 영수증 분석 REST API (JSON 응답)
     * JavaScript fetch 요청을 위한 엔드포인트
     * MVP 기준: 순차적 분석 방식(하나씩 분석)을 채택했기 때문에
     * 단일 파일만 처리하고 JSON 응답을 반환
     * 
     * 변경 사항: Gemini가 반환한 JSON 문자열을 객체로 파싱하여 프론트엔드에 전달
     * 이유: 프론트엔드에서 바로 사용할 수 있도록 하여 에러 처리와 데이터 접근을 용이하게 함
     */
    @PostMapping("/analyze/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> analyzeReceiptApi(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // 파일 검증
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "파일을 선택해주세요.");
                return ResponseEntity.badRequest().body(response);
            }

            // 파일을 Base64로 변환
            String base64Image = Base64.getEncoder()
                .encodeToString(file.getBytes());

            // GeminiService를 통해 영수증 분석 (JSON 문자열 반환)
            String resultJsonString = geminiService.analyzeReceipt(base64Image);

            // JSON 문자열을 객체로 파싱
            Map<String, Object> resultMap;
            try {
                // 마크다운 코드 블록 제거 (Gemini가 ```json ... ``` 형식으로 반환할 수 있음)
                String cleanedJson = resultJsonString.trim();
                if (cleanedJson.startsWith("```json")) {
                    cleanedJson = cleanedJson.substring(7);
                }
                if (cleanedJson.startsWith("```")) {
                    cleanedJson = cleanedJson.substring(3);
                }
                if (cleanedJson.endsWith("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.length() - 3);
                }
                cleanedJson = cleanedJson.trim();
                
                // JSON 문자열을 Map으로 파싱
                @SuppressWarnings("unchecked")
                Map<String, Object> parsedMap = (Map<String, Object>) objectMapper.readValue(cleanedJson, Map.class);
                resultMap = parsedMap;
                log.debug("JSON 파싱 성공");
                
            } catch (Exception e) {
                log.error("JSON 파싱 실패: {}", e.getMessage(), e);
                // 파싱 실패 시 원본 문자열을 그대로 반환 (에러 메시지일 수 있음)
                resultMap = new HashMap<>();
                resultMap.put("raw", resultJsonString);
            }

            response.put("success", true);
            response.put("result", resultMap);  // 파싱된 객체로 저장
            response.put("fileName", file.getOriginalFilename());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("영수증 분석 중 오류 발생: ", e);
            response.put("success", false);
            response.put("error", "영수증 분석 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}