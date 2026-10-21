package com.grz.shop_api.order.web.dto;

import com.grz.shop_api.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "El nuevo estado es obligatorio")
        OrderStatus newStatus,

        String reason
) {}
