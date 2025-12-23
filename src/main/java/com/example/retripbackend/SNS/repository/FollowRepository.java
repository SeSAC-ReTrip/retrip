package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Follow;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    // 팔로우 관계 확인
    boolean existsByFollowerAndFollowing(User follower, User following);

    // 팔로우 관계 찾기
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    // 팔로워 목록 (나를 팔로우하는 사람들)
    List<Follow> findByFollowing(User following);

    // 팔로잉 목록 (내가 팔로우하는 사람들)
    List<Follow> findByFollower(User follower);

    // 팔로워 수
    long countByFollowing(User following);

    // 팔로잉 수
    long countByFollower(User follower);
}









