package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.order.domain.OrderStatus;

public final class OrderStateFactory {

    private OrderStateFactory() {}

    public static OrderState getState(OrderStatus status) {
        if (status == null) {
            return new CreatedState();
        }
        return switch (status) {
            case CREATED -> new CreatedState();
            case PAID -> new PaidState();
            case SHIPPED -> new ShippedState();
            case DELIVERED -> new DeliveredState();
            case CANCELLED -> new CancelledState();
        };
    }
}
