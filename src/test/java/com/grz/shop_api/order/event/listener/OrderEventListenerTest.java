package com.grz.shop_api.order.event.listener;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderItem;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.PaymentMethod;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.event.OrderCreatedEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.infrastructure.OrderAuditLogRepository;
import com.grz.shop_api.product.application.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Patrón Observer (Listeners de Eventos de Pedido)")
class OrderEventListenerTest {

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-2026-001")
                .userId(UUID.randomUUID())
                .totalAmount(new BigDecimal("250.00"))
                .discountAmount(new BigDecimal("25.00"))
                .finalAmount(new BigDecimal("225.00"))
                .status(OrderStatus.PAID)
                .paymentMethod(PaymentMethod.CARD)
                .paymentTransactionId("TX-12345")
                .shippingAddress("Av. Libertador 1200")
                .items(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem item1 = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId("prod-mongo-1")
                .productName("Camiseta deportiva")
                .unitPrice(new BigDecimal("100.00"))
                .quantity(2)
                .subtotal(new BigDecimal("200.00"))
                .build();

        OrderItem item2 = OrderItem.builder()
                .id(UUID.randomUUID())
                .productId("prod-mongo-2")
                .productName("Medias de compresión")
                .unitPrice(new BigDecimal("25.00"))
                .quantity(2)
                .subtotal(new BigDecimal("50.00"))
                .build();

        sampleOrder.addItem(item1);
        sampleOrder.addItem(item2);
    }

    @Nested
    @DisplayName("StockEventListener - Sincronización de Inventario")
    class StockEventListenerTests {

        @Mock
        private ProductService productService;

        private StockEventListener stockEventListener;

        @BeforeEach
        void init() {
            stockEventListener = new StockEventListener(productService);
        }

        @Test
        @DisplayName("handleOrderPaid: Debe descontar stock para cada item de la orden")
        void handleOrderPaid_shouldDeductStockForEachItem() {
            OrderPaidEvent event = OrderPaidEvent.of(sampleOrder);

            stockEventListener.handleOrderPaid(event);

            verify(productService).deductStock("prod-mongo-1", 2);
            verify(productService).deductStock("prod-mongo-2", 2);
            verify(productService, times(2)).deductStock(anyString(), anyInt());
        }

        @Test
        @DisplayName("handleOrderPaid: Debe ser tolerante a fallos si un producto falla al descontar stock")
        void handleOrderPaid_shouldHandleExceptionGracefully() {
            doThrow(new RuntimeException("Stock error")).when(productService).deductStock("prod-mongo-1", 2);
            OrderPaidEvent event = OrderPaidEvent.of(sampleOrder);

            assertDoesNotThrow(() -> stockEventListener.handleOrderPaid(event));
            verify(productService).deductStock("prod-mongo-2", 2);
        }

        @Test
        @DisplayName("handleOrderCancelled: Debe reintegrar stock para cada item de la orden")
        void handleOrderCancelled_shouldRestoreStockForEachItem() {
            OrderCancelledEvent event = OrderCancelledEvent.of(sampleOrder, "Usuario solicitó cancelación");

            stockEventListener.handleOrderCancelled(event);

            verify(productService).restoreStock("prod-mongo-1", 2);
            verify(productService).restoreStock("prod-mongo-2", 2);
            verify(productService, times(2)).restoreStock(anyString(), anyInt());
        }

        @Test
        @DisplayName("handleOrderCancelled: Debe ser tolerante a fallos si un producto falla al reintegrar")
        void handleOrderCancelled_shouldHandleExceptionGracefully() {
            doThrow(new RuntimeException("Error inesperado")).when(productService).restoreStock("prod-mongo-1", 2);
            OrderCancelledEvent event = OrderCancelledEvent.of(sampleOrder, "Cancelado por falta de pago");

            assertDoesNotThrow(() -> stockEventListener.handleOrderCancelled(event));
            verify(productService).restoreStock("prod-mongo-2", 2);
        }
    }

    @Nested
    @DisplayName("AuditEventListener - Auditoría en MongoDB")
    class AuditEventListenerTests {

        @Mock
        private OrderAuditLogRepository auditLogRepository;

        private AuditEventListener auditEventListener;

        @BeforeEach
        void init() {
            auditEventListener = new AuditEventListener(auditLogRepository);
        }

        @Test
        @DisplayName("handleOrderCreated: Debe persistir registro ORDER_CREATED con detalles de montos")
        void handleOrderCreated_shouldPersistAuditRecord() {
            OrderCreatedEvent event = OrderCreatedEvent.of(sampleOrder);

            auditEventListener.handleOrderCreated(event);

            ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            OrderAuditLog log = captor.getValue();
            assertThat(log.getOrderId()).isEqualTo(sampleOrder.getId().toString());
            assertThat(log.getOrderNumber()).isEqualTo("ORD-2026-001");
            assertThat(log.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(log.getDetails()).containsEntry("totalAmount", new BigDecimal("250.00"));
            assertThat(log.getDetails()).containsEntry("finalAmount", new BigDecimal("225.00"));
        }

        @Test
        @DisplayName("handleOrderPaid: Debe persistir registro ORDER_PAID con info de transacción y método")
        void handleOrderPaid_shouldPersistPaymentAuditRecord() {
            OrderPaidEvent event = OrderPaidEvent.of(sampleOrder);

            auditEventListener.handleOrderPaid(event);

            ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            OrderAuditLog log = captor.getValue();
            assertThat(log.getEventType()).isEqualTo("ORDER_PAID");
            assertThat(log.getDetails()).containsEntry("paymentMethod", "CARD");
            assertThat(log.getDetails()).containsEntry("transactionId", "TX-12345");
        }

        @Test
        @DisplayName("handleOrderCancelled: Debe persistir registro ORDER_CANCELLED con el motivo")
        void handleOrderCancelled_shouldPersistCancellationAuditRecord() {
            OrderCancelledEvent event = OrderCancelledEvent.of(sampleOrder, "Tarjeta rechazada");

            auditEventListener.handleOrderCancelled(event);

            ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
            verify(auditLogRepository).save(captor.capture());

            OrderAuditLog log = captor.getValue();
            assertThat(log.getEventType()).isEqualTo("ORDER_CANCELLED");
            assertThat(log.getDetails()).containsEntry("reason", "Tarjeta rechazada");
        }
    }

    @Nested
    @DisplayName("NotificationEventListener - Envío de Notificaciones")
    class NotificationEventListenerTests {

        private NotificationEventListener notificationEventListener;

        @BeforeEach
        void init() {
            notificationEventListener = new NotificationEventListener();
        }

        @Test
        @DisplayName("handleOrderPaid & handleOrderCancelled: No deben lanzar excepciones")
        void notificationEvents_shouldExecuteSafely() {
            OrderPaidEvent paidEvent = OrderPaidEvent.of(sampleOrder);
            OrderCancelledEvent cancelledEvent = OrderCancelledEvent.of(sampleOrder, "Cambio de opinión");

            assertDoesNotThrow(() -> notificationEventListener.handleOrderPaid(paidEvent));
            assertDoesNotThrow(() -> notificationEventListener.handleOrderCancelled(cancelledEvent));
        }
    }
}
