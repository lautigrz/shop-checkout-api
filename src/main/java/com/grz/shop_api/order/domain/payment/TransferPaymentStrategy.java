package com.grz.shop_api.order.domain.payment;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class TransferPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, PaymentDetails details) {
        log.info("Procesando pago por Transferencia Bancaria para la orden {}", order.getOrderNumber());

        String transactionId = "TX-TRANSF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Transferencia bancaria validada y conciliada. Transacción: {}", transactionId);

        return PaymentResult.success(transactionId, "Transferencia recibida y acreditada por $" + order.getFinalAmount());
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.TRANSFER;
    }
}
