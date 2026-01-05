package com.example.retripbackend.user.controller;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.repository.PostLikeRepository;
import com.example.retripbackend.SNS.service.FollowService;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.receipt.entity.Receipt;
import com.example.retripbackend.receipt.service.ReceiptService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import com.example.retripbackend.user.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final TravelService travelService;
    private final FollowService followService;
    private final PostLikeRepository postLikeRepository;
    private final ReceiptService receiptService;

    // 내 정보 조회
    @GetMapping("/me")
    public String myProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        UserService.UserStats stats = userService.getUserStats(user);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);

        return "user/profile";
    }

    // 내 정보 수정 페이지
    @GetMapping("/me/edit")
    public String editProfilePage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        return "user/edit";
    }

    // 내 정보 수정 처리
    @PostMapping("/me/edit")
    public String editProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam String name,
        @RequestParam(required = false) String bio,
        @RequestParam(required = false) String profileImage) {
        User user = userDetails.getUser();
        userService.updateProfile(user, name, bio, profileImage);
        return "redirect:/users/me";
    }

    // 비밀번호 변경 페이지
    @GetMapping("/me/password")
    public String changePasswordPage() {
        return "user/password";
    }

    // 비밀번호 변경 처리
    @PostMapping("/me/password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam String currentPassword,
        @RequestParam String newPassword,
        @RequestParam String confirmPassword,
        Model model) {
        try {
            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("error", "새 비밀번호가 일치하지 않습니다.");
                return "user/password";
            }

            User user = userDetails.getUser();
            userService.changePassword(user, currentPassword, newPassword);

            return "redirect:/users/me?passwordChanged=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "user/password";
        }
    }

    // 회원 탈퇴 처리
    @PostMapping("/me/delete")
    public String deleteAccount(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        userService.deleteAccount(user);
        return "redirect:/logout";
    }

    // 내 게시글 목록
    @GetMapping("/me/posts")
    public String myPosts(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        User user = userDetails.getUser();
        Page<Post> posts = postService.getUserPosts(user, page, 10);

        model.addAttribute("user", user);
        model.addAttribute("posts", posts);

        return "user/posts";
    }

    // 내 여행 목록
    @GetMapping("/me/travels")
    public String myTravels(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        List<Travel> travels = travelService.getUserTravels(user);

        model.addAttribute("user", user);
        model.addAttribute("travels", travels);

        return "user/travels";
    }

    // 가계부 페이지
    @GetMapping("/me/account")
    public String myAccount() {
        return "profile-account/profile-account";
    }

    // 가계부 생성 페이지
    @GetMapping("/me/account/create")
    public String createAccountPage() {
        return "profile-account/profile-account-create";
    }

    // 가계부 영수증 선택 페이지
    @GetMapping("/me/account/select")
    public String selectAccountPage() {
        return "profile-account/profile-account-select";
    }

    // 가계부 상세 페이지
    @GetMapping("/me/account/detail")
    public String accountDetailPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();

        List<Travel> userTravels = travelService.getUserTravels(user);

        if (!userTravels.isEmpty()) {
            Travel travel = userTravels.get(0);
            List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);

            model.addAttribute("travel", travel);
            model.addAttribute("receipts", receipts);

            model.addAttribute("pageTitle", travel.getCity() + " 여행");
            model.addAttribute("destination", travel.getCity() + ", " + travel.getCountry());
            model.addAttribute("totalAmount", travel.getTotalAmount());
        } else {
            model.addAttribute("receipts", List.of());
            model.addAttribute("pageTitle", "가계부");
            model.addAttribute("destination", "여행지");
            model.addAttribute("totalAmount", 0);
        }

        return "profile-account/profile-account-detail";
    }

    // 좋아요 리스트 조회
    @GetMapping("/me/liked")
    public String myLikes(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        if (userDetails == null) return "redirect:/login";
        User currentUser = userDetails.getUser();

        // fetch join 메서드를 사용하여 post.user 정보까지 한 번에 로딩
        List<Post> likedPosts = postLikeRepository.findByUserWithPostAndUser(currentUser)
            .stream()
            .map(postLike -> postLike.getPost())
            .collect(Collectors.toList());

        model.addAttribute("likedPosts", likedPosts);

        return "user/profile-likeLists";
    }

    // 특정 사용자 프로필 조회
    @GetMapping("/{userId}")
    public String userProfile(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long userId,
        Model model) {
        User currentUser = userDetails.getUser();
        User targetUser = userService.findById(userId);

        UserService.UserStats stats = userService.getUserStats(targetUser);
        boolean isFollowing = followService.isFollowing(currentUser, targetUser);

        model.addAttribute("user", targetUser);
        model.addAttribute("stats", stats);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("isMe", currentUser.getUserId().equals(userId));

        return "user/profile";
    }

    // 특정 사용자의 게시글 목록
    @GetMapping("/{userId}/posts")
    public String userPosts(@PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        User user = userService.findById(userId);
        Page<Post> posts = postService.getUserPosts(user, page, 10);

        model.addAttribute("user", user);
        model.addAttribute("posts", posts);

        return "user/posts";
    }

    // 팔로우
    @PostMapping("/{userId}/follow")
    public String follow(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long userId) {
        User follower = userDetails.getUser();
        User following = userService.findById(userId);

        followService.follow(follower, following);

        return "redirect:/users/" + userId;
    }

    // 언팔로우
    @PostMapping("/{userId}/unfollow")
    public String unfollow(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long userId) {
        User follower = userDetails.getUser();
        User following = userService.findById(userId);

        followService.unfollow(follower, following);

        return "redirect:/users/" + userId;
    }

    // 팔로워 목록
    @GetMapping("/{userId}/followers")
    public String followers(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId);
        List<User> followers = followService.getFollowers(user);

        model.addAttribute("user", user);
        model.addAttribute("followers", followers);
        model.addAttribute("type", "followers");

        return "user/follow-list";
    }

    // 팔로잉 목록
    @GetMapping("/{userId}/followings")
    public String followings(@PathVariable Long userId, Model model) {
        User user = userService.findById(userId);
        List<User> followings = followService.getFollowings(user);

        model.addAttribute("user", user);
        model.addAttribute("followings", followings);
        model.addAttribute("type", "followings");

        return "user/follow-list";
    }
}