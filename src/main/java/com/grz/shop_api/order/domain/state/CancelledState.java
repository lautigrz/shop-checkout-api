package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.OrderStatus;

public class CancelledState implements OrderState {

    @Override
    public void pay(OrderContext context) {
        throw new BusinessRuleException("No se puede pagar un pedido que ha sido cancelado.");
    }

    @Override
    public void ship(OrderContext context) {
        throw new BusinessRuleException("No se puede enviar un pedido cancelado.");
    }

    @Override
    public void deliver(OrderContext context) {
        throw new BusinessRuleException("No se puede entregar un pedido cancelado.");
    }

    @Override
    public void cancel(OrderContext context) {
        throw new BusinessRuleException("El pedido ya se encuentra cancelado.");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CANCELLED;
    }
}
