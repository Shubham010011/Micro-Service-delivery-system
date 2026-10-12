package com.realtime.delivery.common.constants;

/**
 * Application-wide roles. Encoded as a claim inside the JWT and also
 * forwarded downstream by the API Gateway as the "X-User-Role" header.
 */
public enum Role {
    CUSTOMER,
    DELIVERY_PARTNER,
    ADMIN
}
