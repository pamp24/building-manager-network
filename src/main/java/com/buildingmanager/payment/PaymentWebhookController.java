package com.buildingmanager.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final StripePaymentService stripePaymentService;
    private final VivaPaymentService vivaPaymentService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        log.debug("Received Stripe webhook");
        stripePaymentService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/viva")
    public ResponseEntity<String> handleVivaWebhook(@RequestParam("orderCode") String orderCode) {
        log.debug("Received Viva Wallet webhook for orderCode={}", orderCode);
        vivaPaymentService.handleWebhook(orderCode);
        return ResponseEntity.ok("OK");
    }
}
