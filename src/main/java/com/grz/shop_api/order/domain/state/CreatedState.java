package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.OrderStatus;

public class CreatedState implements OrderState {

    @Override
    public void pay(OrderContext context) {
        context.setState(new PaidState());
    }

    @Override
    public void ship(OrderContext context) {
        throw new BusinessRuleException("El pedido no puede ser enviado porque aún no ha sido pagado.");
    }

    @Override
    public void deliver(OrderContext context) {
        throw new BusinessRuleException("El pedido no puede ser entregado antes de ser pagado y enviado.");
    }

    @Override
    public void cancel(OrderContext context) {
        context.setState(new CancelledState());
    }

    @Override
    public OrderStatus getStatus() {
        return OrderStatus.CREATED;
    }
}
