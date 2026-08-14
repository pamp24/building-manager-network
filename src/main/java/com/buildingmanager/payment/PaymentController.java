package com.buildingmanager.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final VivaPaymentService vivaPaymentService;

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@Validated @RequestBody PaymentRequest req) {
        PaymentDTO dto = paymentService.createPayment(req);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @Validated @RequestBody PaymentIntentRequest req
    ) {
        PaymentIntentResponse response = switch (req.getGateway().toUpperCase()) {
            case "STRIPE" -> stripePaymentService.createPaymentIntent(
                    req.getStatementId(), req.getUserId(), req.getApartmentId(),
                    req.getAmount(), req.getReturnUrl(), req.getCancelUrl());
            case "VIVA_WALLET" -> vivaPaymentService.createCheckoutOrder(
                    req.getStatementId(), req.getUserId(), req.getApartmentId(),
                    req.getAmount(), req.getReturnUrl());
            default -> throw new IllegalArgumentException("Unsupported gateway: " + req.getGateway());
        };
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statement/{statementId}")
    public ResponseEntity<List<PaymentDTO>> getPaymentsForStatement(
            @PathVariable Integer statementId,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsForStatement(statementId, size));
    }

    @GetMapping("/recent/{buildingId}")
    public ResponseEntity<List<PaymentDTO>> getRecentByBuilding(
            @PathVariable Integer buildingId,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(paymentService.getRecentPaymentsByBuilding(buildingId, size));
    }

    @GetMapping("/statement/{statementId}/users")
    public List<StatementUserPaymentDTO> getUserPayments(@PathVariable Integer statementId) {
        return paymentService.getUserPaymentsForStatement(statementId);
    }
    @GetMapping("/building/{buildingId}/summary")
    public ResponseEntity<CommonStatementSummaryDTO> getBuildingSummary(@PathVariable Integer buildingId) {
        return ResponseEntity.ok(paymentService.getBuildingSummary(buildingId));
    }

    @GetMapping("/building/{buildingId}/current-month")
    public ResponseEntity<List<StatementUserPaymentDTO>> getBuildingCurrentMonthPayments(
            @PathVariable Integer buildingId
    ) {
        return ResponseEntity.ok(paymentService.getCurrentMonthPayments(buildingId));
    }
    @GetMapping("/building/{buildingId}/month/{month}")
    public ResponseEntity<List<StatementUserPaymentDTO>> getPaymentsByMonth(
            @PathVariable Integer buildingId,
            @PathVariable String month) {
        return ResponseEntity.ok(paymentService.getPaymentsByBuildingAndMonth(buildingId, month));
    }
    @GetMapping("/building/{buildingId}/last-statement")
    public ResponseEntity<List<StatementUserPaymentDTO>> getUserPaymentsForLastStatement(
            @PathVariable Integer buildingId
    ) {
        return ResponseEntity.ok(paymentService.getUserPaymentsForLastStatement(buildingId));
    }

}
