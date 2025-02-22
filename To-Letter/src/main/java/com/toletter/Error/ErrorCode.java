package com.toletter.Error;

import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public enum ErrorCode {

    RUNTIME_EXCEPTION(HttpStatus.BAD_REQUEST, 400, "400 Bad Request"),
    UNAUTHORIZED_EXCEPTION(HttpStatus.UNAUTHORIZED, 401, "401 UnAuthorized"), // 비인증(클라이언트X)
    FORBIDDEN_EXCEPTION(HttpStatus.FORBIDDEN, 403, "403 Forbidden"), // 미승인(클라이언트O)
    NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND, 404, "404 Not Found"), // 서버 오류
    CONFLICT_EXCEPTION(HttpStatus.CONFLICT, 409, "409 Conflict"), // 요청과 서버 충돌
    INTERNAL_SERVER_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR, 500, "500 server error") // 서버 에러
    ;

    private final HttpStatus status;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
