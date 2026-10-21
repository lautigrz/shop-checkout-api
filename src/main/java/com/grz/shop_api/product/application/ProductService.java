package com.grz.shop_api.product.application;

import com.grz.shop_api.common.exception.ConflictException;
import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.product.domain.Product;
import com.grz.shop_api.product.infrastructure.ProductRepository;
import com.grz.shop_api.product.web.ProductMapper;
import com.grz.shop_api.product.web.dto.ProductCreateRequest;
import com.grz.shop_api.product.web.dto.ProductResponse;
import com.grz.shop_api.product.web.dto.ProductUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("Ya existe un producto con el SKU: " + request.sku());
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        Product product = getEntityById(id);
        productMapper.updateEntityFromDto(request, product);
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    public void deleteProduct(String id) {
        Product product = getEntityById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public ProductResponse getById(String id) {
        return productMapper.toResponse(getEntityById(id));
    }

    public Product getEntityById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    public List<ProductResponse> getAllActive(String category) {
        List<Product> products;
        if (StringUtils.hasText(category)) {
            products = productRepository.findByCategoryIgnoreCaseAndActiveTrue(category);
        } else {
            products = productRepository.findByActiveTrue();
        }
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public void deductStock(String productId, int quantity) {
        Product product = getEntityById(productId);
        product.deductStock(quantity);
        productRepository.save(product);
    }

    public void restoreStock(String productId, int quantity) {
        Product product = getEntityById(productId);
        product.restoreStock(quantity);
        productRepository.save(product);
    }
}
