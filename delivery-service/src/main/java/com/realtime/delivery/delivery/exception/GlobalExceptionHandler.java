package com.realtime.delivery.delivery.exception;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.common.exception.BaseExceptionHandler;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {

    /** Optimistic (@Version) or pessimistic lock loss: someone else changed the same assignment first. */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleConcurrency(ConcurrencyFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("This order was just updated by someone else. Please refresh and try again."));
    }
}
