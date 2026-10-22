package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** A single trip request through its whole lifecycle. */
@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_rides_rider", columnList = "rider_id"),
        @Index(name = "idx_rides_driver", columnList = "driver_id"),
        @Index(name = "idx_rides_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor
public class Ride {

    public enum Status {
        REQUESTED,        // waiting for a driver
        ACCEPTED,         // driver assigned, heading to pickup
        DRIVER_ARRIVING,  // driver at/near pickup
        IN_PROGRESS,      // trip underway
        COMPLETED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @Column(nullable = false)
    private double pickupLat;
    @Column(nullable = false)
    private double pickupLng;
    @Column(nullable = false)
    private double dropoffLat;
    @Column(nullable = false)
    private double dropoffLng;

    private String pickupLabel;
    private String dropoffLabel;

    /** Haversine distance in km, computed by the backend. */
    @Column(nullable = false)
    private double distanceKm;

    /** Fare quoted at booking time (backend-calculated). */
    @Column(nullable = false)
    private double estimatedFare;

    /** Final fare charged (set on completion; equals estimate unless adjusted). */
    private Double finalFare;

    private String promoCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.REQUESTED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant completedAt;

    /** Rider's SOS note, if any, during an active ride. */
    private String sosNote;

    /** Optimistic lock so two drivers can never accept the same ride. */
    @Version
    private Long version;
}
