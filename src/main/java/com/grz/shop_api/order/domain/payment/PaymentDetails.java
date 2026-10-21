package com.grz.shop_api.order.domain.payment;

public record PaymentDetails(
        String cardNumber,
        String cardHolderName,
        String expirationDate,
        String cvv,
        String installments,
        String transferCbu,
        String mercadoPagoToken
) {}
