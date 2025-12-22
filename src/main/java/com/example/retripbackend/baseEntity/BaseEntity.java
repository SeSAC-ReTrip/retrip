package com.example.retripbackend.baseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter // private 필드를 외부에서 “읽을 수 있게만” 해주는 공식 통로
@MappedSuperclass //엔티티 공통 필드 상속용
@EntityListeners(AuditingEntityListener.class) // 생성일 / 수정일 자동 저장
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}
