package com.example.retripbackend.SNS.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "post_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder; // 이미지 순서

    @Column
    private Integer receiptIndex; // 영수증 인덱스 추가

    @Builder
    public PostImage(Post post, String imageUrl, int displayOrder, Integer receiptIndex) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.receiptIndex = receiptIndex;
    }
}