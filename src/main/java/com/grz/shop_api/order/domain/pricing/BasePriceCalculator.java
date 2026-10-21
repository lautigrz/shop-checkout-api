package com.grz.shop_api.order.domain.pricing;

import java.math.BigDecimal;

public class BasePriceCalculator implements PriceCalculator {

    @Override
    public BigDecimal calculate(BigDecimal currentPrice) {
        return currentPrice;
    }

    @Override
    public String getDescription() {
        return "Precio base";
    }
}
