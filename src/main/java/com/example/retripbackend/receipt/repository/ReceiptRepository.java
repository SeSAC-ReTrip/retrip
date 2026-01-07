package com.example.retripbackend.receipt.repository;

import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.SNS.entity.Travel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    
    // 특정 여행의 영수증 목록 조회
    List<Receipt> findByTravelOrderByPaidAtDesc(Travel travel);
    
    // 특정 여행의 영수증 목록 조회 (최신순)
    List<Receipt> findByTravel_TravelIdOrderByPaidAtDesc(Long travelId);
    
    // Receipt 조회 시 Travel과 Travel의 User도 함께 조회 (LAZY 로딩 문제 해결)
    @EntityGraph(attributePaths = {"travel", "travel.user"})
    Optional<Receipt> findWithTravelByReceiptId(Long receiptId);
    
    // JPQL을 사용한 명시적 조인 쿼리 (더 안전한 방법)
    @Query("SELECT r FROM Receipt r JOIN FETCH r.travel t JOIN FETCH t.user WHERE r.receiptId = :receiptId")
    Optional<Receipt> findByIdWithTravelAndUser(@Param("receiptId") Long receiptId);
    
    // 특정 여행의 영수증 amount 합계 조회
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Receipt r WHERE r.travel.travelId = :travelId")
    int sumAmountByTravelId(@Param("travelId") Long travelId);
}

