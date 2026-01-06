package com.example.retripbackend.receipt.service;

import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.receipt.repository.ReceiptRepository;
import com.example.retripbackend.SNS.entity.Travel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    /**
     * 특정 여행의 영수증 목록 조회
     */
    public List<Receipt> getReceiptsByTravel(Long travelId) {
        return receiptRepository.findByTravel_TravelIdOrderByPaidAtDesc(travelId);
    }

    /**
     * 특정 여행의 영수증 목록 조회
     */
    public List<Receipt> getReceiptsByTravel(Travel travel) {
        return receiptRepository.findByTravelOrderByPaidAtDesc(travel);
    }

    /**
     * Gemini 분석 결과를 기반으로 Receipt 저장
     * 
     * @param travel Travel 엔티티
     * @param receiptData Gemini가 반환한 분석 결과 (Map)
     * @param imageUrl 영수증 이미지 URL (선택사항)
     * @return 저장된 Receipt
     */
    @Transactional
    public Receipt saveReceiptFromAnalysis(Travel travel, Map<String, Object> receiptData, String imageUrl) {
        // Gemini JSON 데이터를 Receipt 엔티티로 변환
        String storeName = (String) receiptData.getOrDefault("placeName", "알 수 없음");
        
        // amount 변환 (double -> int)
        int amount = 0;
        Object amountObj = receiptData.get("amount");
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).intValue();
        } else if (amountObj instanceof String) {
            try {
                amount = (int) Double.parseDouble((String) amountObj);
            } catch (NumberFormatException e) {
                log.warn("amount 파싱 실패: {}", amountObj);
            }
        }
        
        // paidAt 변환 (ISO 8601 String -> LocalDateTime)
        LocalDateTime paidAt = parsePaidAt(receiptData.get("paidAt"));
        
        // category는 선택사항 (Gemini JSON에 없으면 null)
        String category = (String) receiptData.get("category");
        
        // address
        String address = (String) receiptData.get("address");
        
        // currency
        String currency = (String) receiptData.get("currency");
        
        // latitude
        Double latitude = null;
        Object latitudeObj = receiptData.get("latitude");
        if (latitudeObj instanceof Number) {
            latitude = ((Number) latitudeObj).doubleValue();
        } else if (latitudeObj instanceof String) {
            try {
                latitude = Double.parseDouble((String) latitudeObj);
            } catch (NumberFormatException e) {
                log.warn("latitude 파싱 실패: {}", latitudeObj);
            }
        }
        
        // longitude
        Double longitude = null;
        Object longitudeObj = receiptData.get("longitude");
        if (longitudeObj instanceof Number) {
            longitude = ((Number) longitudeObj).doubleValue();
        } else if (longitudeObj instanceof String) {
            try {
                longitude = Double.parseDouble((String) longitudeObj);
            } catch (NumberFormatException e) {
                log.warn("longitude 파싱 실패: {}", longitudeObj);
            }
        }
        
        // Receipt 생성 및 저장
        Receipt receipt = Receipt.builder()
            .travel(travel)
            .storeName(storeName)
            .amount(amount)
            .paidAt(paidAt)
            .category(category)
            .imageUrl(imageUrl)
            .address(address)
            .currency(currency)
            .latitude(latitude)
            .longitude(longitude)
            .build();
        
        Receipt savedReceipt = receiptRepository.save(receipt);
        
        log.info("영수증 저장 완료: receiptId={}, storeName={}, amount={}, currency={}, address={}", 
            savedReceipt.getReceiptId(), storeName, amount, currency, address);
        
        return savedReceipt;
    }

    /**
     * paidAt 파싱 (ISO 8601 String -> LocalDateTime)
     */
    private LocalDateTime parsePaidAt(Object paidAtObj) {
        if (paidAtObj == null) {
            return LocalDateTime.now();
        }
        
        String paidAtStr = paidAtObj.toString();
        if (paidAtStr.isEmpty()) {
            return LocalDateTime.now();
        }
        
        // 여러 날짜 형식 시도
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                if (paidAtStr.contains("T")) {
                    return LocalDateTime.parse(paidAtStr, formatter);
                } else {
                    return LocalDate.parse(paidAtStr, formatter).atStartOfDay();
                }
            } catch (DateTimeParseException e) {
                // 다음 형식 시도
            }
        }
        
        log.warn("paidAt 파싱 실패, 현재 시간 사용: {}", paidAtStr);
        return LocalDateTime.now();
    }
}

