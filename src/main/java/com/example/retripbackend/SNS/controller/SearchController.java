package com.example.retripbackend.SNS.controller;

import com.example.retripbackend.SNS.entity.Post;
import com.example.retripbackend.SNS.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    // Explore 메인 화면
    @GetMapping("/explore")
    public String explorePage(@RequestParam(defaultValue = "latest") String sort,
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

        return "search/explore";
    }

    //Search 검색 입력 화면
    @GetMapping("/search")
    public String searchInputPage(Model model) {
        // 인기 도시 리스트 상위 16개
        List<String> popularCities = postService.getTopCitiesByPostCount(16);
        model.addAttribute("popularCities", popularCities);
        return "search/explore-search";
    }

    //특정 도시 검색 결과 피드
    @GetMapping("/search/results")
    public String searchResults(@RequestParam String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        if (keyword == null || keyword.isBlank()) {
            return "redirect:/search";
        }

        Page<Post> posts = postService.searchPostsByCity(keyword, page, 10);
        model.addAttribute("keyword", keyword);
        model.addAttribute("posts", posts);

        return "search/results";
    }
}