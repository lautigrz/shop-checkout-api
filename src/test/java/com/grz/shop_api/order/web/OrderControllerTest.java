package com.grz.shop_api.order.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grz.shop_api.common.exception.GlobalExceptionHandler;
import com.grz.shop_api.order.application.CheckoutFacade;
import com.grz.shop_api.order.application.OrderService;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.PaymentMethod;
import com.grz.shop_api.order.domain.payment.PaymentResult;
import com.grz.shop_api.order.web.dto.CheckoutRequest;
import com.grz.shop_api.order.web.dto.CheckoutResponse;
import com.grz.shop_api.order.web.dto.OrderItemRequest;
import com.grz.shop_api.order.web.dto.OrderResponse;
import com.grz.shop_api.order.web.dto.OrderStatusUpdateRequest;
import com.grz.shop_api.order.web.dto.PaymentDetailsDto;
import com.grz.shop_api.common.security.UserPrincipal;
import com.grz.shop_api.user.domain.Role;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Capa Web - OrderController (MockMvc Standalone)")
class OrderControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CheckoutFacade checkoutFacade;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new UserPrincipal(UUID.randomUUID(), "customer@shop.com", "pass", Role.ROLE_CUSTOMER, true, List.of());
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /checkout: Debe retornar 201 Created cuando la petición es válida")
    void checkout_shouldReturn201Created_whenRequestIsValid() throws Exception {
        // GIVEN: Una petición válida de Checkout
        CheckoutRequest request = new CheckoutRequest(
                List.of(new OrderItemRequest("prod-1", 2)),
                "Av. Corrientes 1000",
                PaymentMethod.CARD,
                new PaymentDetailsDto("4500123456789999", "Juan Perez", "12/28", "123", "1", null, null),
                "PROMO10",
                false
         );
        OrderResponse orderRes = new OrderResponse(UUID.randomUUID(), "ORD-001", UUID.randomUUID(), new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("90.00"), OrderStatus.PAID, PaymentMethod.CARD, "TX-1", "Av. Corrientes", List.of(), Instant.now(), Instant.now());
        CheckoutResponse response = new CheckoutResponse(orderRes, PaymentResult.success("TX-1", "Aprobado"), "Desglose");

        when(checkoutFacade.checkout(any(), any(CheckoutRequest.class))).thenReturn(response);

        // WHEN & THEN:
        mockMvc.perform(post("/api/v1/orders/checkout")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(request)))
                 .andExpect(status().isCreated())
                 .andExpect(jsonPath("$.order.orderNumber").value("ORD-001"))
                 .andExpect(jsonPath("$.order.status").value("PAID"));
    }

    @Test
    @DisplayName("POST /checkout: Debe retornar 400 Bad Request cuando la lista de ítems está vacía")
    void checkout_shouldReturn400BadRequest_whenItemsListIsEmpty() throws Exception {
        // GIVEN: Una petición con lista de ítems vacía
        CheckoutRequest invalidRequest = new CheckoutRequest(Collections.emptyList(), "Direccion", PaymentMethod.CARD, null, null, false);

        // WHEN & THEN: Validar que el @Valid del controlador y GlobalExceptionHandler retornen 400
        mockMvc.perform(post("/api/v1/orders/checkout")
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(invalidRequest)))
                 .andExpect(status().isBadRequest())
                 .andExpect(jsonPath("$.title").value("Datos de entrada inválidos"));
    }

    @Test
    @DisplayName("GET /{id}: Debe retornar 200 OK con el pedido consultado")
    void getById_shouldReturn200_whenOrderExists() throws Exception {
        // GIVEN: Un ID existente de orden
        UUID orderId = UUID.randomUUID();
        OrderResponse orderRes = new OrderResponse(orderId, "ORD-999", UUID.randomUUID(), new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), OrderStatus.PAID, PaymentMethod.TRANSFER, null, "Direccion", List.of(), Instant.now(), Instant.now());
        when(orderService.getById(orderId)).thenReturn(orderRes);

        // WHEN & THEN:
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.orderNumber").value("ORD-999"));
    }

    @Test
    @DisplayName("GET /my-orders: Debe retornar lista de pedidos del usuario autenticado")
    void getMyOrders_shouldReturnOrdersList() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse orderRes = new OrderResponse(orderId, "ORD-999", UUID.randomUUID(), new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), OrderStatus.PAID, PaymentMethod.TRANSFER, null, "Direccion", List.of(), Instant.now(), Instant.now());
        when(orderService.getUserOrders(any(UUID.class))).thenReturn(List.of(orderRes));

        mockMvc.perform(get("/api/v1/orders/my-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-999"));
    }

    @Test
    @DisplayName("PATCH /{id}/status: Debe actualizar el estado del pedido")
    void updateStatus_shouldReturnUpdatedOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderStatusUpdateRequest updateReq = new OrderStatusUpdateRequest(OrderStatus.SHIPPED, "Despachado en correo");
        OrderResponse orderRes = new OrderResponse(orderId, "ORD-999", UUID.randomUUID(), new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), OrderStatus.SHIPPED, PaymentMethod.TRANSFER, null, "Direccion", List.of(), Instant.now(), Instant.now());

        when(orderService.updateStatus(orderId, OrderStatus.SHIPPED, "Despachado en correo")).thenReturn(orderRes);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("GET /{id}/audit: Debe retornar historial de auditoría de MongoDB")
    void getAuditHistory_shouldReturnAuditList() throws Exception {
        UUID orderId = UUID.randomUUID();
        com.grz.shop_api.order.web.dto.AuditLogResponse auditLog = new com.grz.shop_api.order.web.dto.AuditLogResponse(
                "log-1", orderId.toString(), "ORD-999", "ORDER_CREATED", "CREATED", "Creado", java.util.Map.of(), Instant.now()
        );
        when(orderService.getOrderAuditHistory(orderId)).thenReturn(List.of(auditLog));

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_CREATED"));
    }
}
