package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostRepository;
import com.example.retripbackend.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    // 게시글 피드 조회 (최신순)
    public Page<Post> getLatestPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 게시글 피드 조회 (추천순)
    public Page<Post> getRecommendedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByLikeCountDescCreatedAtDesc(pageable);
    }

    // 게시글 상세 조회
    @Transactional
    public Post getPostById(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        post.incrementViewCount();

        return post;
    }

    // 특정 사용자의 게시글 목록
    public Page<Post> getUserPosts(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
    }

    // 특정 여행의 게시글 목록
    public List<Post> getTravelPosts(Long travelId) {
        return postRepository.findByTravelId(travelId);
    }

    // 게시글 작성
    @Transactional
    public Post createPost(User author, Travel travel, String title, String content) {
        Post post = Post.of(author, travel, title, content);
        return postRepository.save(post);
    }

    // 게시글 수정
    @Transactional
    public void updatePost(Post post, String title, String content) {
        post.update(title, content);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Post post) {
        postRepository.delete(post);
    }

    // 도시별 게시글 조회
    public Page<Post> getPostsByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByCityOrderByCreatedAtDesc(city, pageable);
    }
}









