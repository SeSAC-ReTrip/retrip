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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final TravelService travelService;

    // 게시글 피드 (홈)
    @GetMapping
    public String feed(@RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        Page<Post> posts;

        if ("recommend".equals(sort)) {
            posts = postService.getRecommendedPosts(page, 10);
        } else {
            posts = postService.getLatestPosts(page, 10);
        }

        model.addAttribute("posts", posts);
        model.addAttribute("sort", sort);

        return "post/feed";
    }

    // 게시글 상세
    @GetMapping("/{postId}")
    public String detail(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        Model model) {
        User currentUser = userDetails.getUser();
        Post post = postService.getPostById(postId);
        List<Comment> comments = commentService.getPostComments(post);
        boolean isLiked = postLikeService.isLiked(post, currentUser);
        boolean isAuthor = post.isAuthor(currentUser);

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("isLiked", isLiked);
        model.addAttribute("isAuthor", isAuthor);

        return "post/detail";
    }

    // 게시글 작성 페이지
    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        List<Travel> travels = travelService.getUserTravels(user);

        model.addAttribute("travels", travels);

        return "post/create";
    }

    // 게시글 작성 처리
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId,
        @RequestParam String title,
        @RequestParam String content) {
        User author = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);

        Post post = postService.createPost(author, travel, title, content);

        return "redirect:/posts/" + post.getPostId();
    }

    // 게시글 수정 페이지
    @GetMapping("/{postId}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        Model model) {
        User currentUser = userDetails.getUser();
        Post post = postService.getPostById(postId);

        if (!post.isAuthor(currentUser)) {
            return "redirect:/posts/" + postId;
        }

        model.addAttribute("post", post);

        return "post/edit";
    }

    // 게시글 수정 처리
    @PostMapping("/{postId}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam String content) {
        User currentUser = userDetails.getUser();
        Post post = postService.getPostById(postId);

        if (!post.isAuthor(currentUser)) {
            return "redirect:/posts/" + postId;
        }

        postService.updatePost(post, title, content);

        return "redirect:/posts/" + postId;
    }

    // 게시글 삭제
    @PostMapping("/{postId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        User currentUser = userDetails.getUser();
        Post post = postService.getPostById(postId);

        if (!post.isAuthor(currentUser)) {
            return "redirect:/posts/" + postId;
        }

        postService.deletePost(post);

        return "redirect:/posts";
    }

    // 좋아요
    @PostMapping("/{postId}/like")
    public String like(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        User user = userDetails.getUser();
        Post post = postService.getPostById(postId);

        try {
            postLikeService.like(post, user);
        } catch (RuntimeException e) {
            // 이미 좋아요한 경우 무시
        }

        return "redirect:/posts/" + postId;
    }

    // 좋아요 취소
    @PostMapping("/{postId}/unlike")
    public String unlike(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        User user = userDetails.getUser();
        Post post = postService.getPostById(postId);

        try {
            postLikeService.unlike(post, user);
        } catch (RuntimeException e) {
            // 좋아요 안 한 경우 무시
        }

        return "redirect:/posts/" + postId;
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public String createComment(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        @RequestParam String content) {
        User author = userDetails.getUser();
        Post post = postService.getPostById(postId);

        commentService.createComment(post, author, content);

        return "redirect:/posts/" + postId;
    }

    // 댓글 삭제
    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long commentId,
        @RequestParam Long postId) {
        // 댓글 삭제 로직 (CommentRepository 필요)
        return "redirect:/posts/" + postId;
    }

    @Value("${google.map.api.key}")
    private String googleMapApiKey;

    @GetMapping("/post/{id}")
    public String getPost(@PathVariable Long id, Model model) {
        // 게시물 데이터 추가
        // model.addAttribute("post", post);

        // Google Maps API 키 전달 (중요!)
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "content";
    }
}









