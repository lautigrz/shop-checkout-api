package com.grz.shop_api.order.infrastructure;

import com.grz.shop_api.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithItems(@Param("userId") UUID userId);

    Optional<Order> findByOrderNumber(String orderNumber);
}
