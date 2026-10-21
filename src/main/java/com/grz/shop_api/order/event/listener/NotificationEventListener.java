package com.grz.shop_api.order.event.listener;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationEventListener {

    @EventListener
    public void handleOrderPaid(OrderPaidEvent event) {
        Order order = event.order();
        log.info("[Observer/Notificación] Enviando email de confirmación para pedido {}: ¡Tu pago de ${} fue aprobado!",
                order.getOrderNumber(), order.getFinalAmount());
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = event.order();
        log.info("[Observer/Notificación] Enviando email de cancelación para pedido {}: El pedido ha sido cancelado ({})",
                order.getOrderNumber(), event.reason());
    }
}
