package com.grz.shop_api.order.application;

import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.PaymentMethod;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.infrastructure.OrderAuditLogRepository;
import com.grz.shop_api.order.infrastructure.OrderRepository;
import com.grz.shop_api.order.web.OrderMapper;
import com.grz.shop_api.order.web.dto.AuditLogResponse;
import com.grz.shop_api.order.web.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Servicio de Pedidos (OrderService)")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderAuditLogRepository auditLogRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId;
    private UUID userId;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-100")
                .userId(userId)
                .totalAmount(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("100.00"))
                .status(OrderStatus.PAID)
                .paymentMethod(PaymentMethod.CARD)
                .shippingAddress("Calle Falsa 123")
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        orderResponse = new OrderResponse(
                orderId,
                "ORD-100",
                userId,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                OrderStatus.PAID,
                PaymentMethod.CARD,
                "TX-1",
                "Calle Falsa 123",
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("getById: Debe retornar OrderResponse si el pedido existe")
    void getById_shouldReturnOrderResponse_whenFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.getById(orderId);

        assertThat(result).isNotNull();
        assertThat(result.orderNumber()).isEqualTo("ORD-100");
        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("getById: Debe lanzar ResourceNotFoundException si el pedido no existe")
    void getById_shouldThrowResourceNotFoundException_whenNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido no encontrado");
    }

    @Test
    @DisplayName("getUserOrders: Debe retornar la lista de pedidos del usuario mapeados")
    void getUserOrders_shouldReturnOrderResponseList() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        List<OrderResponse> result = orderService.getUserOrders(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderNumber()).isEqualTo("ORD-100");
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("updateStatus: Debe realizar transición a SHIPPED cuando el estado actual es PAID")
    void updateStatus_shouldTransitionToShipped_whenValid() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.updateStatus(orderId, OrderStatus.SHIPPED, null);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("updateStatus: Debe realizar transición a DELIVERED cuando el estado actual es SHIPPED")
    void updateStatus_shouldTransitionToDelivered_whenValid() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.updateStatus(orderId, OrderStatus.DELIVERED, null);

        assertThat(result).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("updateStatus: Debe cancelar la orden y publicar OrderCancelledEvent")
    void updateStatus_shouldTransitionToCancelledAndPublishEvent() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        orderService.updateStatus(orderId, OrderStatus.CANCELLED, "Cliente desistió");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Cliente desistió");
        assertThat(eventCaptor.getValue().order()).isEqualTo(order);
    }

    @Test
    @DisplayName("updateStatus: Debe lanzar IllegalArgumentException ante transición no soportada directamente")
    void updateStatus_shouldThrowIllegalArgumentException_whenStatusNotAllowed() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, OrderStatus.CREATED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transición directa no permitida");
    }

    @Test
    @DisplayName("getOrderAuditHistory: Debe retornar el historial de auditoría de MongoDB")
    void getOrderAuditHistory_shouldReturnAuditResponseList() {
        OrderAuditLog auditLog = OrderAuditLog.builder()
                .id("mongo-id-1")
                .orderId(orderId.toString())
                .orderNumber("ORD-100")
                .eventType("ORDER_CREATED")
                .status("CREATED")
                .message("Orden creada")
                .details(Map.of())
                .timestamp(Instant.now())
                .build();

        AuditLogResponse auditLogResponse = new AuditLogResponse(
                "mongo-id-1",
                orderId.toString(),
                "ORD-100",
                "ORDER_CREATED",
                "CREATED",
                "Orden creada",
                Map.of(),
                Instant.now()
        );

        when(auditLogRepository.findByOrderIdOrderByTimestampDesc(orderId.toString())).thenReturn(List.of(auditLog));
        when(orderMapper.toAuditResponseList(List.of(auditLog))).thenReturn(List.of(auditLogResponse));

        List<AuditLogResponse> history = orderService.getOrderAuditHistory(orderId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).eventType()).isEqualTo("ORDER_CREATED");
        verify(auditLogRepository).findByOrderIdOrderByTimestampDesc(orderId.toString());
    }
}
