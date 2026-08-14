package com.buildingmanager.payment;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    @Value("${payment.gateway.stripe.secret-key}")
    private String secretKey;

    @Value("${payment.gateway.stripe.webhook-secret}")
    private String webhookSecret;

    private final PaymentRepository paymentRepository;
    private final CommonExpenseStatementRepository statementRepository;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PaymentService paymentService;

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
    }

    public PaymentIntentResponse createPaymentIntent(Integer statementId, Integer userId, Integer apartmentId, Double amount, String returnUrl, String cancelUrl) {
        CommonExpenseStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException("Statement not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found"));

        long amountCents = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("eur")
                .putMetadata("statement_id", String.valueOf(statementId))
                .putMetadata("user_id", String.valueOf(userId))
                .putMetadata("apartment_id", String.valueOf(apartmentId))
                .putMetadata("user_email", user.getEmail())
                .setReturnUrl(returnUrl)
                .build();

        try {
            PaymentIntent intent = PaymentIntent.create(params);

            return PaymentIntentResponse.builder()
                    .clientSecret(intent.getClientSecret())
                    .gatewayTransactionId(intent.getId())
                    .gateway("stripe")
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment intent creation failed", e);
            throw new IllegalStateException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Stripe webhook signature verification failed", e);
            throw new IllegalArgumentException("Invalid webhook signature", e);
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);

            if (intent == null) {
                log.warn("PaymentIntent object is null in webhook event");
                return;
            }

            String intentId = intent.getId();
            Integer statementId = Integer.valueOf(intent.getMetadata().get("statement_id"));
            Integer userId = Integer.valueOf(intent.getMetadata().get("user_id"));
            Integer apartmentId = Integer.valueOf(intent.getMetadata().get("apartment_id"));
            double amount = intent.getAmount() / 100.0;

            PaymentRequest req = new PaymentRequest();
            req.setStatementId(statementId);
            req.setUserId(userId);
            req.setApartmentId(apartmentId);
            req.setAmount(amount);
            req.setPaymentMethod("STRIPE");
            req.setGateway("STRIPE");
            req.setReferenceNumber(intentId);
            req.setGatewayPaymentMethodId(intent.getPaymentMethod());

            PaymentDTO paymentDTO = paymentService.createPayment(req);

            Payment payment = paymentRepository.findById(paymentDTO.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payment not found after creation"));
            payment.setGatewayTransactionId(intentId);
            payment.setGatewayStatus(intent.getStatus());
            payment.setGatewayRawResponse(intent.toJson());
            paymentRepository.save(payment);

            log.info("Stripe payment succeeded: intentId={}, statementId={}, amount={}", intentId, statementId, amount);

        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject()
                    .orElse(null);
            if (intent != null) {
                log.warn("Stripe payment failed: intentId={}, error={}", intent.getId(),
                        intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : "unknown");
            }
        }
    }
}
