package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    // 게시글별 이미지 목록 조회 (순서대로)
    List<PostImage> findByPostOrderByDisplayOrderAsc(Post post);

    // 게시글 전체 삭제 시 사용 (기존 메서드 유지)
    void deleteByPost(Post post);

    //특정 URL을 가진 이미지만 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM PostImage pi WHERE pi.imageUrl = :imageUrl AND pi.post = :post")
    void deleteByImageUrlAndPost(@Param("imageUrl") String imageUrl, @Param("post") Post post);


    // 현재 게시글에 등록된 이미지 중 가장 높은 순서(displayOrder)
    @Query("SELECT MAX(pi.displayOrder) FROM PostImage pi WHERE pi.post = :post")
    Optional<Integer> findMaxDisplayOrderByPost(@Param("post") Post post);
}
