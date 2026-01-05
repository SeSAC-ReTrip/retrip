package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Comment;
import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.service.CommentService;
import com.example.retripbackend.SNS.service.PostLikeService;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final TravelService travelService;

    @Value("${google.map.api.key:}")
    private String googleMapApiKey;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String feed(@RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        Page<Post> posts = "recommend".equals(sort)
            ? postService.getRecommendedPosts(page, 10)
            : postService.getLatestPosts(page, 10);

        model.addAttribute("posts", posts);
        model.addAttribute("sort", sort);
        return "home";
    }

    @GetMapping("/posts/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        List<Travel> travels = travelService.getUserTravels(userDetails.getUser());
        model.addAttribute("travels", travels);
        return "post/create";
    }

    // [추가] 가계부 선택 후 상세 편집 페이지로 이동
    @GetMapping("/posts/detail")
    public String detailPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId, Model model) {
        if (userDetails == null) return "redirect:/login";
        Travel travel = travelService.getTravelById(travelId);
        model.addAttribute("travel", travel);
        model.addAttribute("googleMapApiKey", googleMapApiKey);
        // 작성자 정보와 오늘 날짜 등을 모델에 추가
        model.addAttribute("username", userDetails.getUser().getName());
        return "post/detail";
    }

    @PostMapping("/posts/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId, @RequestParam String title, @RequestParam String content) {
        Travel travel = travelService.getTravelById(travelId);
        Post post = postService.createPost(userDetails.getUser(), travel, title, content);
        return "redirect:/posts/upload/complete?postId=" + post.getPostId();
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

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("isLiked", (currentUser != null) && postLikeService.isLiked(post, currentUser));
        model.addAttribute("isAuthor", (currentUser != null) && post.isAuthor(currentUser));
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "post/content";
    }

    @PostMapping("/posts/{postId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails, @PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        if (post.isAuthor(userDetails.getUser())) {
            postService.deletePost(post);
        }
        return "redirect:/home";
    }
}