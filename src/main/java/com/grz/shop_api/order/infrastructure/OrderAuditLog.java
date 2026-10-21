package com.grz.shop_api.order.infrastructure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "order_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAuditLog {

    @Id
    private String id;

    @Indexed
    private String orderId;

    @Indexed
    private String orderNumber;

    private String eventType;

    private String status;

    private String message;

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    private Instant timestamp;
}
