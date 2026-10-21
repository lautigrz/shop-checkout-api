package com.grz.shop_api.order.web;

import com.grz.shop_api.common.security.UserPrincipal;
import com.grz.shop_api.order.application.CheckoutFacade;
import com.grz.shop_api.order.application.OrderService;
import com.grz.shop_api.order.web.dto.AuditLogResponse;
import com.grz.shop_api.order.web.dto.CheckoutRequest;
import com.grz.shop_api.order.web.dto.CheckoutResponse;
import com.grz.shop_api.order.web.dto.OrderResponse;
import com.grz.shop_api.order.web.dto.OrderStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.grz.shop_api.common.config.OpenApiConfig.SECURITY_SCHEME_NAME;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gestión de Pedidos, Checkout y Auditoría de Eventos")
@SecurityRequirement(name = SECURITY_SCHEME_NAME)
public class OrderController {

    private final CheckoutFacade checkoutFacade;
    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Realizar Checkout de un pedido",
            description = "Orquesta stock (MongoDB), cálculo de descuentos en capas (Decorator), " +
                    "pago (Strategy/Factory), ciclo de vida (State) y eventos (Observer)")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutFacade.checkout(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-orders")
    @Operation(summary = "Obtener historial de pedidos del usuario autenticado")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.getUserOrders(principal.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de un pedido por ID")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar estado de pedido (Solo Administradores / Logística)",
            description = "Aplica el patrón State validando transiciones legales (CREATED -> PAID -> SHIPPED -> DELIVERED / CANCELLED)")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.newStatus(), request.reason()));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Consultar historial de auditoría del pedido en MongoDB (Solo Administradores)",
            description = "Devuelve los logs no estructurados capturados por los eventos del sistema")
    public ResponseEntity<List<AuditLogResponse>> getAuditHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrderAuditHistory(id));
    }
}
