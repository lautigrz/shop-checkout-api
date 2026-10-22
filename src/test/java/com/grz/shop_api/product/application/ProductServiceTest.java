package com.grz.shop_api.product.application;

import com.grz.shop_api.common.exception.ConflictException;
import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.product.domain.Product;
import com.grz.shop_api.product.infrastructure.ProductRepository;
import com.grz.shop_api.product.web.ProductMapper;
import com.grz.shop_api.product.web.dto.ProductCreateRequest;
import com.grz.shop_api.product.web.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitarios - Servicio de Productos (Catálogo MongoDB)")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Crear Producto: Debe guardar en MongoDB y retornar respuesta si el SKU es único")
    void createProduct_shouldSaveAndReturnProduct_whenSkuIsUnique() {

        // GIVEN:
        ProductCreateRequest request = new ProductCreateRequest("SKU-100", "Monitor 4K", "Specs", new BigDecimal("400.00"), 5, "Tecnología", Map.of("panel", "IPS"));
        Product entity = Product.builder().
                sku(request.sku())
                .name(request.name())
                .price(request.price())
                .stock(request.stock()).build();
        ProductResponse response = new ProductResponse("id-123", "SKU-100", "Monitor 4K", "Specs", new BigDecimal("400.00"), 5, "Tecnología", Map.of(), true, null, null);

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(entity)).thenReturn(entity);
        when(productMapper.toResponse(entity)).thenReturn(response);

        // WHEN:
        ProductResponse result = productService.createProduct(request);

        // THEN:
        assertNotNull(result);
        assertEquals("SKU-100", result.sku());
        verify(productRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("Crear Producto: Debe lanzar ConflictException si el SKU ya existe en MongoDB")
    void createProduct_shouldThrowConflictException_whenSkuAlreadyExists() {
        // GIVEN:
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-DUPLICADO",
                "Mouse",
                "Mouse inalámbrico",
                new BigDecimal("25.00"),
                10,
                "Periféricos",
                Map.of()
        );

        when(productRepository.existsBySku("SKU-DUPLICADO")).thenReturn(true);

        // WHEN & THEN:
        assertThrows(ConflictException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Descontar Stock: Debe restar la cantidad solicitada si hay stock disponible")
    void deductStock_shouldReduceQuantity_whenStockIsAvailable() {
        // GIVEN: Un producto con stock = 10 en MongoDB
        Product product = Product.builder().id("p1").name("Mouse").stock(10).build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        // WHEN: Se descuentan 3 unidades
        productService.deductStock("p1", 3);

        // THEN: El stock resultante debe ser 7 y el producto debe haberse guardado
        assertEquals(7, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Descontar Stock: Debe lanzar IllegalStateException si no hay suficiente stock")
    void deductStock_shouldThrowException_whenStockIsInsufficient() {
        // GIVEN: Un producto con stock = 2

        Product product = Product.builder().id("p1").name("Mouse").stock(2).build();
        when(productRepository.findById("p1")).thenReturn(Optional.of(product));

        // WHEN & THEN: Intentar descontar 5 debe arrojar IllegalStateException
        assertThrows(IllegalStateException.class, () -> productService.deductStock("p1", 5));
        verify(productRepository, never()).save(any());
    }
}
