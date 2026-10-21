package com.grz.shop_api.order.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoyaltyDiscountDecorator extends PriceDecorator {

    private final BigDecimal loyaltyDiscountRate;

    public LoyaltyDiscountDecorator(PriceCalculator wrappee, BigDecimal loyaltyDiscountRate) {
        super(wrappee);
        this.loyaltyDiscountRate = loyaltyDiscountRate;
    }

    @Override
    public BigDecimal calculate(BigDecimal currentPrice) {
        BigDecimal previousPrice = super.calculate(currentPrice);
        BigDecimal discount = previousPrice.multiply(loyaltyDiscountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = previousPrice.subtract(discount);
        return finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPrice;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + Descuento Cliente Frecuente (-" + loyaltyDiscountRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "% )";
    }
}
