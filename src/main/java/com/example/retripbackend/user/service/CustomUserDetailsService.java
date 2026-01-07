package com.example.retripbackend.user.service;

import com.example.retripbackend.user.entity.User;
import com.example.retripbackend.user.repository.UserRepository;
import java.util.Collection;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override // 이메일로 사용자를 찾아서 인증 정보 반환
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return new CustomUserDetails(user);
    }

    // Spring Security User 대신 커스텀 UserDetails 구현
    @RequiredArgsConstructor
    public static class CustomUserDetails implements UserDetails {
        private final User user;

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        @Override // Spring Security가 로그인 시 비밀번호 검증할 때 사용
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true; // 계정 만료 안됨
        }

        @Override
        public boolean isAccountNonLocked() {
            return true; // 계정 잠금 안됨
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true; // 비밀번호 만료 안됨
        }

        @Override
        public boolean isEnabled() {
            return true; // 계정 활성화 됨
        }

        // User 엔티티 전체 접근용
        public User getUser() {
            return user;
        }
    }
}
