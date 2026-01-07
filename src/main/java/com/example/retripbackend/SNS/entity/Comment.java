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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 정적 팩토리 메서드
    public static Comment of(Post post, User author, String content) {
        Comment comment = new Comment();
        comment.post = post;
        comment.author = author;
        comment.content = content;
        return comment;
    }

    // 비즈니스 메서드
    public void update(String content) {
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
    }

    public boolean isAuthor(User user) {
        return this.author.getUserId().equals(user.getUserId());
    }
}









