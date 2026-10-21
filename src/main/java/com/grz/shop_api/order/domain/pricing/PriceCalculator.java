package com.grz.shop_api.order.domain.pricing;

import java.math.BigDecimal;

public interface PriceCalculator {
    BigDecimal calculate(BigDecimal currentPrice);
    String getDescription();
}
