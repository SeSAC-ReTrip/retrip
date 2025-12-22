package com.example.retripbackend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AuthDto {

    // 회원가입 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
            message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
        private String password;

        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다.")
        private String name;
    }

    // 로그인 요청
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    // 로그인 응답
    @Getter
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private UserInfo user;

        @Getter
        @AllArgsConstructor
        public static class UserInfo {
            private Long userId;
            private String name;
            private String profileImage;
        }
    }
}