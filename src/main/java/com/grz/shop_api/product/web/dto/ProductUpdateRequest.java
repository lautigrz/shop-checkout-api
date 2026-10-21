package com.grz.shop_api.product.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.Map;

public record ProductUpdateRequest(
        String name,
        String description,
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal price,
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,
        String category,
        Map<String, Object> attributes,
        Boolean active
) {}
