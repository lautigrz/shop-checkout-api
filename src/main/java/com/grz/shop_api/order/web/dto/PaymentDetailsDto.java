package com.grz.shop_api.order.web.dto;

public record PaymentDetailsDto(
        String cardNumber,
        String cardHolderName,
        String expirationDate,
        String cvv,
        String installments,
        String transferCbu,
        String mercadoPagoToken
) {}
