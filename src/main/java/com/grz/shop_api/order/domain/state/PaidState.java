package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.OrderStatus;

public class PaidState implements OrderState {

    @Override
    public void pay(OrderContext context) {
        throw new BusinessRuleException("El pedido ya fue pagado previamente.");
    }

    @Override
    public void ship(OrderContext context) {
        context.setState(new ShippedState());
    }

    @Override
    public void deliver(OrderContext context) {
        throw new BusinessRuleException("El pedido debe ser despachado/enviado antes de poder marcarse como entregado.");
    }

    @Override
    public void cancel(OrderContext context) {
        context.setState(new CancelledState());
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.PAID;
    }
}
