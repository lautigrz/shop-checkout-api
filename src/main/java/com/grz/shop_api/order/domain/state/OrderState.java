package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.order.domain.OrderStatus;

public interface OrderState {
    void pay(OrderContext context);
    void ship(OrderContext context);
    void deliver(OrderContext context);
    void cancel(OrderContext context);
    OrderStatus getStatus();
}
