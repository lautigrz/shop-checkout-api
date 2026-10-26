package com.grz.shop_api.product.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grz.shop_api.common.exception.GlobalExceptionHandler;
import com.grz.shop_api.common.exception.ResourceNotFoundException;
import com.grz.shop_api.product.application.ProductService;
import com.grz.shop_api.product.web.dto.ProductCreateRequest;
import com.grz.shop_api.product.web.dto.ProductResponse;
import com.grz.shop_api.product.web.dto.ProductUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de Capa Web - ProductController (MockMvc Standalone)")
class ProductControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = new ProductResponse(
                "prod-123",
                "SKU-TEST-1",
                "Teclado Mecánico RGB",
                "Teclado gamer switches blue",
                new BigDecimal("89.99"),
                15,
                "Periféricos",
                Map.of("color", "Negro", "conectividad", "USB"),
                true,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/products: Debe retornar 200 OK con la lista de productos")
    void getAll_shouldReturn200AndProductList() throws Exception {
        when(productService.getAllActive(null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value("prod-123"))
                .andExpect(jsonPath("$[0].name").value("Teclado Mecánico RGB"))
                .andExpect(jsonPath("$[0].price").value(89.99));

        verify(productService).getAllActive(null);
    }

    @Test
    @DisplayName("GET /api/v1/products?category=Periféricos: Debe filtrar productos por categoría")
    void getAll_withCategory_shouldPassFilterToService() throws Exception {
        when(productService.getAllActive("Periféricos")).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Periféricos")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Periféricos"));

        verify(productService).getAllActive("Periféricos");
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}: Debe retornar 200 OK cuando el producto existe")
    void getById_shouldReturn200AndProduct() throws Exception {
        when(productService.getById("prod-123")).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/products/{id}", "prod-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-123"))
                .andExpect(jsonPath("$.sku").value("SKU-TEST-1"));

        verify(productService).getById("prod-123");
    }

    @Test
    @DisplayName("GET /api/v1/products/{id}: Debe retornar 404 Not Found cuando el producto no existe")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(productService.getById("prod-not-found"))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con ID: prod-not-found"));

        mockMvc.perform(get("/api/v1/products/{id}", "prod-not-found")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Producto no encontrado con ID: prod-not-found"));
    }

    @Test
    @DisplayName("POST /api/v1/products: Debe retornar 201 Created cuando la petición es válida")
    void create_shouldReturn201Created() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-TEST-1",
                "Teclado Mecánico RGB",
                "Teclado gamer switches blue",
                new BigDecimal("89.99"),
                15,
                "Periféricos",
                Map.of("color", "Negro")
        );

        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("prod-123"))
                .andExpect(jsonPath("$.name").value("Teclado Mecánico RGB"));

        verify(productService).createProduct(any(ProductCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/products: Debe retornar 400 Bad Request cuando faltan campos obligatorios")
    void create_shouldReturn400_whenValidationFails() throws Exception {
        ProductCreateRequest invalidRequest = new ProductCreateRequest(
                "", // Blank SKU
                "", // Blank name
                null,
                new BigDecimal("-1.00"), // Invalid negative price
                -5, // Invalid negative stock
                "", // Blank category
                null
        );

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id}: Debe retornar 200 OK cuando se actualiza el producto")
    void update_shouldReturn200Ok() throws Exception {
        ProductUpdateRequest updateRequest = new ProductUpdateRequest(
                "Teclado Mecánico RGB v2",
                "Descripción actualizada",
                new BigDecimal("99.99"),
                20,
                "Periféricos",
                Map.of(),
                true
        );

        when(productService.updateProduct(eq("prod-123"), any(ProductUpdateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/products/{id}", "prod-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-123"));

        verify(productService).updateProduct(eq("prod-123"), any(ProductUpdateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/products/{id}: Debe retornar 204 No Content")
    void delete_shouldReturn204NoContent() throws Exception {
        doNothing().when(productService).deleteProduct("prod-123");

        mockMvc.perform(delete("/api/v1/products/{id}", "prod-123"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct("prod-123");
    }
}
