package com.grz.shop_api.product.web;

import com.grz.shop_api.product.application.ProductService;
import com.grz.shop_api.product.web.dto.ProductCreateRequest;
import com.grz.shop_api.product.web.dto.ProductResponse;
import com.grz.shop_api.product.web.dto.ProductUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.grz.shop_api.common.config.OpenApiConfig.SECURITY_SCHEME_NAME;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Catálogo de productos almacenados en MongoDB con atributos flexibles")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Listar productos activos (Público)", description = "Permite filtrar por categoría")
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.getAllActive(category));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID (Público)")
    public ResponseEntity<ProductResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    @Operation(summary = "Crear nuevo producto (Solo Administradores)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    @Operation(summary = "Actualizar producto existente (Solo Administradores)")
    public ResponseEntity<ProductResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = SECURITY_SCHEME_NAME)
    @Operation(summary = "Desactivar/eliminar producto (Solo Administradores)")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
