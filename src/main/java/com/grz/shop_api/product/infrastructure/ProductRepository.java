package com.grz.shop_api.product.infrastructure;

import com.grz.shop_api.product.domain.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByActiveTrue();
    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
}
