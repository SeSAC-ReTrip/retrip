package com.example.retripbackend.ai.controller;

import com.example.retripbackend.ai.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

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
}