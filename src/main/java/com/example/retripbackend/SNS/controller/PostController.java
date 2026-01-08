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
import java.util.ArrayList;
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

        List<TravelWithCurrency> travelsWithCurrency = travels.stream()
            .map(travel -> {
                List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);
                String currency = receipts.stream()
                    .filter(r -> r.getCurrency() != null && !r.getCurrency().isEmpty())
                    .map(Receipt::getCurrency)
                    .findFirst()
                    .orElse("KRW");

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

        List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);

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
        @RequestParam(required = false) List<Long> receiptIds,
        @RequestParam(required = false) List<String> receiptContents,
        @RequestParam(value = "receiptImages[0]", required = false) MultipartFile[] receiptImages0,
        @RequestParam(value = "receiptImages[1]", required = false) MultipartFile[] receiptImages1,
        @RequestParam(value = "receiptImages[2]", required = false) MultipartFile[] receiptImages2,
        @RequestParam(value = "receiptImages[3]", required = false) MultipartFile[] receiptImages3,
        @RequestParam(value = "receiptImages[4]", required = false) MultipartFile[] receiptImages4) {
        try {
            Travel travel = travelService.getTravelById(travelId);

            List<MultipartFile[]> receiptImagesList = new ArrayList<>();
            if (receiptImages0 != null) receiptImagesList.add(receiptImages0);
            if (receiptImages1 != null) receiptImagesList.add(receiptImages1);
            if (receiptImages2 != null) receiptImagesList.add(receiptImages2);
            if (receiptImages3 != null) receiptImagesList.add(receiptImages3);
            if (receiptImages4 != null) receiptImagesList.add(receiptImages4);

            Post post = postService.createPostWithReceipts(userDetails.getUser(), travel, title,
                receiptIds, receiptContents, receiptImagesList);
            return "redirect:/posts/upload/complete?postId=" + post.getPostId();
        } catch (Exception e) {
            e.printStackTrace();
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
        List<PostImage> images = postService.getPostImages(post);

        Map<Integer, List<PostImage>> imagesByReceipt = new HashMap<>();
        for (PostImage image : images) {
            Integer receiptIndex = image.getReceiptIndex();
            if (receiptIndex != null) {
                imagesByReceipt.computeIfAbsent(receiptIndex, k -> new ArrayList<>()).add(image);
            }
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("images", images);
        model.addAttribute("imagesByReceipt", imagesByReceipt);
        model.addAttribute("isLiked", (currentUser != null) && postLikeService.isLiked(post, currentUser));
        model.addAttribute("isAuthor", (currentUser != null) && post.isAuthor(currentUser));
        model.addAttribute("googleMapApiKey", googleMapApiKey);

        return "post/content";
    }

    @GetMapping("/posts/{postId}")
    public String detail(@PathVariable Long postId) {
        return "redirect:/posts/" + postId + "/content";
    }

    @GetMapping("/posts/{postId}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        Model model) {
        if (userDetails == null) return "redirect:/login";

        Post post = postService.getPostById(postId);

        if (!post.isAuthor(userDetails.getUser())) {
            return "redirect:/posts/" + postId;
        }

        Travel travel = post.getTravel();
        List<PostImage> images = postService.getPostImages(post);
        List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);

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

        model.addAttribute("travel", travel);
        model.addAttribute("post", post);
        model.addAttribute("images", images);
        model.addAttribute("locations", locations);
        model.addAttribute("googleMapApiKey", googleMapApiKey);
        model.addAttribute("username", userDetails.getUser().getName());
        model.addAttribute("isEdit", true);

        return "post/detail";
    }

    @PostMapping("/posts/{postId}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId,
        @RequestParam String title,
        @RequestParam(required = false) List<Long> receiptIds,
        @RequestParam(required = false) List<String> receiptContents,
        @RequestParam(value = "receiptImages[0]", required = false) MultipartFile[] receiptImages0,
        @RequestParam(value = "receiptImages[1]", required = false) MultipartFile[] receiptImages1,
        @RequestParam(value = "receiptImages[2]", required = false) MultipartFile[] receiptImages2,
        @RequestParam(value = "receiptImages[3]", required = false) MultipartFile[] receiptImages3,
        @RequestParam(value = "receiptImages[4]", required = false) MultipartFile[] receiptImages4) {
        try {
            List<MultipartFile[]> receiptImagesList = new ArrayList<>();
            if (receiptImages0 != null) receiptImagesList.add(receiptImages0);
            if (receiptImages1 != null) receiptImagesList.add(receiptImages1);
            if (receiptImages2 != null) receiptImagesList.add(receiptImages2);
            if (receiptImages3 != null) receiptImagesList.add(receiptImages3);
            if (receiptImages4 != null) receiptImagesList.add(receiptImages4);

            postService.updatePostWithReceipts(postId, title, userDetails.getUser(),
                receiptIds, receiptContents, receiptImagesList);
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

    @ResponseBody
    @PostMapping("/posts/{postId}/like")
    public String toggleLike(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long postId) {
        if (userDetails == null) return "fail";

        User user = userDetails.getUser();
        Post post = postService.getPostById(postId);

        if (postLikeService.isLiked(post, user)) {
            postLikeService.unlike(post, user);
            return "unliked";
        } else {
            postLikeService.like(post, user);
            return "liked";
        }
    }

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