package com.grz.shop_api.order.application;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.PaymentMethod;
import com.grz.shop_api.order.domain.payment.PaymentResult;
import com.grz.shop_api.order.domain.payment.PaymentStrategy;
import com.grz.shop_api.order.domain.payment.PaymentStrategyFactory;
import com.grz.shop_api.order.event.OrderCreatedEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import com.grz.shop_api.order.infrastructure.OrderRepository;
import com.grz.shop_api.order.web.OrderMapper;
import com.grz.shop_api.order.web.dto.CheckoutRequest;
import com.grz.shop_api.order.web.dto.CheckoutResponse;
import com.grz.shop_api.order.web.dto.OrderItemRequest;
import com.grz.shop_api.order.web.dto.OrderResponse;
import com.grz.shop_api.order.web.dto.PaymentDetailsDto;
import com.grz.shop_api.product.application.ProductService;
import com.grz.shop_api.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Patrón Facade (Orquestador de Checkout)")
class CheckoutFacadeTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private CheckoutFacade checkoutFacade;

    @Test
    @DisplayName("Checkout: Debe orquestar exitosamente validación, cobro, guardado y disparo de eventos")
    void checkout_shouldCompleteSuccessfully_whenStockAndPaymentAreValid() {
        // GIVEN:
        UUID userId = UUID.randomUUID();
        Product product = Product.builder().id("p1").name("Smartphone").price(new BigDecimal("100.00")).stock(10).active(true).build();
        CheckoutRequest request = new CheckoutRequest(List.of(new OrderItemRequest("p1", 2)), "Direccion 123", PaymentMethod.CARD, new PaymentDetailsDto("4500123456789999", "Juan", "12/28", "123", "1", null, null), null, false);
        PaymentStrategy mockStrategy = mock(PaymentStrategy.class);

        // TODO: Simular comportamiento de los mocks:
        when(productService.getEntityById("p1")).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentStrategyFactory.getStrategy(PaymentMethod.CARD)).thenReturn(mockStrategy);
        when(mockStrategy.processPayment(any(), any())).thenReturn(PaymentResult.success("TX-123", "Aprobado"));
        OrderResponse expectedOrder = mock(OrderResponse.class);
        when(orderMapper.toResponse(any())).thenReturn(expectedOrder);

        // WHEN:
        CheckoutResponse response = checkoutFacade.checkout(userId, request);
        // THEN:
        // TODO: Verificar que la respuesta contiene el resultado del pago y la orden
        assertEquals("TX-123", response.paymentResult().transactionId());
        assertEquals("Aprobado", response.paymentResult().message());
        assertEquals(expectedOrder, response.order());

        // TODO: Verificar que se publicaron los eventos OrderCreatedEvent y OrderPaidEvent
        verify(eventPublisher, times(1)).publishEvent(any(OrderCreatedEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("Checkout: Debe lanzar BusinessRuleException si el producto no tiene stock suficiente")
    void checkout_shouldThrowBusinessRuleException_whenStockIsInsufficient() {
        // GIVEN: Un producto con stock 1 y una petición solicitando cantidad 5
        UUID userId = UUID.randomUUID();
        Product product = Product.builder().id("p1").name("Zapatillas").price(new BigDecimal("50.00")).stock(1).active(true).build();
        CheckoutRequest request = new CheckoutRequest(List.of(new OrderItemRequest("p1", 5)), "Direccion 123", PaymentMethod.CARD, new PaymentDetailsDto("4500123456789999", "Juan", "12/28", "123", "1", null, null), null, false);
        when(productService.getEntityById("p1")).thenReturn(product);

        // WHEN & THEN:
        assertThrows(BusinessRuleException.class, () -> checkoutFacade.checkout(userId, request));
        // TODO: Verificar que jamás se intentó procesar el pago ni guardar la orden
        verifyNoInteractions(paymentStrategyFactory);
        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Checkout: Debe lanzar BusinessRuleException si el producto está inactivo")
    void checkout_shouldThrowBusinessRuleException_whenProductIsInactive() {
        // GIVEN: Un producto con active = false
        UUID userId = UUID.randomUUID();
        Product product = Product.builder().id("p1").name("Zapatillas").price(new BigDecimal("50.00")).stock(1).active(false).build();
        CheckoutRequest request = new CheckoutRequest(List.of(new OrderItemRequest("p1", 5)), "Direccion 123", PaymentMethod.CARD, new PaymentDetailsDto("4500123456789999", "Juan", "12/28", "123", "1", null, null), null, false);

        when(productService.getEntityById("p1")).thenReturn(product);

        // WHEN & THEN:
        assertThrows(BusinessRuleException.class, () -> checkoutFacade.checkout(userId, request));
    }

    @Test
    @DisplayName("Checkout: Debe aplicar el decorador de cupón sobre el total calculado")
    void checkout_shouldApplyCouponDiscount_whenCouponCodeIsProvided() {
        // GIVEN: Una compra con base $100.00 y cupón 'PROMO10' (10% de descuento)
        UUID userId = UUID.randomUUID();
        Product product = Product.builder().id("p1").
                name("Smartphone").
                price(new BigDecimal("100.00")).stock(10).active(true).build();
        CheckoutRequest requestConCupon = new CheckoutRequest(List.of(new OrderItemRequest("p1", 1)), "Direccion 123", PaymentMethod.CARD, new PaymentDetailsDto("4500123456789999", "Juan", "12/28", "123", "1", null, null), "PROMO10", false);
        PaymentStrategy mockStrategy = mock(PaymentStrategy.class);

        // TODO: Configurar mocks para permitir que el checkout avance
        when(productService.getEntityById("p1")).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentStrategyFactory.getStrategy(PaymentMethod.CARD)).thenReturn(mockStrategy);
        when(mockStrategy.processPayment(any(), any())).thenReturn(PaymentResult.success("TX-456", "Aprobado"));

        // WHEN:
        CheckoutResponse response = checkoutFacade.checkout(userId, requestConCupon);

        // THEN:
        // TODO: Capturar la orden guardada con ArgumentCaptor<Order>
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).save(orderCaptor.capture());

        // TODO: Verificar que finalAmount sea $90.00 y discountAmount sea $10.00
        Order savedOrder = orderCaptor.getValue();
        assertEquals(new BigDecimal("90.00"), savedOrder.getFinalAmount());
        assertEquals(new BigDecimal("10.00"), savedOrder.getDiscountAmount());
        assertTrue(response.pricingBreakdown().contains("PROMO10"));
    }
}
