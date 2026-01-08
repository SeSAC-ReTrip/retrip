package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Comment;
import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.PostImage;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.service.CommentService;
import com.example.retripbackend.SNS.service.FileStorageService;
import com.example.retripbackend.SNS.service.PostLikeService;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.receipt.service.ReceiptService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final TravelService travelService;
    private final FileStorageService fileStorageService;
    private final ReceiptService receiptService;

    @Value("${google.map.api.key:}")
    private String googleMapApiKey;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String feed(@RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        Page<Post> posts = "recommend".equals(sort)
            ? postService.getRecommendedPosts(page, 10)
            : postService.getLatestPosts(page, 10);

        model.addAttribute("posts", posts);
        model.addAttribute("sort", sort);

        // 좋아요 상태 확인을 위해 postLikeService를 모델에 전달 (HTML에서 사용)
        if (userDetails != null) {
            model.addAttribute("currentUser", userDetails.getUser());
            model.addAttribute("postLikeService", postLikeService);
        }

        return "home";
    }

    @GetMapping("/posts/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        List<Travel> travels = travelService.getUserTravels(userDetails.getUser());
        
        // Travel과 통화 정보를 함께 담는 리스트 생성
        List<TravelWithCurrency> travelsWithCurrency = travels.stream()
            .map(travel -> {
                // 해당 travel의 첫 번째 receipt의 통화 가져오기
                List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);
                String currency = receipts.stream()
                    .filter(r -> r.getCurrency() != null && !r.getCurrency().isEmpty())
                    .map(Receipt::getCurrency)
                    .findFirst()
                    .orElse("KRW"); // 기본값
                
                return new TravelWithCurrency(travel, currency);
            })
            .collect(Collectors.toList());
        
        model.addAttribute("travels", travelsWithCurrency);
        return "post/create";
    }

    @GetMapping("/posts/detail")
    public String detailPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId, Model model) {
        if (userDetails == null) return "redirect:/login";
        Travel travel = travelService.getTravelById(travelId);

        // 1. 해당 여행의 영수증 조회
        List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);

        // 2. 지도에 사용할 좌표 데이터 생성
        List<Map<String, Object>> locations = receipts.stream()
                .filter(r -> r.getLatitude() != null && r.getLongitude() != null)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("lat", r.getLatitude());
                    map.put("lng", r.getLongitude());
                    map.put("title", r.getStoreName());
                    return map;
                })
                .collect(Collectors.toList());

        // 3. model에 추가
        model.addAttribute("travel", travel);
        model.addAttribute("locations", locations);
        model.addAttribute("googleMapApiKey", googleMapApiKey);
        model.addAttribute("username", userDetails.getUser().getName());
        return "post/detail";
    }

    @PostMapping("/posts/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam(value = "images", required = false) MultipartFile[] images) {
        try {
            Travel travel = travelService.getTravelById(travelId);
            Post post = postService.createPost(userDetails.getUser(), travel, title, content, images);
            return "redirect:/posts/upload/complete?postId=" + post.getPostId();
        } catch (Exception e) {
            return "redirect:/posts/detail?travelId=" + travelId + "&error=" + e.getMessage();
        }
    }

    @GetMapping("/posts/upload/complete")
    public String uploadComplete(@RequestParam Long postId, Model model) {
        model.addAttribute("postId", postId);
        return "post/upload-complete";
    }

    @GetMapping("/posts/{postId}/content")
    public String content(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId, Model model) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        Post post = postService.getPostById(postId);
        List<Comment> comments = commentService.getPostComments(post);
        List<com.example.retripbackend.SNS.entity.PostImage> images = postService.getPostImages(post);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("images", images);
        model.addAttribute("isLiked", (currentUser != null) && postLikeService.isLiked(post, currentUser));
        model.addAttribute("isAuthor", (currentUser != null) && post.isAuthor(currentUser));
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "post/content";
    }

    // 게시물 상세 페이지 - content로 리다이렉트
    @GetMapping("/posts/{postId}")
    public String detail(@PathVariable Long postId) {
        return "redirect:/posts/" + postId + "/content";
    }

    // 게시물 수정 페이지 (detail 페이지 재사용)
    @GetMapping("/posts/{postId}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        Model model) {
        if (userDetails == null) return "redirect:/login";

        Post post = postService.getPostById(postId);

        // 작성자만 수정 가능
        if (!post.isAuthor(userDetails.getUser())) {
            return "redirect:/posts/" + postId;
        }

        Travel travel = post.getTravel();

        // 게시물의 이미지 목록 조회 추가
        List<PostImage> images = postService.getPostImages(post);

        model.addAttribute("travel", travel);
        model.addAttribute("post", post);
        model.addAttribute("images", images);  // 이미지 추가
        model.addAttribute("googleMapApiKey", googleMapApiKey);
        model.addAttribute("username", userDetails.getUser().getName());
        model.addAttribute("isEdit", true);  // 수정 모드 플래그

        return "post/detail";
    }

    @PostMapping("/posts/{postId}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam String content,
        @RequestParam(value = "images", required = false) MultipartFile[] images) {  // 이미지 파라미터 추가
        try {
            postService.updatePost(postId, title, content, userDetails.getUser(), images);  // images 전달
        } catch (RuntimeException e) {
            return "redirect:/posts/" + postId + "?error=unauthorized";
        } catch (IOException e) {
            return "redirect:/posts/" + postId + "?error=upload";
        }
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/posts/{postId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        if (post.isAuthor(userDetails.getUser())) {
            postService.deletePost(post);
        }
        return "redirect:/home";
    }


    // 좋아요 기능
    @ResponseBody
    @PostMapping("/posts/{postId}/like")
    public String toggleLike(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        if (userDetails == null) return "fail";

        User user = userDetails.getUser();
        Post post = postService.getPostById(postId);

        if (postLikeService.isLiked(post, user)) {
            postLikeService.unlike(post, user);
            return "unliked"; // 취소 성공 응답
        } else {
            postLikeService.like(post, user);
            return "liked"; // 추가 성공 응답
        }
    }
    
    // Travel과 통화 정보를 함께 담는 내부 클래스
    public static class TravelWithCurrency {
        private final Travel travel;
        private final String currency;
        
        public TravelWithCurrency(Travel travel, String currency) {
            this.travel = travel;
            this.currency = currency;
        }
        
        public Travel getTravel() { return travel; }
        public String getCurrency() { return currency; }
    }
}