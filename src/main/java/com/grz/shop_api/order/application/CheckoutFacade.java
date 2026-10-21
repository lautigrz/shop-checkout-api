package com.grz.shop_api.order.application;

import com.grz.shop_api.common.exception.BusinessRuleException;
import com.grz.shop_api.order.domain.Order;
import com.grz.shop_api.order.domain.OrderItem;
import com.grz.shop_api.order.domain.OrderStatus;
import com.grz.shop_api.order.domain.payment.PaymentDetails;
import com.grz.shop_api.order.domain.payment.PaymentResult;
import com.grz.shop_api.order.domain.payment.PaymentStrategy;
import com.grz.shop_api.order.domain.payment.PaymentStrategyFactory;
import com.grz.shop_api.order.domain.pricing.BasePriceCalculator;
import com.grz.shop_api.order.domain.pricing.CouponDiscountDecorator;
import com.grz.shop_api.order.domain.pricing.LoyaltyDiscountDecorator;
import com.grz.shop_api.order.domain.pricing.PriceCalculator;
import com.grz.shop_api.order.domain.state.CreatedState;
import com.grz.shop_api.order.domain.state.OrderContext;
import com.grz.shop_api.order.event.OrderCreatedEvent;
import com.grz.shop_api.order.event.OrderPaidEvent;
import com.grz.shop_api.order.infrastructure.OrderRepository;
import com.grz.shop_api.order.web.OrderMapper;
import com.grz.shop_api.order.web.dto.CheckoutRequest;
import com.grz.shop_api.order.web.dto.CheckoutResponse;
import com.grz.shop_api.order.web.dto.OrderItemRequest;
import com.grz.shop_api.order.web.dto.PaymentDetailsDto;
import com.grz.shop_api.product.application.ProductService;
import com.grz.shop_api.product.domain.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutFacade {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderMapper orderMapper;

    @Transactional
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request) {
        log.info("Iniciando flujo de Checkout (Facade) para el usuario {}", userId);

        String orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(userId)
                .shippingAddress(request.shippingAddress())
                .paymentMethod(request.paymentMethod())
                .status(OrderStatus.CREATED)
                .build();


        BigDecimal baseTotal = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.items()) {
            Product product = productService.getEntityById(itemReq.productId());

            if (!product.isActive()) {
                throw new BusinessRuleException("El producto '" + product.getName() + "' no se encuentra disponible.");
            }
            if (!product.hasStock(itemReq.quantity())) {
                throw new BusinessRuleException("Stock insuficiente para '" + product.getName() + "'. Solicitado: "
                        + itemReq.quantity() + ", Disponible: " + product.getStock());
            }

            OrderItem orderItem = OrderItem.create(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    itemReq.quantity()
            );
            order.addItem(orderItem);
            baseTotal = baseTotal.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(baseTotal);


        PriceCalculator priceCalculator = new BasePriceCalculator();

        if (StringUtils.hasText(request.couponCode())) {
            BigDecimal couponRate = resolveCouponRate(request.couponCode());
            priceCalculator = new CouponDiscountDecorator(priceCalculator, request.couponCode(), couponRate);
        }

        if (request.loyaltyMember()) {
            priceCalculator = new LoyaltyDiscountDecorator(priceCalculator, new BigDecimal("0.05"));
        }

        BigDecimal finalPrice = priceCalculator.calculate(baseTotal);
        BigDecimal discountAmount = baseTotal.subtract(finalPrice);

        order.setFinalAmount(finalPrice);
        order.setDiscountAmount(discountAmount);

        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(OrderCreatedEvent.of(savedOrder));

        PaymentStrategy paymentStrategy = paymentStrategyFactory.getStrategy(request.paymentMethod());
        PaymentDetails paymentDetails = toPaymentDetails(request.paymentDetails());
        PaymentResult paymentResult = paymentStrategy.processPayment(savedOrder, paymentDetails);

        OrderContext orderContext = new OrderContext(savedOrder, new CreatedState());
        orderContext.pay();

        savedOrder.setPaymentTransactionId(paymentResult.transactionId());
        Order completedOrder = orderRepository.save(savedOrder);


        eventPublisher.publishEvent(OrderPaidEvent.of(completedOrder));

        log.info("Checkout completado exitosamente para la orden {}", completedOrder.getOrderNumber());

        return new CheckoutResponse(
                orderMapper.toResponse(completedOrder),
                paymentResult,
                priceCalculator.getDescription()
        );
    }

    private BigDecimal resolveCouponRate(String code) {
        String normalized = code.trim().toUpperCase();
        return switch (normalized) {
            case "DESCUENTO10", "PROMO10" -> new BigDecimal("0.10");
            case "DESCUENTO20", "PROMO20" -> new BigDecimal("0.20");
            default -> throw new BusinessRuleException("Cupón de descuento no válido: " + code);
        };
    }

    private PaymentDetails toPaymentDetails(PaymentDetailsDto dto) {
        if (dto == null) {
            return new PaymentDetails(null, null, null, null, null, null, null);
        }
        return new PaymentDetails(
                dto.cardNumber(),
                dto.cardHolderName(),
                dto.expirationDate(),
                dto.cvv(),
                dto.installments(),
                dto.transferCbu(),
                dto.mercadoPagoToken()
        );
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int randomSuffix = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "ORD-" + date + "-" + randomSuffix;
    }
}
