package com.grz.shop_api.order.event;

import com.grz.shop_api.order.domain.Order;

import java.time.Instant;

public record OrderCreatedEvent(
        Order order,
        Instant occurredOn
) {
    public static OrderCreatedEvent of(Order order) {
        return new OrderCreatedEvent(order, Instant.now());
    }
}
