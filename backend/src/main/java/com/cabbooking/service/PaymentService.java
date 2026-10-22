package com.cabbooking.service;

import com.cabbooking.dto.MiscDtos.*;
import com.cabbooking.entity.Driver;
import com.cabbooking.entity.Payment;
import com.cabbooking.entity.Ride;
import com.cabbooking.repository.DriverRepository;
import com.cabbooking.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Stripe payments in test mode. The payable amount is ALWAYS computed by the
 * backend from the ride fare - the frontend never decides what is charged.
 * If no Stripe keys are configured the app still runs; payments stay PENDING
 * and the UI says so honestly instead of faking a successful payment.
 */
@Service
public class PaymentService {

    private static final double DRIVER_SHARE = 0.80;

    private final PaymentRepository payments;
    private final DriverRepository drivers;
    private final String secretKey;
    private final String webhookSecret;
    private final String publishableKey;

    public PaymentService(PaymentRepository payments, DriverRepository drivers,
                          @Value("${stripe.secret-key:}") String secretKey,
                          @Value("${stripe.webhook-secret:}") String webhookSecret,
                          @Value("${stripe.publishable-key:}") String publishableKey) {
        this.payments = payments;
        this.drivers = drivers;
        this.secretKey = secretKey;
        this.webhookSecret = webhookSecret;
        this.publishableKey = publishableKey;
    }

    @PostConstruct
    void init() {
        if (isConfigured()) Stripe.apiKey = secretKey;
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    /** Called when a ride completes: one PENDING payment row per ride. */
    @Transactional
    public Payment createPending(Ride ride) {
        return payments.findByRideId(ride.getId()).orElseGet(() -> {
            Payment p = new Payment();
            p.setRide(ride);
            p.setAmount(ride.getFinalFare() != null ? ride.getFinalFare() : ride.getEstimatedFare());
            p.setStatus(Payment.Status.PENDING);
            return payments.save(p);
        });
    }

    /**
     * Rider asks to pay: creates the Stripe PaymentIntent (amount from backend)
     * and returns its client secret for Stripe.js. Idempotent per ride.
     */
    @Transactional
    public PaymentIntentResponse createIntent(Long riderId, Long rideId) {
        Payment p = ownedPayment(riderId, rideId);
        if (p.getStatus() == Payment.Status.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ride already paid");
        }
        if (!isConfigured()) {
            return new PaymentIntentResponse(null, "NOT_CONFIGURED", p.getAmount(), false);
        }
        try {
            if (p.getStripePaymentIntentId() != null) {
                // Reuse the existing intent instead of charging twice.
                PaymentIntent existing = PaymentIntent.retrieve(p.getStripePaymentIntentId());
                return new PaymentIntentResponse(existing.getClientSecret(), "REQUIRES_PAYMENT", p.getAmount(), true);
            }
            long paise = Math.round(p.getAmount() * 100);
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(paise)
                    .setCurrency("inr")
                    .putMetadata("rideId", String.valueOf(rideId))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            p.setStripePaymentIntentId(intent.getId());
            payments.save(p);
            return new PaymentIntentResponse(intent.getClientSecret(), "REQUIRES_PAYMENT", p.getAmount(), true);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
        }
    }

    /**
     * After Stripe.js confirms the card, the frontend calls this. The backend
     * re-reads the PaymentIntent from Stripe and only marks SUCCEEDED when
     * Stripe itself says so - the client is never trusted.
     */
    @Transactional
    public PaymentResponse confirm(Long riderId, Long rideId) {
        Payment p = ownedPayment(riderId, rideId);
        if (p.getStatus() == Payment.Status.SUCCEEDED) return toResponse(p);
        if (!isConfigured() || p.getStripePaymentIntentId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment not configured or not initiated");
        }
        try {
            PaymentIntent intent = PaymentIntent.retrieve(p.getStripePaymentIntentId());
            if ("succeeded".equals(intent.getStatus())) {
                markSucceeded(p);
            } else {
                p.setStatus(Payment.Status.FAILED);
            }
            return toResponse(p);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe error: " + e.getMessage());
        }
    }

    /** Stripe webhook: verifies the signature, then marks the payment paid (idempotent). */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        if (!isConfigured() || webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Webhooks not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook signature");
        }
        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);
            if (intent != null) {
                payments.findByStripePaymentIntentId(intent.getId()).ifPresent(p -> {
                    if (p.getStatus() != Payment.Status.SUCCEEDED) markSucceeded(p);
                });
            }
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse forRide(Long riderId, Long rideId) {
        return toResponse(ownedPayment(riderId, rideId));
    }

    // ------------------------------ helpers ------------------------------

    private Payment ownedPayment(Long riderId, Long rideId) {
        Payment p = payments.findByRideId(rideId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No payment for this ride"));
        if (!p.getRide().getRider().getId().equals(riderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your ride");
        }
        return p;
    }

    /** Marks paid and credits the driver's 80% share. Safe to call twice. */
    private void markSucceeded(Payment p) {
        p.setStatus(Payment.Status.SUCCEEDED);
        p.setPaidAt(Instant.now());
        Ride ride = p.getRide();
        if (ride.getDriver() != null) {
            drivers.findByUserId(ride.getDriver().getId()).ifPresent(d -> {
                d.setTotalEarnings(round2(d.getTotalEarnings() + p.getAmount() * DRIVER_SHARE));
                drivers.save(d);
            });
        }
        payments.save(p);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(p.getId(), p.getAmount(), p.getCurrency(), p.getStatus().name(),
                p.getCreatedAt().toString(), p.getPaidAt() == null ? null : p.getPaidAt().toString());
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
