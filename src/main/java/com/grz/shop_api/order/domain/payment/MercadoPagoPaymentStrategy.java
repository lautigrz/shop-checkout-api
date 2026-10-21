package com.grz.shop_api.order.domain.payment;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MercadoPagoPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, PaymentDetails details) {
        log.info("Procesando pago mediante pasarela MercadoPago para la orden {}", order.getOrderNumber());

        String transactionId = "MP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        log.info("Cobro de MercadoPago procesado con éxito. ID: {}", transactionId);

        return PaymentResult.success(transactionId, "Cobro aprobado vía Mercado Pago por $" + order.getFinalAmount());
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.MERCADOPAGO;
    }
}
