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

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final TravelService travelService;
    private final PostService postService;

    // 검색 페이지
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        if (keyword != null && !keyword.isBlank()) {
            // 도시 검색 (간단한 구현 - 실제로는 더 복잡한 검색 로직 필요)
            Page<Post> posts = postService.getPostsByCity(keyword, page, 10);
            model.addAttribute("posts", posts);
            model.addAttribute("keyword", keyword);
        }

        return "search/result";
    }

    // 인기 도시 목록
    @GetMapping("/cities/popular")
    public String popularCities(Model model) {
        List<Object[]> popularCities = travelService.getPopularCities();

        model.addAttribute("cities", popularCities);

        return "search/popular-cities";
    }

    // 도시별 게시글
    @GetMapping("/cities/{cityName}/posts")
    public String cityPosts(@PathVariable String cityName,
        @RequestParam(defaultValue = "0") int page,
        Model model) {
        Page<Post> posts = postService.getPostsByCity(cityName, page, 10);

        model.addAttribute("city", cityName);
        model.addAttribute("posts", posts);

        return "search/city-posts";
    }
}
