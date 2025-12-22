package com.example.retripbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== 인증 관련 =====
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    NAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 이름입니다."),
    INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // ===== JWT 토큰 관련 =====
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다."),
    TOKEN_UNSUPPORTED(HttpStatus.UNAUTHORIZED, "지원하지 않는 토큰 형식입니다."),
    TOKEN_MALFORMED(HttpStatus.UNAUTHORIZED, "잘못된 토큰입니다."),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "토큰 서명이 유효하지 않습니다."),
    TOKEN_ILLEGAL_ARGUMENT(HttpStatus.UNAUTHORIZED, "토큰이 비어있습니다."),

    // ===== 사용자 관련 =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // ===== 권한 관련 =====
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_RESOURCE_OWNER(HttpStatus.FORBIDDEN, "본인의 리소스만 수정/삭제할 수 있습니다."),

    // ===== Validation 에러 =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // ===== 서버 에러 =====
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
