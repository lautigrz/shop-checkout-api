package com.grz.shop_api.order.domain.payment;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.common.exception.PaymentFailedException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests Unitarios - Patrones Strategy y Factory (Pasarelas de Pago)")
class PaymentStrategyFactoryTest {

    private PaymentStrategyFactory factory;
    private CardPaymentStrategy cardStrategy;
    private TransferPaymentStrategy transferStrategy;
    private MercadoPagoPaymentStrategy mercadoPagoStrategy;

    @BeforeEach
    void setUp() {
        // TODO: Instanciar las estrategias concretas
        cardStrategy = new CardPaymentStrategy();
        transferStrategy = new TransferPaymentStrategy();
        mercadoPagoStrategy = new MercadoPagoPaymentStrategy();

        // TODO: Inyectar la lista de estrategias a la factoría
        factory = new PaymentStrategyFactory(List.of(cardStrategy, transferStrategy, mercadoPagoStrategy));
    }

    @Test
    @DisplayName("Factory: Debe resolver CardPaymentStrategy cuando el método es CARD")
    void factory_shouldReturnCardStrategy_whenMethodIsCard() {
        // GIVEN: El enum PaymentMethod.CARD

        // WHEN: Se consulta a la factoría
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.CARD);

        // THEN: La estrategia debe ser instancia de CardPaymentStrategy
        assertNotNull(strategy);
        assertInstanceOf(CardPaymentStrategy.class, strategy);
    }

    @Test
    @DisplayName("Factory: Debe resolver TransferPaymentStrategy cuando el método es TRANSFER")
    void factory_shouldReturnTransferStrategy_whenMethodIsTransfer() {
        // GIVEN: PaymentMethod.TRANSFER

        // WHEN: Se consulta a la factoría
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.TRANSFER);

        // THEN:
        // TODO: Verificar que factory.getStrategy(PaymentMethod.TRANSFER) retorna TransferPaymentStrategy

        assertNotNull(strategy);
        assertInstanceOf(TransferPaymentStrategy.class, strategy);
    }

    @Test
    @DisplayName("Factory: Debe resolver MercadoPagoPaymentStrategy cuando el método es MERCADOPAGO")
    void factory_shouldReturnMercadoPagoStrategy_whenMethodIsMercadoPago() {
        // GIVEN: PaymentMethod.MERCADOPAGO

        // WHEN: Se consulta a la factoría
        PaymentStrategy strategy = factory.getStrategy(PaymentMethod.MERCADOPAGO);

        // THEN:
        // TODO: Verificar que factory.getStrategy(PaymentMethod.MERCADOPAGO) retorna MercadoPagoPaymentStrategy

        assertNotNull(strategy);
        assertInstanceOf(MercadoPagoPaymentStrategy.class, strategy);
    }

    @Test
    @DisplayName("CardStrategy: Debe procesar el pago exitosamente con datos válidos")
    void cardPaymentStrategy_shouldSucceed_whenCardDetailsAreValid() {
        // GIVEN: Una orden con monto $150.00 y datos de tarjeta válidos
        Order order = Order.builder().orderNumber("ORD-001").finalAmount(new BigDecimal("150.00")).build();
        PaymentDetails details = new PaymentDetails("4500123456789999", "Juan Perez", "12/28", "123", "1", null, null);

        // WHEN: Se procesa el pago
        PaymentResult result = cardStrategy.processPayment(order, details);

        // THEN: El resultado debe ser exitoso y contener un ID de transacción generado
        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("TX-CARD-"));
    }

    @Test
    @DisplayName("CardStrategy: Debe lanzar PaymentFailedException si la tarjeta termina en 0000 (Simulación rechazo)")
    void cardPaymentStrategy_shouldFail_whenCardEndsWith0000() {
        // GIVEN: Una orden y una tarjeta que termina en '0000'
        Order order = Order.builder().orderNumber("ORD-001").finalAmount(new BigDecimal("150.00")).build();
        PaymentDetails details = new PaymentDetails("4500123456780000", "Juan Perez", "12/28", "123", "1", null, null);

        // WHEN & THEN: Debe arrojar PaymentFailedException
        assertThrows(PaymentFailedException.class, () -> cardStrategy.processPayment(order, details));
    }
}
