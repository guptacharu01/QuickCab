package com.cabbooking.controller;

import com.cabbooking.dto.MiscDtos.PaymentIntentResponse;
import com.cabbooking.dto.MiscDtos.PaymentResponse;
import com.cabbooking.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Rider payments + the Stripe webhook (public, signature-verified). */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    private Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /** Tells the frontend whether Stripe is configured and hands it the publishable key. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("configured", payments.isConfigured(),
                "publishableKey", payments.isConfigured() ? payments.getPublishableKey() : "");
    }

    @PostMapping("/rides/{rideId}/intent")
    public PaymentIntentResponse intent(@PathVariable("rideId") Long rideId) {
        return payments.createIntent(uid(), rideId);
    }

    @PostMapping("/rides/{rideId}/confirm")
    public PaymentResponse confirm(@PathVariable("rideId") Long rideId) {
        return payments.confirm(uid(), rideId);
    }

    @GetMapping("/rides/{rideId}")
    public PaymentResponse one(@PathVariable("rideId") Long rideId) {
        return payments.forRide(uid(), rideId);
    }

    /** Stripe calls this. The signature is verified; never trust the body blindly. */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String sig) {
        payments.handleWebhook(payload, sig);
        return ResponseEntity.status(HttpStatus.OK).body("ok");
    }
}
