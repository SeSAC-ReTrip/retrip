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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

@Slf4j
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

    // 가계부 생성 처리
    @PostMapping("/me/account/create")
    public String createAccount(
        @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam String country,
        @RequestParam String city,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam String title) {
        
        User user = userDetails.getUser();
        
        log.info("========== 가계부 생성 요청 ==========");
        log.info("사용자 정보:");
        log.info("  - user_id: {}", user.getUserId());
        log.info("  - email: {}", user.getEmail());
        log.info("  - name: {}", user.getName());
        log.info("입력받은 데이터:");
        log.info("  - country: [{}] (길이: {})", country, country != null ? country.length() : 0);
        log.info("  - city: [{}] (길이: {})", city, city != null ? city.length() : 0);
        log.info("  - startDate: {}", startDate);
        log.info("  - endDate: {}", endDate);
        log.info("  - title: {}", title);
        
        // Travel 생성
        Travel travel = travelService.createTravel(
            user,
            country,
            city,
            title,
            startDate,
            endDate,
            null  // memo는 null
        );
        
        log.info("========== Travel 엔티티 저장 완료 ==========");
        log.info("저장된 Travel 정보:");
        log.info("  - travelId: {}", travel.getTravelId());
        log.info("  - user_id: {}", travel.getUser().getUserId());
        log.info("  - country: {}", travel.getCountry());
        log.info("  - city: {}", travel.getCity());
        log.info("  - title: {}", travel.getTitle());
        log.info("  - startDate: {}", travel.getStartDate());
        log.info("  - endDate: {}", travel.getEndDate());
        log.info("  - createdAt: {}", travel.getCreatedAt());
        log.info("  - updatedAt: {}", travel.getUpdatedAt());
        log.info("===========================================");
        
        // 생성된 Travel ID를 파라미터로 전달하여 select 페이지로 이동
        return "redirect:/users/me/account/select?travelId=" + travel.getTravelId();
    }

    // 가계부 영수증 선택 페이지
    @GetMapping("/me/account/select")
    public String selectAccountPage() {
        return "profile-account/profile-account-select";
    }

    // 가계부 상세 페이지
    @GetMapping("/me/account/detail")
    public String accountDetailPage(
        @AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam(required = false) Long travelId,
        Model model) {
        User user = userDetails.getUser();
        
        Travel travel;
        
        // travelId가 제공된 경우 해당 Travel 조회, 없으면 최근 Travel 사용
        if (travelId != null) {
            travel = travelService.getTravelById(travelId);
            
            // 권한 체크: 본인의 Travel인지 확인
            if (!travel.isOwner(user)) {
                log.warn("권한 없는 Travel 접근 시도: travelId={}, userId={}", travelId, user.getUserId());
                // 권한이 없으면 최근 Travel 사용
                List<Travel> userTravels = travelService.getUserTravels(user);
                if (!userTravels.isEmpty()) {
                    travel = userTravels.get(0);
                } else {
                    travel = null;
                }
            }
        } else {
            // travelId가 없으면 사용자의 최근 Travel 찾기
            List<Travel> userTravels = travelService.getUserTravels(user);
            if (!userTravels.isEmpty()) {
                travel = userTravels.get(0);
            } else {
                travel = null;
            }
        }
        
        if (travel != null) {
            // 해당 Travel의 영수증 목록 조회
            List<Receipt> receipts = receiptService.getReceiptsByTravel(travel);
            
            model.addAttribute("travel", travel);
            model.addAttribute("receipts", receipts);
            
            // 페이지 헤더 정보 설정
            // 사용자가 입력한 가계부 제목을 그대로 사용
            model.addAttribute("pageTitle", travel.getTitle());
            model.addAttribute("destination", travel.getCity() + ", " + travel.getCountry());
            model.addAttribute("totalAmount", travel.getTotalAmount());
            
            log.info("가계부 상세 페이지 조회: travelId={}, title={}, city={}, country={}, startDate={}, endDate={}", 
                travel.getTravelId(), travel.getTitle(), travel.getCity(), travel.getCountry(), 
                travel.getStartDate(), travel.getEndDate());
        } else {
            // Travel이 없으면 빈 리스트
            model.addAttribute("receipts", List.of());
            model.addAttribute("pageTitle", "가계부");
            model.addAttribute("destination", "여행지");
            model.addAttribute("totalAmount", 0);
        }
        
        return "profile-account/profile-account-detail";
    }

    //좋아요 리스트
    @GetMapping("/me/liked")
    public String myLikes(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User currentUser = userDetails.getUser();

        List<Post> likedPosts = postLikeRepository.findByUser(currentUser)
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









