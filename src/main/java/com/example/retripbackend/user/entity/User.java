package com.example.retripbackend.user.entity;

import com.example.retripbackend.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String profileImage;

    @Column(length = 200)
    private String bio;

    // ===== 정적 팩토리 메서드 =====

    public static User of(String email, String encodedPassword, String name) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.name = name;
        return user;
    }

    // ===== 비즈니스 메서드 =====

    public void updateProfile(String name, String bio, String profileImage) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (bio != null) {
            this.bio = bio;
        }
        if (profileImage != null) {
            this.profileImage = profileImage;
        }
    }

    public void updatePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }

    public boolean isOwner(Long userId) {
        return this.userId.equals(userId);
    }
}
