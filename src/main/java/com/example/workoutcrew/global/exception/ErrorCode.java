package com.example.workoutcrew.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "요청을 수행할 권한이 없습니다."),
    INVALID_CREW_PASSWORD(HttpStatus.FORBIDDEN, "크루에 가입할 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    CREW_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    MEMBERSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 값입니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 값입니다."),
    CREW_NAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 값입니다."),
    ALREADY_CREW_MEMBER(HttpStatus.CONFLICT, "이미 가입한 크루입니다."),
    CREW_FULL(HttpStatus.CONFLICT, "크루 정원이 가득 찼습니다."),
    MAX_USERS_BELOW_CURRENT(HttpStatus.CONFLICT, "현재 크루원 수보다 최대 인원을 작게 설정할 수 없습니다."),
    INVALID_CREW_STATE(HttpStatus.CONFLICT, "현재 크루 상태에서는 요청을 처리할 수 없습니다."),
    CONCURRENT_REQUEST_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 충돌했습니다. 다시 시도해 주세요."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    MEDIA_TYPE_NOT_SUPPORTED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
