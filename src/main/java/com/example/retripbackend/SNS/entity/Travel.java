package com.example.retripbackend.SNS.entity;

import com.example.retripbackend.baseEntity.BaseEntity;
import com.example.retripbackend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
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

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private int totalAmount = 0;

    // 정적 팩토리 메서드
    public static Travel of(User user, String country, String city, LocalDate startDate, LocalDate endDate) {
        Travel travel = new Travel();
        travel.user = user;
        travel.country = country;
        travel.city = city;
        travel.startDate = startDate;
        travel.endDate = endDate;
        return travel;
    }

    // 비즈니스 메서드
    public void update(String country, String city, LocalDate startDate, LocalDate endDate, String memo) {
        if (country != null && !country.isBlank()) {
            this.country = country;
        }
        if (city != null && !city.isBlank()) {
            this.city = city;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }

    public void updateTotalAmount(int amount) {
        this.totalAmount = amount;
    }

    public boolean isOwner(User user) {
        return this.user.getUserId().equals(user.getUserId());
    }
}
