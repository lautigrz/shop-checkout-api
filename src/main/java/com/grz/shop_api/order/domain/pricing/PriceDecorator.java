package com.grz.shop_api.order.domain.pricing;

import java.math.BigDecimal;

public abstract class PriceDecorator implements PriceCalculator {

    protected final PriceCalculator wrappee;

    protected PriceDecorator(PriceCalculator wrappee) {
        this.wrappee = wrappee;
    }

    @Override
    public BigDecimal calculate(BigDecimal currentPrice) {
        return wrappee.calculate(currentPrice);
    }

    @Override
    public String getDescription() {
        return wrappee.getDescription();
    }
}
