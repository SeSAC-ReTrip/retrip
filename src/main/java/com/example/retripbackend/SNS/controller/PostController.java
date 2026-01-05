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
        model.addAttribute("travels", travels);
        return "post/create";
    }

    @GetMapping("/posts/detail")
    public String detailPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId, Model model) {
        if (userDetails == null) return "redirect:/login";
        Travel travel = travelService.getTravelById(travelId);
        model.addAttribute("travel", travel);
        model.addAttribute("googleMapApiKey", googleMapApiKey);
        model.addAttribute("username", userDetails.getUser().getName());
        return "post/detail";
    }

    @PostMapping("/posts/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId,
        @RequestParam String title,
        @RequestParam String content) {
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
}