package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** Extra profile for users with the DRIVER role (1 driver row per driver user). */
@Entity
@Table(name = "drivers")
@Getter @Setter @NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String vehicleModel;

    @Column(nullable = false)
    private String vehiclePlate;

    /** Only online drivers receive ride requests. */
    @Column(nullable = false)
    private boolean online = false;

    /** Last known location, updated by the driver app. */
    private Double currentLat;
    private Double currentLng;
    private Instant locationUpdatedAt;

    /** Running average shown to riders. */
    @Column(nullable = false)
    private double ratingAvg = 0.0;

    @Column(nullable = false)
    private int ratingCount = 0;

    /** Lifetime earnings (driver's 80% share of completed fares). */
    @Column(nullable = false)
    private double totalEarnings = 0.0;
}
