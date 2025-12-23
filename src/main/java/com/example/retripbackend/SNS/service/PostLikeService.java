package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostLike;
import com.example.retripbackend.SNS.repository.PostLikeRepository;
import com.example.retripbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    // 좋아요 여부 확인
    public boolean isLiked(Post post, User user) {
        return postLikeRepository.existsByPostAndUser(post, user);
    }

    // 좋아요
    @Transactional
    public void like(Post post, User user) {
        // 이미 좋아요했는지 확인
        if (postLikeRepository.existsByPostAndUser(post, user)) {
            throw new RuntimeException("이미 좋아요한 게시글입니다.");
        }

        PostLike like = PostLike.of(post, user);
        postLikeRepository.save(like);

        // 게시글의 좋아요 수 증가
        post.incrementLikeCount();
    }

    // 좋아요 취소
    @Transactional
    public void unlike(Post post, User user) {
        PostLike like = postLikeRepository.findByPostAndUser(post, user)
            .orElseThrow(() -> new RuntimeException("좋아요하지 않은 게시글입니다."));

        postLikeRepository.delete(like);

        // 게시글의 좋아요 수 감소
        post.decrementLikeCount();
    }
}
