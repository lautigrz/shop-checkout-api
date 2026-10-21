package com.grz.shop_api.order.event.listener;

import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderItem;
import com.grz.shop_api.order.event.OrderCancelledEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import com.grz.shop_api.product.application.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockEventListener {

    private final ProductService productService;

    @EventListener
    public void handleOrderPaid(OrderPaidEvent event) {
        Order order = event.order();
        log.info("[Observer/Stock] Descontando inventario para la orden pagada {}", order.getOrderNumber());

        for (OrderItem item : order.getItems()) {
            try {
                productService.deductStock(item.getProductId(), item.getQuantity());
                log.info("[Observer/Stock] Descontadas {} unidades del producto {}", item.getQuantity(), item.getProductName());
            } catch (Exception e) {
                log.error("[Observer/Stock] Error al descontar stock para el producto {}: {}", item.getProductId(), e.getMessage());
            }
        }
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = event.order();
        log.info("[Observer/Stock] Reintegrando inventario para la orden cancelada {}", order.getOrderNumber());

        for (OrderItem item : order.getItems()) {
            try {
                productService.restoreStock(item.getProductId(), item.getQuantity());
                log.info("[Observer/Stock] Reintegradas {} unidades del producto {}", item.getQuantity(), item.getProductName());
            } catch (Exception e) {
                log.error("[Observer/Stock] Error al reintegrar stock para el producto {}: {}", item.getProductId(), e.getMessage());
            }
        }
    }
}
