package com.grz.shop_api.order.infrastructure;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAuditLogRepository extends MongoRepository<OrderAuditLog, String> {
    List<OrderAuditLog> findByOrderIdOrderByTimestampDesc(String orderId);
    List<OrderAuditLog> findByOrderNumberOrderByTimestampDesc(String orderNumber);
}
