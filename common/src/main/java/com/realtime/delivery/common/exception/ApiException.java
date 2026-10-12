package com.realtime.delivery.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all business exceptions. Carries the HTTP status that the
 * shared exception handler will translate it into, so services no longer
 * have to map every failure to a single hard-coded status code.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
