package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Comment;
import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostLikeRepository;
import com.example.retripbackend.SNS.service.CommentService;
import com.example.retripbackend.SNS.service.PostLikeService;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import java.util.List;
import java.util.stream.Collectors;
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

    @Value("${google.map.api.key:}")
    private String googleMapApiKey;

    // 홈 화면 (피드)
    @GetMapping
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

    // 게시글 상세 조회
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
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "post/detail";
    }

    // 게시글 컨텐츠 조회 (content.html)
    @GetMapping("/{postId}/content")
    public String content(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
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
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "post/content";
    }

    // 게시글 작성 페이지
    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        List<Travel> travels = travelService.getUserTravels(userDetails.getUser());
        model.addAttribute("travels", travels);
        return "post/create";
    }

    // 게시글 작성 처리
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam Long travelId,
        @RequestParam String title,
        @RequestParam String content) {
        Travel travel = travelService.getTravelById(travelId);
        Post post = postService.createPost(userDetails.getUser(), travel, title, content);

        return "redirect:/posts/upload/complete?postId=" + post.getPostId();
    }

    // 업로드 완료 페이지
    @GetMapping("/upload/complete")
    public String uploadComplete(@RequestParam Long postId, Model model) {
        model.addAttribute("postId", postId);
        return "post/upload-complete";
    }

    // 게시글 수정 페이지
    @GetMapping("/{postId}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        Model model) {
        Post post = postService.getPostById(postId);

        if (!post.isAuthor(userDetails.getUser())) {
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
        try {
            postService.updatePost(postId, title, content, userDetails.getUser());
        } catch (RuntimeException e) {
            return "redirect:/posts/" + postId + "?error=unauthorized";
        }

        return "redirect:/posts/" + postId;
    }

    // 게시글 삭제
    @PostMapping("/{postId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        Post post = postService.getPostById(postId);

        if (post.isAuthor(userDetails.getUser())) {
            postService.deletePost(post);
        }

        return "redirect:/posts";
    }

    // 좋아요 처리
    @PostMapping("/{postId}/like")
    public String like(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        try {
            postLikeService.like(post, userDetails.getUser());
        } catch (RuntimeException ignored) { }

        return "redirect:/posts/" + postId;
    }

    // 좋아요 취소 처리
    @PostMapping("/{postId}/unlike")
    public String unlike(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        try {
            postLikeService.unlike(post, userDetails.getUser());
        } catch (RuntimeException ignored) { }

        return "redirect:/posts/" + postId;
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public String createComment(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        @RequestParam String content) {
        Post post = postService.getPostById(postId);
        commentService.createComment(post, userDetails.getUser(), content);

        return "redirect:/posts/" + postId;
    }

    // 댓글 삭제
    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long commentId,
        @RequestParam Long postId) {
        commentService.deleteComment(commentId, userDetails.getUser());
        return "redirect:/posts/" + postId;
    }
}