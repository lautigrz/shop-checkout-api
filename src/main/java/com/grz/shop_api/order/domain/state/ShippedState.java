package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.OrderStatus;

public class ShippedState implements OrderState {

    @Override
    public void pay(OrderContext context) {
        throw new BusinessRuleException("El pedido ya está pagado y enviado.");
    }

    @Override
    public void ship(OrderContext context) {
        throw new BusinessRuleException("El pedido ya se encuentra en tránsito de envío.");
    }

    @Override
    public void deliver(OrderContext context) {
        context.setState(new DeliveredState());
    }

    @Override
    public void cancel(OrderContext context) {
        throw new BusinessRuleException("No es posible cancelar un pedido que ya se encuentra en tránsito.");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.SHIPPED;
    }
}
