package com.grz.shop_api.order.domain.payment;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.PaymentMethod;

public interface PaymentStrategy {
    PaymentResult processPayment(Order order, PaymentDetails details);
    PaymentMethod getMethod();
}
