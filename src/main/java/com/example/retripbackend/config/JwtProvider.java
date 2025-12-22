package com.example.retripbackend.config;


import com.example.retripbackend.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.security.SignatureException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final SecretKey secretKey;
    private final long ACCESS_TOKEN_EXPIRY = 1000L * 60 * 60 * 24 * 7; // 7일

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public JwtProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(Long userId) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        if (blacklistedTokens.contains(token)) {
            log.warn(ErrorCode.TOKEN_BLACKLISTED.getMessage());
            return false;
        }

        try {
            Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn(ErrorCode.TOKEN_EXPIRED.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn(ErrorCode.TOKEN_UNSUPPORTED.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn(ErrorCode.TOKEN_MALFORMED.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn(ErrorCode.TOKEN_ILLEGAL_ARGUMENT.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 예상치 못한 오류가 발생했습니다.", e);
            return false;
        }
    }

    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
        log.info("토큰이 블랙리스트에 추가되었습니다.");
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.warn("토큰 파싱 실패: {}", e.getMessage());
            throw e;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return Long.parseLong(claims.getSubject());
    }
}








