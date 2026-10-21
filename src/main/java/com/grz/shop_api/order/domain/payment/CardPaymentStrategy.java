package com.grz.shop_api.order.domain.payment;

import com.grz.shop_api.common.exception.PaymentFailedException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Component
public class CardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult processPayment(Order order, PaymentDetails details) {
        log.info("Procesando pago con Tarjeta de Crédito/Débito para la orden {}", order.getOrderNumber());

        if (details == null || !StringUtils.hasText(details.cardNumber()) || !StringUtils.hasText(details.cvv())) {
            throw new PaymentFailedException("Datos de tarjeta incompletos (requiere número de tarjeta y CVV).");
        }

        // Simulación: tarjetas que terminan en '0000' fallan por fondos insuficientes
        if (details.cardNumber().endsWith("0000")) {
            throw new PaymentFailedException("La tarjeta fue rechazada por la entidad bancaria (Fondos insuficientes).");
        }

        String transactionId = "TX-CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Pago con tarjeta aprobado exitosamente. Transacción: {}", transactionId);

        return PaymentResult.success(transactionId, "Pago con tarjeta aprobado por $" + order.getFinalAmount());
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CARD;
    }
}
