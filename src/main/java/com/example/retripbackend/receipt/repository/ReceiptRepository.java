package com.example.retripbackend.receipt.repository;

import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.SNS.entity.Travel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    // 특정 여행의 영수증 목록 조회
    List<Receipt> findByTravelOrderByPaidAtDesc(Travel travel);
    
    // 특정 여행의 영수증 목록 조회 (최신순)
    List<Receipt> findByTravel_TravelIdOrderByPaidAtDesc(Long travelId);
}

