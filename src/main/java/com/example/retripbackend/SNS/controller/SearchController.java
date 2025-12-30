package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.service.PostService;
import com.example.retripbackend.SNS.service.TravelService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    /**
     * 검색 초기 화면
     * - 실제 게시물이 많은 도시들을 DB에서 조회하여 표시
     * GET /search
     */
    @GetMapping("/search")
    public String searchPage(Model model) {
        // DB에서 실제 게시물이 많은 도시 상위 16개 조회
        List<String> popularCities = postService.getTopCitiesByPostCount(16);

        // 만약 게시물이 아직 없으면 빈 리스트가 표시됨
        model.addAttribute("popularCities", popularCities);
        return "search/explore";
    }

    /**
     * 검색 결과 화면
     * - 도시명으로 게시물 검색
     * GET /search/results?keyword=파리&page=0
     */
    @GetMapping("/search/results")
    public String searchResults(@RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        // 빈 검색어 처리
        if (keyword == null || keyword.isBlank()) {
            return "redirect:/search/explore";
        }

        // 도시명으로 게시물 검색 (Travel의 city 필드 기준)
        Page<Post> posts = postService.searchPostsByCity(keyword, page, 10);

        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", posts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", posts.getTotalPages());

        return "search/results";
    }

    /**
     * 인기 도시 태그 클릭 시 바로 검색 결과로 이동
     * GET /search/city?city=일본
     *
     * ✅ RedirectAttributes를 사용하면 Spring이 자동으로 URL 인코딩 처리
     */
    @GetMapping("/search/city")
    public String searchByCity(@RequestParam String city,
        RedirectAttributes redirectAttributes) {
        // Spring이 자동으로 한글을 URL 인코딩 처리
        redirectAttributes.addAttribute("keyword", city);
        return "redirect:/search/results";
    }
}