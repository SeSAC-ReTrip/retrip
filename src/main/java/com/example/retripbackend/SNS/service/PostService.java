package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostImageRepository;
import com.example.retripbackend.SNS.repository.PostRepository;
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

    /**
     * [에러 해결] UserController에서 호출하는 사용자 게시글 목록 조회
     */
    public Page<Post> getUserPosts(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByAuthor(user, pageable);
    }

    public Page<Post> getLatestPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<Post> getRecommendedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByLikeCountDescCreatedAtDesc(pageable);
    }

    @Transactional
    public Post getPostById(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        post.incrementViewCount();
        return post;
    }

    public List<PostImage> getPostImages(Post post) {
        return postImageRepository.findByPostOrderByDisplayOrderAsc(post);
    }

    @Transactional
    public Post createPost(User author, Travel travel, String title, String content, MultipartFile[] images) throws IOException {
        List<String> imageUrls = null;
        String thumbnailUrl = null;

        if (images != null && images.length > 0 && !images[0].isEmpty()) {
            imageUrls = fileStorageService.saveFiles(images);
            thumbnailUrl = fileStorageService.getThumbnailUrl(imageUrls);
        }

        Post post = Post.builder()
            .author(author)
            .travel(travel)
            .title(title)
            .content(content)
            .imageUrl(thumbnailUrl)
            .build();

        post = postRepository.save(post);

        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                postImageRepository.save(PostImage.builder()
                    .post(post)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build());
            }
        }
        return post;
    }

    @Transactional
    public void updatePost(Long postId, String title, String content, MultipartFile[] newImages, List<String> removedImageUrls, User currentUser) throws IOException {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.isAuthor(currentUser)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        // 1. 기존 이미지 중 삭제된 것 처리
        if (removedImageUrls != null && !removedImageUrls.isEmpty()) {
            for (String url : removedImageUrls) {
                postImageRepository.deleteByImageUrlAndPost(url, post);
                fileStorageService.deleteFile(url);
            }
        }

        // 2. 새로운 이미지 추가 저장
        if (newImages != null && newImages.length > 0 && !newImages[0].isEmpty()) {
            List<String> newUrls = fileStorageService.saveFiles(newImages);
            int lastOrder = postImageRepository.findMaxDisplayOrderByPost(post).orElse(-1);
            for (int i = 0; i < newUrls.size(); i++) {
                postImageRepository.save(PostImage.builder()
                    .post(post)
                    .imageUrl(newUrls.get(i))
                    .displayOrder(lastOrder + 1 + i)
                    .build());
            }
        }

        // 3. 텍스트 정보 업데이트
        post.update(title, content);

        // 4. 썸네일 재설정
        List<PostImage> remainingImages = postImageRepository.findByPostOrderByDisplayOrderAsc(post);
        if (!remainingImages.isEmpty()) {
            post.updateThumbnail(remainingImages.get(0).getImageUrl());
        } else {
            post.updateThumbnail(null);
        }
    }

    @Transactional
    public void deletePost(Post post) {
        List<PostImage> images = postImageRepository.findByPostOrderByDisplayOrderAsc(post);
        for (PostImage img : images) {
            fileStorageService.deleteFile(img.getImageUrl());
        }
        postRepository.delete(post);
    }

    public Page<Post> getPostsByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByCityOrderByCreatedAtDesc(city, pageable);
    }

    public Page<Post> searchPostsByCity(String cityKeyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return postRepository.findByTravel_CityContainingIgnoreCase(cityKeyword, pageable);
    }

    public List<String> getTopCitiesByPostCount(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findTopCitiesByPostCount(pageable);
    }
}