package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostLike;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // 좋아요 존재 확인
    boolean existsByPostAndUser(Post post, User user);

    // 좋아요 찾기
    Optional<PostLike> findByPostAndUser(Post post, User user);

    // 좋아요 수 조회
    long countByPost(Post post);


    @Query("SELECT pl FROM PostLike pl " +
        "JOIN FETCH pl.post p " +
        "JOIN FETCH p.author " +
        "WHERE pl.user = :user")
    List<PostLike> findByUserWithPostAndUser(@Param("user") User user);

    // 기본 목록 조회
    List<PostLike> findByUser(User user);
}