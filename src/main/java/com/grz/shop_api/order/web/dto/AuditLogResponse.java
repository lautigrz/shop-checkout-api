package com.grz.shop_api.order.web.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        String id,
        String orderId,
        String orderNumber,
        String eventType,
        String status,
        String message,
        Map<String, Object> details,
        Instant timestamp
) {}
