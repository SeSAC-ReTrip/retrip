package com.example.retripbackend.receipt.service;

import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.receipt.repository.ReceiptRepository;
import com.example.retripbackend.SNS.entity.Travel;
import java.util.List;
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
}

