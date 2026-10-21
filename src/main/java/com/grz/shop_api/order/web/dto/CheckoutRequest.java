package com.grz.shop_api.order.web.dto;

import com.grz.shop_api.order.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty(message = "Debe incluir al menos un producto en el pedido")
        @Valid
        List<OrderItemRequest> items,

        @NotBlank(message = "La dirección de envío es obligatoria")
        String shippingAddress,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        PaymentDetailsDto paymentDetails,

        String couponCode,

        boolean loyaltyMember
) {}
