package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/travels")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;
    private final PostService postService;

    // 여행 목록
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        List<Travel> travels = travelService.getUserTravels(user);

        model.addAttribute("travels", travels);

        return "travel/list";
    }

    // 여행 상세
    @GetMapping("/{travelId}")
    public String detail(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId,
        Model model) {
        User currentUser = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);
        List<Post> posts = postService.getTravelPosts(travelId);
        boolean isOwner = travel.isOwner(currentUser);

        model.addAttribute("travel", travel);
        model.addAttribute("posts", posts);
        model.addAttribute("isOwner", isOwner);

        return "travel/detail";
    }

    // 여행 생성 페이지
    @GetMapping("/create")
    public String createPage() {
        return "travel/create";
    }

    // 여행 생성 처리
    @PostMapping("/create")
    public String create(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @RequestParam String country,
        @RequestParam String city,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(required = false) String memo) {
        User user = userDetails.getUser();

        Travel travel = travelService.createTravel(user, country, city, startDate, endDate, memo);

        return "redirect:/travels/" + travel.getTravelId();
    }

    // 여행 수정 페이지
    @GetMapping("/{travelId}/edit")
    public String editPage(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId,
        Model model) {
        User currentUser = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);

        if (!travel.isOwner(currentUser)) {
            return "redirect:/travels/" + travelId;
        }

        model.addAttribute("travel", travel);

        return "travel/edit";
    }

    // 여행 수정 처리
    @PostMapping("/{travelId}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId,
        @RequestParam String country,
        @RequestParam String city,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(required = false) String memo) {
        User currentUser = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);

        if (!travel.isOwner(currentUser)) {
            return "redirect:/travels/" + travelId;
        }

        travelService.updateTravel(travel, country, city, startDate, endDate, memo);

        return "redirect:/travels/" + travelId;
    }

    // 여행 삭제
    @PostMapping("/{travelId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId) {
        User currentUser = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);

        if (!travel.isOwner(currentUser)) {
            return "redirect:/travels/" + travelId;
        }

        travelService.deleteTravel(travel);

        return "redirect:/travels";
    }
}









