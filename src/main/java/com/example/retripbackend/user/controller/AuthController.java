package com.example.retripbackend.user.controller;

import com.example.retripbackend.user.entity.SignupForm;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.repository.UserRepository;
import com.example.retripbackend.user.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 메인 페이지
    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "logout", required = false) String logout,
        Model model) {
        if (error != null) {
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.");
        }
        return "login";
    }

    // 회원가입 페이지
    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupForm form,
        BindingResult bindingResult,
        Model model) {

        // Validation 에러
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(form.getEmail())) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "signup";
        }

        // 비밀번호 확인
        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            model.addAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "signup";
        }

        // 회원가입 처리
        String encodedPassword = passwordEncoder.encode(form.getPassword());
        User user = User.of(form.getEmail(), encodedPassword, form.getName());
        userRepository.save(user);

        log.info("새로운 사용자 가입: {}", form.getEmail());

        return "redirect:/login?signup=true";
    }

    // 로그인 후 홈 페이지
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal CustomUserDetailsService.CustomUserDetails userDetails,
        Model model) {
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        return "home";
    }
}
