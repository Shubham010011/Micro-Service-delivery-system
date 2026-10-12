package com.realtime.delivery.delivery.exception;

import com.realtime.delivery.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DeliveryException extends ApiException {

    private DeliveryException(HttpStatus status, String message) {
        super(status, message);
    }

    public static DeliveryException badRequest(String message)  { return new DeliveryException(HttpStatus.BAD_REQUEST, message); }
    public static DeliveryException forbidden(String message)   { return new DeliveryException(HttpStatus.FORBIDDEN, message); }
    public static DeliveryException notFound(String message)    { return new DeliveryException(HttpStatus.NOT_FOUND, message); }
    public static DeliveryException conflict(String message)    { return new DeliveryException(HttpStatus.CONFLICT, message); }
    public static DeliveryException unavailable(String message) { return new DeliveryException(HttpStatus.SERVICE_UNAVAILABLE, message); }
}
