package com.example.retripbackend.user.service;

import com.example.retripbackend.config.JwtProvider;
import com.example.retripbackend.exception.ApiException;
import com.example.retripbackend.exception.ErrorCode;
import com.example.retripbackend.user.dto.AuthDto;
import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(AuthDto.SignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 사용자 생성
        User user = User.of(
            request.getEmail(),
            encodedPassword,
            request.getName()
        );

        userRepository.save(user);
        log.info("새로운 사용자 가입: email={}, name={}", request.getEmail(), request.getName());
    }

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Access Token 생성
        String accessToken = jwtProvider.generateAccessToken(user.getUserId());

        // 응답 생성
        AuthDto.LoginResponse.UserInfo userInfo = new AuthDto.LoginResponse.UserInfo(
            user.getUserId(),
            user.getName(),
            user.getProfileImage()
        );

        log.info("사용자 로그인: email={}, name={}", user.getEmail(), user.getName());
        return new AuthDto.LoginResponse(accessToken, userInfo);
    }

    @Transactional
    public void logout(String token) {
        // Bearer 제거
        if (token.startsWith(JwtProvider.BEARER_PREFIX)) {
            token = token.substring(JwtProvider.BEARER_PREFIX.length());
        }

        // 토큰 블랙리스트에 추가
        jwtProvider.invalidateToken(token);
        log.info("사용자 로그아웃 완료");
    }
}
