package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Follow;
import com.example.retripbackend.SNS.repository.FollowRepository;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;

    // 팔로우 여부 확인
    public boolean isFollowing(User follower, User following) {
        return followRepository.existsByFollowerAndFollowing(follower, following);
    }

    // 팔로우
    @Transactional
    public void follow(User follower, User following) {
        // 자기 자신 팔로우 방지
        if (follower.getUserId().equals(following.getUserId())) {
            throw new RuntimeException("자기 자신을 팔로우할 수 없습니다.");
        }

        // 이미 팔로우 중인지 확인
        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new RuntimeException("이미 팔로우 중입니다.");
        }

        Follow follow = Follow.of(follower, following);
        followRepository.save(follow);
    }

    // 언팔로우
    @Transactional
    public void unfollow(User follower, User following) {
        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
            .orElseThrow(() -> new RuntimeException("팔로우 관계가 없습니다."));

        followRepository.delete(follow);
    }

    // 팔로워 목록 (나를 팔로우하는 사람들)
    public List<User> getFollowers(User user) {
        return followRepository.findByFollowing(user).stream()
            .map(Follow::getFollower)
            .collect(Collectors.toList());
    }

    // 팔로잉 목록 (내가 팔로우하는 사람들)
    public List<User> getFollowings(User user) {
        return followRepository.findByFollower(user).stream()
            .map(Follow::getFollowing)
            .collect(Collectors.toList());
    }

    // 팔로워 수
    public long getFollowerCount(User user) {
        return followRepository.countByFollowing(user);
    }

    // 팔로잉 수
    public long getFollowingCount(User user) {
        return followRepository.countByFollower(user);
    }
}









