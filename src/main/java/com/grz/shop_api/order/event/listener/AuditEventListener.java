package com.grz.shop_api.order.event.listener;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.event.OrderCreatedEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.infrastructure.OrderAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final OrderAuditLogRepository auditLogRepository;

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.order();
        log.info("[Observer/Auditoría Mongo] Registrando evento ORDER_CREATED para {}", order.getOrderNumber());

        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .eventType("ORDER_CREATED")
                .status(order.getStatus().name())
                .message("Orden creada con monto base $" + order.getTotalAmount())
                .details(Map.of(
                        "totalAmount", order.getTotalAmount(),
                        "discountAmount", order.getDiscountAmount(),
                        "finalAmount", order.getFinalAmount(),
                        "itemCount", order.getItems().size()
                ))
                .timestamp(event.occurredOn())
                .build();

        auditLogRepository.save(auditLog);
    }

    @EventListener
    public void handleOrderPaid(OrderPaidEvent event) {
        Order order = event.order();
        log.info("[Observer/Auditoría Mongo] Registrando evento ORDER_PAID para {}", order.getOrderNumber());

        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .eventType("ORDER_PAID")
                .status(order.getStatus().name())
                .message("Pago confirmado con método " + order.getPaymentMethod() + ". Transacción: " + order.getPaymentTransactionId())
                .details(Map.of(
                        "paymentMethod", order.getPaymentMethod().name(),
                        "transactionId", order.getPaymentTransactionId() != null ? order.getPaymentTransactionId() : "N/A",
                        "finalAmount", order.getFinalAmount()
                ))
                .timestamp(event.occurredOn())
                .build();

        auditLogRepository.save(auditLog);
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = event.order();
        log.info("[Observer/Auditoría Mongo] Registrando evento ORDER_CANCELLED para {}", order.getOrderNumber());

        OrderAuditLog auditLog = OrderAuditLog.builder()
                .orderId(order.getId().toString())
                .orderNumber(order.getOrderNumber())
                .eventType("ORDER_CANCELLED")
                .status(order.getStatus().name())
                .message("Orden cancelada. Motivo: " + event.reason())
                .details(Map.of("reason", event.reason()))
                .timestamp(event.occurredOn())
                .build();

        auditLogRepository.save(auditLog);
    }
}
