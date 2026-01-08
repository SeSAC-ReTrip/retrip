package com.example.retripbackend.SNS.service;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostImageRepository;
import com.example.retripbackend.SNS.repository.PostRepository;
import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.receipt.repository.ReceiptRepository;
import com.example.retripbackend.user.entity.User;
import java.io.IOException;
import java.util.ArrayList;
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
    private final ReceiptRepository receiptRepository;

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
                    .receiptIndex(null) // 일반 게시글은 영수증 인덱스 없음
                    .build();
                postImageRepository.save(postImage);
            }
        }

        return post;
    }

    // 영수증별 데이터를 받아서 게시글 생성
    @Transactional
    public Post createPostWithReceipts(User author, Travel travel, String title,
        List<Long> receiptIds, List<String> receiptContents, List<MultipartFile[]> receiptImagesList) throws IOException {

        // 모든 이미지를 하나의 리스트로 합치면서 영수증 인덱스 기록
        List<String> allImageUrls = new ArrayList<>();
        List<Integer> imageReceiptIndexes = new ArrayList<>();

        if (receiptImagesList != null && !receiptImagesList.isEmpty()) {
            for (int receiptIdx = 0; receiptIdx < receiptImagesList.size(); receiptIdx++) {
                MultipartFile[] images = receiptImagesList.get(receiptIdx);
                if (images != null && images.length > 0) {
                    List<String> urls = fileStorageService.saveFiles(images);
                    allImageUrls.addAll(urls);

                    // 각 이미지가 어느 영수증에 속하는지 기록
                    for (int i = 0; i < urls.size(); i++) {
                        imageReceiptIndexes.add(receiptIdx);
                    }
                }
            }
        }

        // content를 영수증별로 구성
        StringBuilder contentBuilder = new StringBuilder();
        if (receiptContents != null && !receiptContents.isEmpty()) {
            for (int i = 0; i < receiptContents.size(); i++) {
                if (receiptContents.get(i) != null && !receiptContents.get(i).trim().isEmpty()) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n\n");
                    }
                    contentBuilder.append("[영수증 ").append(i + 1).append("]\n");
                    contentBuilder.append(receiptContents.get(i).trim());
                }
            }
        }

        String thumbnailUrl = allImageUrls.isEmpty() ? null : fileStorageService.getThumbnailUrl(allImageUrls);

        Post post = Post.builder()
            .author(author)
            .travel(travel)
            .title(title)
            .content(contentBuilder.toString())
            .imageUrl(thumbnailUrl)
            .build();

        post = postRepository.save(post);

        // PostImage 엔티티들 저장 (영수증 인덱스 포함)
        if (!allImageUrls.isEmpty()) {
            for (int i = 0; i < allImageUrls.size(); i++) {
                PostImage postImage = PostImage.builder()
                    .post(post)
                    .imageUrl(allImageUrls.get(i))
                    .displayOrder(i)
                    .receiptIndex(imageReceiptIndexes.get(i)) // 영수증 인덱스 저장
                    .build();
                postImageRepository.save(postImage);
            }
        }

        // 각 영수증에 설명 업데이트 (ReceiptRepository 직접 사용)
        if (receiptIds != null && receiptContents != null) {
            for (int i = 0; i < Math.min(receiptIds.size(), receiptContents.size()); i++) {
                if (receiptContents.get(i) != null && !receiptContents.get(i).trim().isEmpty()) {
                    final int index = i;
                    receiptRepository.findById(receiptIds.get(i)).ifPresent(receipt -> {
                        receipt.updateDescription(receiptContents.get(index).trim());
                    });
                }
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
                    .receiptIndex(null)
                    .build();
                postImageRepository.save(postImage);
            }
        }
    }

    // 영수증별 데이터로 게시글 수정
    @Transactional
    public void updatePostWithReceipts(Long postId, String title, User currentUser,
        List<Long> receiptIds, List<String> receiptContents, List<MultipartFile[]> receiptImagesList) throws IOException {

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        if (!post.isAuthor(currentUser)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        // content를 영수증별로 구성
        StringBuilder contentBuilder = new StringBuilder();
        if (receiptContents != null && !receiptContents.isEmpty()) {
            for (int i = 0; i < receiptContents.size(); i++) {
                if (receiptContents.get(i) != null && !receiptContents.get(i).trim().isEmpty()) {
                    if (contentBuilder.length() > 0) {
                        contentBuilder.append("\n\n");
                    }
                    contentBuilder.append("[영수증 ").append(i + 1).append("]\n");
                    contentBuilder.append(receiptContents.get(i).trim());
                }
            }
        }

        post.update(title, contentBuilder.toString());

        // 이미지가 새로 업로드되었으면
        boolean hasNewImages = receiptImagesList != null && receiptImagesList.stream()
            .anyMatch(images -> images != null && images.length > 0 && !images[0].isEmpty());

        if (hasNewImages) {
            // 기존 이미지 삭제
            List<PostImage> oldImages = postImageRepository.findByPostOrderByDisplayOrderAsc(post);
            postImageRepository.deleteAll(oldImages);

            // 모든 이미지를 하나의 리스트로 합치면서 영수증 인덱스 기록
            List<String> allImageUrls = new ArrayList<>();
            List<Integer> imageReceiptIndexes = new ArrayList<>();

            for (int receiptIdx = 0; receiptIdx < receiptImagesList.size(); receiptIdx++) {
                MultipartFile[] images = receiptImagesList.get(receiptIdx);
                if (images != null && images.length > 0 && !images[0].isEmpty()) {
                    List<String> urls = fileStorageService.saveFiles(images);
                    allImageUrls.addAll(urls);

                    // 각 이미지가 어느 영수증에 속하는지 기록
                    for (int i = 0; i < urls.size(); i++) {
                        imageReceiptIndexes.add(receiptIdx);
                    }
                }
            }

            if (!allImageUrls.isEmpty()) {
                String thumbnailUrl = fileStorageService.getThumbnailUrl(allImageUrls);
                post.updateImageUrl(thumbnailUrl);

                // PostImage 엔티티 저장 (영수증 인덱스 포함)
                for (int i = 0; i < allImageUrls.size(); i++) {
                    PostImage postImage = PostImage.builder()
                        .post(post)
                        .imageUrl(allImageUrls.get(i))
                        .displayOrder(i)
                        .receiptIndex(imageReceiptIndexes.get(i))
                        .build();
                    postImageRepository.save(postImage);
                }
            }
        }

        // 각 영수증에 설명 업데이트 (ReceiptRepository 직접 사용)
        if (receiptIds != null && receiptContents != null) {
            for (int i = 0; i < Math.min(receiptIds.size(), receiptContents.size()); i++) {
                if (receiptContents.get(i) != null && !receiptContents.get(i).trim().isEmpty()) {
                    final int index = i;
                    receiptRepository.findById(receiptIds.get(i)).ifPresent(receipt -> {
                        receipt.updateDescription(receiptContents.get(index).trim());
                    });
                }
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
    // SearchController의 검색 결과 표시에 사용
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