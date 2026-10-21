package com.grz.shop_api.order.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CouponDiscountDecorator extends PriceDecorator {

    private final String couponCode;
    private final BigDecimal discountRate;

    public CouponDiscountDecorator(PriceCalculator wrappee, String couponCode, BigDecimal discountRate) {
        super(wrappee);
        this.couponCode = couponCode;
        this.discountRate = discountRate;
    }

    @Override
    public BigDecimal calculate(BigDecimal currentPrice) {
        BigDecimal previousPrice = super.calculate(currentPrice);
        BigDecimal discount = previousPrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalPrice = previousPrice.subtract(discount);
        return finalPrice.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalPrice;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " + Cupón [" + couponCode + ": -" + discountRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%]";
    }
}
