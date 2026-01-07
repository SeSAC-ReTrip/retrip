package com.example.retripbackend.SNS.entity;

import com.example.retripbackend.baseEntity.BaseEntity;
import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "travels")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Travel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long travelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String title;  // 여행 제목

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private int totalAmount = 0;

    // Receipt와의 관계 추가
    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Receipt> receipts = new ArrayList<>();

    @Builder
    public Travel(User user, String country, String city, String title, LocalDate startDate, LocalDate endDate, String memo) {
        this.user = user;
        this.country = country;
        this.city = city;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.memo = memo;
    }

    // 권한 체크
    public boolean isOwner(User user) {
        return this.user.getUserId().equals(user.getUserId());
    }

    // 수정 (나중에 가계부와 연동 시 제거 가능)
    public void updateInfo(String country, String city, LocalDate startDate, LocalDate endDate, String memo) {
        this.country = country;
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.memo = memo;
    }

    // 총액 업데이트 (가계부에서 호출)
    public void updateTotalAmount(int amount) {
        this.totalAmount = amount;
    }
}
