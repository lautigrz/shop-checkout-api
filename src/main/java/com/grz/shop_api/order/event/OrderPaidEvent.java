package com.grz.shop_api.order.event;

import com.grz.shop_api.order.domain.Order;

import java.time.Instant;

public record OrderPaidEvent(
        Order order,
        Instant occurredOn
) {
    public static OrderPaidEvent of(Order order) {
        return new OrderPaidEvent(order, Instant.now());
    }
}
