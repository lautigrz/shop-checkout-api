package com.grz.shop_api.common.config;

import com.grz.shop_api.product.domain.Product;
import com.grz.shop_api.product.infrastructure.ProductRepository;
import com.grz.shop_api.user.domain.Role;
import com.grz.shop_api.user.domain.User;
import com.grz.shop_api.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedProducts();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            log.info("Inicializando usuarios de prueba...");

            User admin = User.builder()
                    .email("admin@shop.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("Principal")
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build();

            User customer = User.builder()
                    .email("customer@shop.com")
                    .password(passwordEncoder.encode("customer123"))
                    .firstName("Juan")
                    .lastName("Pérez")
                    .role(Role.ROLE_CUSTOMER)
                    .active(true)
                    .build();

            userRepository.save(admin);
            userRepository.save(customer);
            log.info("Usuarios creados: admin@shop.com (admin123), customer@shop.com (customer123)");
        }
    }

    private void seedProducts() {
        if (productRepository.count() == 0) {
            log.info("Inicializando catálogo de productos en MongoDB...");

            Product phone = Product.builder()
                    .sku("TECH-PHN-001")
                    .name("Smartphone Ultra 5G")
                    .description("Teléfono de alta gama con pantalla AMOLED y triple cámara")
                    .category("Electrónica")
                    .price(new BigDecimal("999.99"))
                    .stock(25)
                    .attributes(Map.of(
                            "color", "Negro Titanio",
                            "almacenamiento", "256GB",
                            "ram", "8GB",
                            "pantalla", "6.7 pulgadas"
                    ))
                    .active(true)
                    .build();

            Product shoes = Product.builder()
                    .sku("CLOTH-SHOE-002")
                    .name("Zapatillas Running Pro")
                    .description("Zapatillas deportivas con amortiguación reactiva")
                    .category("Indumentaria")
                    .price(new BigDecimal("129.50"))
                    .stock(50)
                    .attributes(Map.of(
                            "talle", "42",
                            "color", "Gris / Naranja",
                            "genero", "Unisex",
                            "peso", "280g"
                    ))
                    .active(true)
                    .build();

            Product keyboard = Product.builder()
                    .sku("TECH-KB-003")
                    .name("Teclado Mecánico RGB Custom")
                    .description("Teclado 75% inalámbrico con switches hot-swappable")
                    .category("Periféricos")
                    .price(new BigDecimal("149.00"))
                    .stock(15)
                    .attributes(Map.of(
                            "switches", "Gateron Brown",
                            "conectividad", "Bluetooth / 2.4GHz / USB-C",
                            "iluminacion", "RGB por tecla",
                            "layout", "ANSI 75%"
                    ))
                    .active(true)
                    .build();

            productRepository.save(phone);
            productRepository.save(shoes);
            productRepository.save(keyboard);
            log.info("Productos de prueba creados exitosamente en MongoDB.");
        }
    }
}
