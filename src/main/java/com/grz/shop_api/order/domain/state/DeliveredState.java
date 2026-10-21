package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.OrderStatus;

public class DeliveredState implements OrderState {

    @Override
    public void pay(OrderContext context) {
        throw new BusinessRuleException("El pedido ya fue entregado y pagado.");
    }

    @Override
    public void ship(OrderContext context) {
        throw new BusinessRuleException("El pedido ya ha sido entregado exitosamente al cliente.");
    }

    @Override
    public void deliver(OrderContext context) {
        throw new BusinessRuleException("El pedido ya fue registrado como entregado.");
    }

    @Override
    public void cancel(OrderContext context) {
        throw new BusinessRuleException("No es posible cancelar un pedido que ya fue entregado.");
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.DELIVERED;
    }
}
