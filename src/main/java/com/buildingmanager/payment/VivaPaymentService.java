package com.buildingmanager.payment;

import com.buildingmanager.apartment.Apartment;
import com.buildingmanager.apartment.ApartmentRepository;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatement;
import com.buildingmanager.commonExpenseStatement.CommonExpenseStatementRepository;
import com.buildingmanager.user.User;
import com.buildingmanager.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VivaPaymentService {

    @Value("${payment.gateway.viva.merchant-id}")
    private String merchantId;

    @Value("${payment.gateway.viva.api-key}")
    private String apiKey;

    private static final String VIVA_API_URL = "https://demo.vivapayments.com/api";
    private static final String VIVA_CHECKOUT_URL = "https://demo.vivapayments.com/web/checkout";

    private final RestTemplate restTemplate = new RestTemplate();
    private final PaymentRepository paymentRepository;
    private final CommonExpenseStatementRepository statementRepository;
    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final PaymentService paymentService;

    public PaymentIntentResponse createCheckoutOrder(Integer statementId, Integer userId, Integer apartmentId, Double amount, String returnUrl) {
        CommonExpenseStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new EntityNotFoundException("Statement not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new EntityNotFoundException("Apartment not found"));

        long amountCents = BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100)).longValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String auth = Base64.getEncoder().encodeToString((merchantId + ":" + apiKey).getBytes());
        headers.set("Authorization", "Basic " + auth);

        Map<String, Object> requestBody = Map.of(
                "Amount", amountCents,
                "CustomerTrns", "Statement #" + statementId,
                "Customer", Map.of("Email", user.getEmail()),
                "MerchantTrns", "statement_" + statementId + "_user_" + userId,
                "PaymentNotification", true,
                "RedirectUrl", returnUrl != null ? returnUrl : ""
        );

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    VIVA_API_URL + "/orders", entity, Map.class);

            if (response.getBody() == null) {
                throw new IllegalStateException("Empty response from Viva Wallet");
            }

            String orderCode = String.valueOf(response.getBody().get("OrderCode"));

            return PaymentIntentResponse.builder()
                    .checkoutUrl(VIVA_CHECKOUT_URL + "/?ref=" + orderCode)
                    .gatewayTransactionId(orderCode)
                    .gateway("viva_wallet")
                    .build();

        } catch (Exception e) {
            log.error("Viva Wallet checkout creation failed", e);
            throw new IllegalStateException("Failed to create Viva checkout order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void handleWebhook(String orderCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String auth = Base64.getEncoder().encodeToString((merchantId + ":" + apiKey).getBytes());
            headers.set("Authorization", "Basic " + auth);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    VIVA_API_URL + "/orders/" + orderCode, org.springframework.http.HttpMethod.GET, entity, Map.class);

            if (response.getBody() == null) {
                log.warn("Viva Wallet: empty transaction details for orderCode={}", orderCode);
                return;
            }

            Map<String, Object> txn = response.getBody();
            String status = String.valueOf(txn.get("StatusId"));
            String merchantTrns = String.valueOf(txn.get("MerchantTrns"));

            if (!"1".equals(status)) {
                log.info("Viva Wallet transaction not completed yet: orderCode={}, status={}", orderCode, status);
                return;
            }

            String[] parts = merchantTrns.split("_");
            if (parts.length < 4) {
                log.warn("Viva Wallet: unexpected MerchantTrns format: {}", merchantTrns);
                return;
            }

            Integer statementId = Integer.valueOf(parts[1]);
            Integer userId = Integer.valueOf(parts[3]);

            double amount = 0.0;
            if (txn.get("Total") instanceof Number num) {
                amount = num.longValue() / 100.0;
            }

            PaymentRequest req = new PaymentRequest();
            req.setStatementId(statementId);
            req.setUserId(userId);
            req.setAmount(amount);
            req.setPaymentMethod("VIVA_WALLET");
            req.setGateway("VIVA_WALLET");
            req.setReferenceNumber(orderCode);

            PaymentDTO paymentDTO = paymentService.createPayment(req);

            Payment payment = paymentRepository.findById(paymentDTO.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payment not found after creation"));
            payment.setGatewayTransactionId(orderCode);
            payment.setGatewayStatus("SUCCEEDED");
            payment.setGatewayRawResponse(txn.toString());
            paymentRepository.save(payment);

            log.info("Viva Wallet payment succeeded: orderCode={}, statementId={}", orderCode, statementId);

        } catch (Exception e) {
            log.error("Failed to process Viva Wallet webhook for orderCode={}", orderCode, e);
        }
    }
}
