package com.grz.shop_api.order.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@DisplayName("Tests Unitarios - Patrón Decorator (Cálculo de Precios y Descuentos)")
class PriceDecoratorTest {

    @Test
    @DisplayName("BasePriceCalculator: Debe retornar el monto original sin alteraciones")
    void basePriceCalculator_shouldReturnOriginalAmount() {
        // GIVEN: Un monto base de $100.00 y el calculador base
        // TODO: Instanciar BasePriceCalculator
        BigDecimal basePrice = new BigDecimal("100.00");
        PriceCalculator base = new BasePriceCalculator();

        // WHEN: Se calcula el precio
        // TODO: Invocar calculator.calculate(basePrice);
      BigDecimal result = base.calculate(basePrice);

        // THEN: El precio debe ser exactamente igual a $100.00
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    @DisplayName("CouponDiscountDecorator: Debe aplicar el 10% de descuento correctamente")
    void couponDiscountDecorator_shouldApplyPercentageDiscount() {
        // GIVEN: Un calculador base con precio $200.00 y un decorador de cupón PROMO10 (10%)
        // TODO: PriceCalculator base = new BasePriceCalculator();
        // TODO: PriceCalculator couponDecorator = new CouponDiscountDecorator(base, "PROMO10", new BigDecimal("0.10"));
        PriceCalculator base = new BasePriceCalculator();
        base.calculate(new BigDecimal("200.00"));

        PriceCalculator couponDecorator = new CouponDiscountDecorator(base, "PROMO10", new BigDecimal("0.10"));

        // WHEN: Se calcula el precio final con el decorador
        BigDecimal result = couponDecorator.calculate(new BigDecimal("200.00"));

        // THEN: $200.00 - 10% ($20.00) = $180.00
        assertEquals(new BigDecimal("180.00"), result);
        assertTrue(couponDecorator.getDescription().contains("PROMO10"));
    }

    @Test
    @DisplayName("LoyaltyDiscountDecorator: Debe aplicar el 5% de descuento por cliente frecuente")
    void loyaltyDiscountDecorator_shouldApplyCustomerDiscount() {
        // GIVEN: Un calculador base con precio $100.00 y un decorador de fidelidad (5%)
        PriceCalculator base = new BasePriceCalculator();
        PriceCalculator loyaltyDecorator = new LoyaltyDiscountDecorator(base, new BigDecimal("0.05"));

        // WHEN: Se calcula el precio
        BigDecimal result = loyaltyDecorator.calculate(new BigDecimal("100.00"));

        // THEN: $100.00 - 5% = $95.00
        assertEquals(new BigDecimal("95.00"), result);
    }

    @Test
    @DisplayName("Decoradores encadenados: Debe aplicar cupón y fidelidad de forma acumulativa")
    void stackedDecorators_shouldApplyCouponAndLoyaltySequentially() {
        // GIVEN: Un monto de $100.00 decorado primero con PROMO10 (10%) y luego con Loyalty (5%)
        BigDecimal basePrice = new BigDecimal("100.00");
        // TODO: Envolver BasePriceCalculator con CouponDiscountDecorator
        // TODO: Envolver el decorador anterior con LoyaltyDiscountDecorator
        PriceCalculator base = new BasePriceCalculator();
        PriceCalculator couponDecorator = new CouponDiscountDecorator(base, "PROMO10", new BigDecimal("0.10"));
        PriceCalculator fullyDecorated  = new LoyaltyDiscountDecorator(couponDecorator, new BigDecimal("0.05"));

        // WHEN: Se calcula el precio
        // $100.00 - 10% = $90.00. Luego $90.00 - 5% ($4.50) = $85.50
        BigDecimal result = fullyDecorated .calculate(new BigDecimal("100.00"));

        // THEN: El precio final debe ser $85.50
        assertEquals(new BigDecimal("85.50"), result);

        String description = fullyDecorated.getDescription();
        assertTrue(description.contains("PROMO10"));
        assertTrue(description.contains("Cliente Frecuente"));
    }
}
