package com.grz.shop_api.order.domain.payment;

import java.time.Instant;

public record PaymentResult(
        boolean success,
        String transactionId,
        String message,
        Instant processedAt
) {
    public static PaymentResult success(String transactionId, String message) {
        return new PaymentResult(true, transactionId, message, Instant.now());
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, null, message, Instant.now());
    }
}
