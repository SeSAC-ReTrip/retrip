package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostImageRepository;
import com.example.retripbackend.SNS.repository.PostRepository;
import com.example.retripbackend.SNS.service.FileStorageService;
import com.example.retripbackend.user.entity.User;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final FileStorageService fileStorageService;

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

    // 게시글의 이미지 목록 조회
    public List<PostImage> getPostImages(Post post) {
        return postImageRepository.findByPostOrderByDisplayOrderAsc(post);
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

    // 게시글 작성 (빌더 패턴 적용)
    @Transactional
    public Post createPost(User author, Travel travel, String title, String content, MultipartFile[] images) throws IOException {
        // 이미지 저장
        List<String> imageUrls = null;
        String thumbnailUrl = null;
        
        if (images != null && images.length > 0) {
            imageUrls = fileStorageService.saveFiles(images);
            thumbnailUrl = fileStorageService.getThumbnailUrl(imageUrls);
        }
        
        Post post = Post.builder()
            .author(author)
            .travel(travel)
            .title(title)
            .content(content)
            .imageUrl(thumbnailUrl) // 첫 번째 이미지를 썸네일로
            .build();
        
        post = postRepository.save(post);
        
        // PostImage 엔티티들 저장
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build();
                postImageRepository.save(postImage);
            }
        }
        
        return post;
    }

    // 기존 메서드는 유지하고, 이미지 포함 버전 추가
    @Transactional
    public void updatePost(Long postId, String title, String content, User currentUser, MultipartFile[] images) throws IOException {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.isAuthor(currentUser)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        post.update(title, content);

        // 이미지가 새로 업로드되었으면
        if (images != null && images.length > 0 && !images[0].isEmpty()) {
            // 기존 이미지 삭제
            List<PostImage> oldImages = postImageRepository.findByPostOrderByDisplayOrderAsc(post);
            postImageRepository.deleteAll(oldImages);

            // 새 이미지 저장
            List<String> imageUrls = fileStorageService.saveFiles(images);
            String thumbnailUrl = fileStorageService.getThumbnailUrl(imageUrls);
            post.updateImageUrl(thumbnailUrl);

            // PostImage 엔티티 저장
            for (int i = 0; i < imageUrls.size(); i++) {
                PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build();
                postImageRepository.save(postImage);
            }
        }
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

    // ========== 검색 기능을 위한 새 메서드들 ==========


    // 도시명으로 게시물 검색 (부분 일치)
     //SearchController의 검색 결과 표시에 사용
    public Page<Post> searchPostsByCity(String cityKeyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByTravel_CityContainingIgnoreCase(cityKeyword, pageable);
    }


     // 게시물이 많은 도시 상위 N개 조회
    public List<String> getTopCitiesByPostCount(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findTopCitiesByPostCount(pageable);
    }
}
