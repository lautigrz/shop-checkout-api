package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.order.domain.Order;
import lombok.Getter;

@Getter
public class OrderContext {

    private final Order order;
    private OrderState currentState;

    public OrderContext(Order order, OrderState initialState) {
        this.order = order;
        this.currentState = initialState;
    }

    public void setState(OrderState state) {
        this.currentState = state;
        this.order.setStatus(state.getStatus());
    }

    public void pay() {
        currentState.pay(this);
    }

    public void ship() {
        currentState.ship(this);
    }

    public void deliver() {
        currentState.deliver(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }
}
