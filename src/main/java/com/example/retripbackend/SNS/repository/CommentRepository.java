package com.example.retripbackend.SNS.repository;

import com.example.retripbackend.SNS.entity.Comment;
import com.example.retripbackend.SNS.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게시글의 댓글 (오래된 순)
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);

    // 댓글 수 조회
    long countByPost(Post post);
}
