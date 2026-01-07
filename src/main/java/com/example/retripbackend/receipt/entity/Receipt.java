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
import lombok.Builder;
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

    @Column(nullable = false, length = 200)
    private String storeName;  // 업소명

    @Column(nullable = false)
    private int amount;  // 결제 금액

    @Column(nullable = false)
    private LocalDateTime paidAt;  // 결제 일시

    @Column(length = 50)
    private String category;  // 카테고리 (식비, 교통비 등)

    @Column(columnDefinition = "TEXT")
    private String description;  // SNS용 설명/일기

    @Column(length = 500)
    private String imageUrl;  // 영수증 이미지 URL

    @Column(columnDefinition = "TEXT")
    private String address;  // 주소

    @Column(length = 3)
    private String currency;  // 통화 코드 (ISO 4217: USD, EUR, KRW 등)

    @Column
    private Double latitude;  // 위도

    @Column
    private Double longitude;  // 경도

    @Builder
    public Receipt(Travel travel, String storeName, int amount, LocalDateTime paidAt,
        String category, String imageUrl, String address, String currency, Double latitude, Double longitude) {
        this.travel = travel;
        this.storeName = storeName;
        this.amount = amount;
        this.paidAt = paidAt;
        this.category = category;
        this.imageUrl = imageUrl;
        this.address = address;
        this.currency = currency;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // 설명 추가/수정 (SNS에서만 사용)
    public void updateDescription(String description) {
        this.description = description;
    }

    // 영수증 정보 수정
    public void updateReceiptInfo(String storeName, Integer amount, LocalDateTime paidAt,
        String category, String address, String currency) {
        if (storeName != null && !storeName.trim().isEmpty()) {
            this.storeName = storeName;
        }
        if (amount != null) {
            this.amount = amount;
        }
        if (paidAt != null) {
            this.paidAt = paidAt;
        }
        if (category != null) {
            this.category = category;
        }
        if (address != null) {
            this.address = address;
        }
        if (currency != null) {
            this.currency = currency;
        }
    }
}






