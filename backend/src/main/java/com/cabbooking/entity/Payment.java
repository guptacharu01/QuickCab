package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** One payment per ride. Amounts are always set by the backend. */
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {

    public enum Status { PENDING, SUCCEEDED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    /** Amount in INR, calculated by the backend from the ride fare. */
    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency = "INR";

    /** Stripe PaymentIntent id, e.g. pi_... . Null when Stripe isn't configured. */
    @Column(unique = true)
    private String stripePaymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant paidAt;
}
