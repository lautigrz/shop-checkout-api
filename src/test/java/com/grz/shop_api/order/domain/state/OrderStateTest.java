package com.grz.shop_api.order.domain.state;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Unitarios - Patrón State (Ciclo de Vida de Pedido)")
class OrderStateTest {

    private Order order;
    private OrderContext context;

    @BeforeEach
    void setUp() {
        // TODO: Inicializa una entidad Order básica y el OrderContext con estado CreatedState
        order = Order.builder().status(OrderStatus.CREATED).build();
        context = new OrderContext(order, new CreatedState());
    }

    @Test
    @DisplayName("CreatedState: Debe transicionar a PAID cuando se ejecuta pay()")
    void createdState_shouldTransitionToPaid_whenPayIsCalled() {
        // GIVEN: Un pedido en estado CREATED

        // WHEN: Se invoca context.pay()
        context.pay();

        // THEN: El estado de la orden debe cambiar a PAID

        assertInstanceOf(PaidState.class, context.getCurrentState());
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    @DisplayName("CreatedState: Debe transicionar a CANCELLED cuando se ejecuta cancel()")
    void createdState_shouldTransitionToCancelled_whenCancelIsCalled() {
        // GIVEN: Un pedido en estado CREATED

        // WHEN: Se invoca context.cancel()

        context.cancel();

        // THEN: El estado de la orden debe ser CANCELLED

        assertInstanceOf(CancelledState.class, context.getCurrentState());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("CreatedState: Debe lanzar BusinessRuleException si se intenta enviar sin pagar")
    void createdState_shouldThrowException_whenShipIsCalled() {
        // GIVEN: Un pedido en estado CREATED

        // WHEN & THEN: Intentar enviar directamente debe lanzar BusinessRuleException
        BusinessRuleException exception = assertThrows(BusinessRuleException.class, () -> context.ship());

        assertTrue(exception.getMessage().contains("El pedido no puede ser enviado porque aún no ha sido pagado."));
    }

    @Test
    @DisplayName("PaidState: Debe transicionar a SHIPPED cuando se ejecuta ship()")
    void paidState_shouldTransitionToShipped_whenShipIsCalled() {
        // GIVEN: Un pedido en estado PAID
        order.setStatus(OrderStatus.PAID);

        // TODO: Cambiar el estado del contexto a PaidState

        context.pay();

        // WHEN: Se invoca context.ship()
        context.ship();

        // THEN: El estado debe ser SHIPPED
        assertInstanceOf(ShippedState.class, context.getCurrentState());
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    @DisplayName("PaidState: Debe lanzar BusinessRuleException si se intenta pagar nuevamente")
    void paidState_shouldThrowException_whenPayIsCalledAgain() {
        // GIVEN: Un pedido que ya está en estado PAID
        order.setStatus(OrderStatus.PAID);
        // TODO: Configurar context en PaidState
        context.pay();
        // WHEN & THEN: Intentar pagar otra vez debe fallar

        assertThrows(BusinessRuleException.class, () -> context.pay());
    }

    @Test
    @DisplayName("ShippedState: Debe transicionar a DELIVERED cuando se ejecuta deliver()")
    void shippedState_shouldTransitionToDelivered_whenDeliverIsCalled() {
        // GIVEN: Un pedido en estado SHIPPED
        order.setStatus(OrderStatus.PAID);
        context.pay();

        order.setStatus(OrderStatus.SHIPPED);
        context.ship();

        // WHEN: Se invoca context.deliver()
        context.deliver();

        // THEN: El estado debe ser DELIVERED
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("ShippedState: Debe lanzar BusinessRuleException si se intenta cancelar en tránsito")
    void shippedState_shouldThrowException_whenCancelIsCalled() {
        // GIVEN: Un pedido en estado SHIPPED
        order.setStatus(OrderStatus.PAID);
        context.pay();

        order.setStatus(OrderStatus.SHIPPED);
        context.ship();


        // WHEN & THEN: Intentar cancelar un envío en curso debe fallar
        assertThrows(BusinessRuleException.class, () -> context.cancel());
    }

    @Test
    @DisplayName("CancelledState: No debe permitir ninguna transición posterior")
    void cancelledState_shouldThrowException_whenAnyTransitionIsAttempted() {
        // GIVEN: Un pedido en estado CANCELLED

        order.setStatus(OrderStatus.CANCELLED);
        context.cancel();

        // WHEN & THEN: Intentar cualquier acción después de la cancelación debe lanzar BusinessRuleException
        assertThrows(BusinessRuleException.class, () -> context.pay());
        assertThrows(BusinessRuleException.class, () -> context.ship());
        assertThrows(BusinessRuleException.class, () -> context.deliver());
    }
}
