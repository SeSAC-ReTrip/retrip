package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // 전체 게시글 (최신순)
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 추천순 (좋아요 많은 순)
    Page<Post> findAllByOrderByLikeCountDescCreatedAtDesc(Pageable pageable);

    // 특정 사용자의 게시글 (UserController에서 호출하는 메서드)
    Page<Post> findByAuthor(User author, Pageable pageable);

    // 특정 여행의 게시글
    @Query("SELECT p FROM Post p WHERE p.travel.travelId = :travelId ORDER BY p.createdAt DESC")
    List<Post> findByTravelId(@Param("travelId") Long travelId);

    // 특정 도시의 게시글
    @Query("SELECT p FROM Post p WHERE p.travel.city = :city ORDER BY p.createdAt DESC")
    Page<Post> findByCityOrderByCreatedAtDesc(@Param("city") String city, Pageable pageable);

    // 게시글 수 조회
    long countByAuthor(User author);

    // ========== 검색 기능을 위한 메서드들 ==========


    // 도시명으로 게시물 검색 (대소문자 무시, 부분 일치)
    Page<Post> findByTravel_CityContainingIgnoreCase(String cityKeyword, Pageable pageable);


    // 게시물이 많은 도시 상위 N개 조회
    @Query("SELECT t.city FROM Post p " +
        "JOIN p.travel t " +
        "GROUP BY t.city " +
        "ORDER BY COUNT(p) DESC")
    List<String> findTopCitiesByPostCount(Pageable pageable);
}