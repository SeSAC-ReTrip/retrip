package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.entity.Travel;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
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
    // TODO: 가계부 팀원이 ReceiptService 구현하면 주입
    // private final ReceiptService receiptService;


      //가계부에서 생성된 여행 목록 조회 (선택 화면)
     //가계부 시스템에서 이미 생성된 Travel 엔티티들을 조회만 함
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();

        // TODO: 가계부 시스템과 연동 후 가계부에서 생성된 여행만 조회
        List<Travel> travels = travelService.getUserTravels(user);

        model.addAttribute("travels", travels);
        return "travel/list";  // 가계부 목록 (선택 화면)
    }


     // 선택한 여행의 영수증 목록 보기
     // 와이어프레임: Profile - 가계부 - detail (재목 화면)
    @GetMapping("/{travelId}")
    public String detail(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId,
        Model model) {
        User currentUser = userDetails.getUser();
        Travel travel = travelService.getTravelById(travelId);
        boolean isOwner = travel.isOwner(currentUser);

        // TODO: 가계부 팀원이 Receipt 기능 완성하면 주석 해제
        // List<Receipt> receipts = receiptService.getReceiptsByTravel(travelId);
        // model.addAttribute("receipts", receipts);

        // 임시: 빈 리스트 (가계부 팀원이 Receipt 구현 전까지)
        model.addAttribute("receipts", Collections.emptyList());
        model.addAttribute("travel", travel);
        model.addAttribute("isOwner", isOwner);

        return "travel/detail";  // 영수증 카드 목록
    }

     //여행 수정 폼
     // 가계부에서 이미 생성된 여행의 정보만 수정
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


    //여행 정보 수정 처리
    @PostMapping("/{travelId}/edit")
    public String edit(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId,
        @RequestParam String country,
        @RequestParam String city,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
        @RequestParam(required = false) String memo) {
        User currentUser = userDetails.getUser();

        try {
            travelService.updateTravel(travelId, currentUser, country, city, startDate, endDate, memo);
            return "redirect:/travels/" + travelId;
        } catch (IllegalArgumentException e) {
            return "redirect:/travels/" + travelId;
        }
    }


    //  여행 삭제
    @PostMapping("/{travelId}/delete")
    public String delete(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        @PathVariable Long travelId) {
        User currentUser = userDetails.getUser();

        try {
            travelService.deleteTravel(travelId, currentUser);
            return "redirect:/travels";
        } catch (IllegalArgumentException e) {
            return "redirect:/travels/" + travelId;
        }
    }

    /* ========================================
     * 아래 메서드들은 제거됨 (가계부에서 생성하므로)
     * ======================================== */

    // ❌ GET /travels/create - 제거됨
    // ❌ POST /travels/create - 제거됨
    // 이유: 여행은 가계부 시스템에서 영수증 업로드 시 자동 생성됨
}