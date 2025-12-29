package com.example.retripbackend.receipt.entity;

import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "receipts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false)
    private Travel travel;

    @Column(nullable = false)
    private String storeName;  // 업소명

    @Column(nullable = false)
    private int amount;  // 금액

    @Column(nullable = false)
    private LocalDateTime paidAt;  // 결제일시

    @Column(columnDefinition = "TEXT")
    private String description;  // SNS용 설명

    // TODO: 가계부 팀원이 추가 필드 구현
    // - imageUrl (영수증 이미지)
    // - category (카테고리)
    // - OCR 데이터 등
}






