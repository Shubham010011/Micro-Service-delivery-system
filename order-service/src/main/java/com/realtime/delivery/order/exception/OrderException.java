package com.realtime.delivery.order.exception;

import com.realtime.delivery.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class OrderException extends ApiException {

    private OrderException(HttpStatus status, String message) {
        super(status, message);
    }

    public static OrderException badRequest(String message) { return new OrderException(HttpStatus.BAD_REQUEST, message); }
    public static OrderException forbidden(String message)  { return new OrderException(HttpStatus.FORBIDDEN, message); }
    public static OrderException notFound(String message)   { return new OrderException(HttpStatus.NOT_FOUND, message); }
    public static OrderException conflict(String message)   { return new OrderException(HttpStatus.CONFLICT, message); }
}
