package com.grz.shop_api.order.event;

import com.grz.shop_api.order.domain.Order;

import java.time.Instant;

public record OrderCancelledEvent(
        Order order,
        String reason,
        Instant occurredOn
) {
    public static OrderCancelledEvent of(Order order, String reason) {
        return new OrderCancelledEvent(order, reason, Instant.now());
    }
}
