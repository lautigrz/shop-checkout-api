package com.grz.shop_api.order.application;

import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.state.OrderContext;
import com.grz.shop_api.order.domain.state.OrderState;
import com.grz.shop_api.order.domain.state.OrderStateFactory;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.infrastructure.OrderAuditLogRepository;
import com.grz.shop_api.order.infrastructure.OrderRepository;
import com.grz.shop_api.order.web.OrderMapper;
import com.grz.shop_api.order.web.dto.AuditLogResponse;
import com.grz.shop_api.order.web.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderAuditLogRepository auditLogRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        Order order = findEntityById(id);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, OrderStatus newStatus, String reason) {
        Order order = findEntityById(id);
        log.info("Actualizando estado de orden {} de {} a {}", order.getOrderNumber(), order.getStatus(), newStatus);


        OrderState currentState = OrderStateFactory.getState(order.getStatus());
        OrderContext context = new OrderContext(order, currentState);

        switch (newStatus) {
            case SHIPPED -> context.ship();
            case DELIVERED -> context.deliver();
            case CANCELLED -> {
                context.cancel();
                eventPublisher.publishEvent(OrderCancelledEvent.of(order, reason != null ? reason : "Cancelación manual"));
            }
            default -> throw new IllegalArgumentException("Transición directa no permitida hacia: " + newStatus);
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getOrderAuditHistory(UUID orderId) {
        List<OrderAuditLog> logs = auditLogRepository.findByOrderIdOrderByTimestampDesc(orderId.toString());
        return orderMapper.toAuditResponseList(logs);
    }

    @Transactional(readOnly = true)
    public Order findEntityById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }
}
