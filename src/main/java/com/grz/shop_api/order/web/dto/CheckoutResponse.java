package com.grz.shop_api.order.web.dto;

import com.grz.shop_api.order.domain.payment.PaymentResult;

public record CheckoutResponse(
        OrderResponse order,
        PaymentResult paymentResult,
        String pricingBreakdown
) {}
