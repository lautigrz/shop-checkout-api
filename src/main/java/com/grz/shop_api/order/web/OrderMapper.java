package com.grz.shop_api.order.web;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderItem;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.web.dto.AuditLogResponse;
import com.grz.shop_api.order.web.dto.OrderItemResponse;
import com.grz.shop_api.order.web.dto.OrderResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    AuditLogResponse toAuditResponse(OrderAuditLog log);

    List<AuditLogResponse> toAuditResponseList(List<OrderAuditLog> logs);
}
