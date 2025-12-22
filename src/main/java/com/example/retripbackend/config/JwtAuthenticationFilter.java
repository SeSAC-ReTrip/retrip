package com.example.retripbackend.config;

import org.springframework.security.core.userdetails.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // /auth/** 경로는 인증 없이 통과
        if (requestURI.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(JwtProvider.AUTHORIZATION_HEADER);

        // 토큰이 있으면 검증
        if (header != null && header.startsWith(JwtProvider.BEARER_PREFIX)) {
            String token = header.substring(JwtProvider.BEARER_PREFIX.length());

            if (jwtProvider.validateToken(token)) {
                Long userId = jwtProvider.getUserIdFromToken(token);

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_USER")
                );

                User principal = new User(String.valueOf(userId), "", authorities);
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                chain.doFilter(request, response);
                return;
            } else {
                // 유효하지 않은 토큰
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");

                Map<String, String> errorResponse = Map.of(
                    "message", "유효하지 않은 토큰입니다."
                );

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonResponse = objectMapper.writeValueAsString(errorResponse);
                response.getWriter().write(jsonResponse);
                return;
            }
        }

        // 토큰이 없는 경우 다음 필터로
        chain.doFilter(request, response);
    }
}