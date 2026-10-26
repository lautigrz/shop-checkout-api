package com.grz.shop_api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grz.shop_api.TestcontainersConfiguration;
import com.grz.shop_api.common.security.JwtService;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.PaymentMethod;
import com.grz.shop_api.order.infrastructure.OrderAuditLog;
import com.grz.shop_api.order.infrastructure.OrderAuditLogRepository;
import com.grz.shop_api.order.infrastructure.OrderRepository;
import com.grz.shop_api.order.web.dto.CheckoutRequest;
import com.grz.shop_api.order.web.dto.OrderItemRequest;
import com.grz.shop_api.order.web.dto.PaymentDetailsDto;
import com.grz.shop_api.product.domain.Product;
import com.grz.shop_api.product.infrastructure.ProductRepository;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.domain.User;
import com.grz.shop_api.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.DockerClientFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@DisabledIf(value = "isDockerUnavailable", disabledReason = "Docker no está accesible en este host para Testcontainers")
@DisplayName("Test de Integración E2E con Testcontainers (PostgreSQL 16 + MongoDB 7)")
class ShopApiTestcontainersIntegrationTest {

    static boolean isDockerUnavailable() {
        try {
            return !DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return true;
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderAuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // Limpiar colecciones y tablas para garantizar aislamiento
        auditLogRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Crear usuario en PostgreSQL real
        testUser = User.builder()
                .email("test.integration." + System.currentTimeMillis() + "@shop.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Carlos")
                .lastName("Integracion")
                .role(Role.ROLE_CUSTOMER)
                .active(true)
                .build();
        testUser = userRepository.save(testUser);

        // 2. Generar JWT para el usuario
        jwtToken = jwtService.generateToken(testUser.getEmail(), testUser.getRole().name(), testUser.getId().toString());

        // 3. Crear producto en MongoDB real con atributos flexibles
        testProduct = Product.builder()
                .sku("LAPTOP-PRO-15-" + System.currentTimeMillis())
                .name("Notebook Gamer Pro 15")
                .description("Procesador Octa-Core, 16GB RAM, 512GB SSD")
                .price(new BigDecimal("1000.00"))
                .stock(10)
                .category("Electrónica")
                .attributes(Map.of("pantalla", "15.6 pulgadas", "color", "Gris Espacial"))
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("Flujo E2E Completo: Realizar Checkout con PostgreSQL y MongoDB reales en contenedores")
    void completeCheckoutFlow_shouldPersistOrderInPostgresAndAuditInMongo() throws Exception {
        // GIVEN: Solicitud de Checkout con 2 unidades y cupón de 10%
        CheckoutRequest checkoutRequest = new CheckoutRequest(
                List.of(new OrderItemRequest(testProduct.getId(), 2)),
                "Av. Corrientes 1234, Piso 5, CABA",
                PaymentMethod.CARD,
                new PaymentDetailsDto("4500123456789999", "Carlos Integracion", "11/29", "123", "1", null, null),
                "PROMO10",
                false
        );

        // WHEN: Se envía la petición HTTP POST /api/v1/orders/checkout con el token JWT
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order.orderNumber").exists())
                .andExpect(jsonPath("$.order.status").value("PAID"))
                .andExpect(jsonPath("$.order.totalAmount").value(2000.00))
                .andExpect(jsonPath("$.order.discountAmount").value(200.00))
                .andExpect(jsonPath("$.order.finalAmount").value(1800.00))
                .andExpect(jsonPath("$.paymentResult.success").value(true));

        // THEN: 1. Verificar persistencia transaccional en PostgreSQL
        List<Order> ordersInPostgres = orderRepository.findByUserIdWithItems(testUser.getId());
        assertThat(ordersInPostgres).hasSize(1);
        Order savedOrder = ordersInPostgres.get(0);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(savedOrder.getFinalAmount()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(savedOrder.getItems()).hasSize(1);
        assertThat(savedOrder.getItems().get(0).getQuantity()).isEqualTo(2);

        // THEN: 2. Verificar sincronización de stock en MongoDB (descontado de 10 a 8)
        Optional<Product> updatedProductOpt = productRepository.findById(testProduct.getId());
        assertThat(updatedProductOpt).isPresent();
        assertThat(updatedProductOpt.get().getStock()).isEqualTo(8);

        // THEN: 3. Verificar auditoría no estructurada generada por el Observer en MongoDB
        List<OrderAuditLog> auditLogsInMongo = auditLogRepository.findByOrderIdOrderByTimestampDesc(savedOrder.getId().toString());
        assertThat(auditLogsInMongo).isNotEmpty();
        assertThat(auditLogsInMongo).anyMatch(log -> "ORDER_CREATED".equals(log.getEventType()));
        assertThat(auditLogsInMongo).anyMatch(log -> "ORDER_PAID".equals(log.getEventType()));
    }

    @Test
    @DisplayName("Catálogo en MongoDB Real: Búsqueda y filtrado por categoría")
    void productCatalog_shouldQueryRealMongoDb() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("category", "Electrónica")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].sku").value(testProduct.getSku()))
                .andExpect(jsonPath("$[0].attributes.pantalla").value("15.6 pulgadas"));
    }
}
