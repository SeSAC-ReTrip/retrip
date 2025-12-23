package com.example.retripbackend.user.service;

import com.example.retripbackend.SNS.repository.FollowRepository;
import com.example.retripbackend.SNS.repository.PostRepository;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    // 사용자 조회
    public User findById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    // 프로필 수정
    @Transactional
    public void updateProfile(User user, String name, String bio, String profileImage) {
        user.updateProfile(name, bio, profileImage);
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 올바르지 않습니다.");
        }

        // 새 비밀번호로 변경
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.updatePassword(encodedPassword);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteAccount(User user) {
        userRepository.delete(user);
    }

    // 통계 조회
    public UserStats getUserStats(User user) {
        long followerCount = followRepository.countByFollowing(user);
        long followingCount = followRepository.countByFollower(user);
        long postCount = postRepository.countByAuthor(user);

        return new UserStats(followerCount, followingCount, postCount);
    }

    // 통계 DTO
    public record UserStats(long followerCount, long followingCount, long postCount) {}
}









