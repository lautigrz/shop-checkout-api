package com.grz.shop_api.product.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProductResponse(
        String id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        String category,
        Map<String, Object> attributes,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
