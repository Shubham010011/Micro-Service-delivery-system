package com.realtime.delivery.auth.exception;

import com.realtime.delivery.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AuthException extends ApiException {

    private AuthException(HttpStatus status, String message) {
        super(status, message);
    }

    public static AuthException badRequest(String message)   { return new AuthException(HttpStatus.BAD_REQUEST, message); }
    public static AuthException unauthorized(String message) { return new AuthException(HttpStatus.UNAUTHORIZED, message); }
    public static AuthException forbidden(String message)    { return new AuthException(HttpStatus.FORBIDDEN, message); }
    public static AuthException notFound(String message)     { return new AuthException(HttpStatus.NOT_FOUND, message); }
    public static AuthException conflict(String message)     { return new AuthException(HttpStatus.CONFLICT, message); }
    public static AuthException unavailable(String message)  { return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
