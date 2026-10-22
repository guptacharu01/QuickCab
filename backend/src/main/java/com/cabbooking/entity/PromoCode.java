package com.cabbooking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** Discount codes riders can apply at booking time. */
@Entity
@Table(name = "promo_codes")
@Getter @Setter @NoArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    /** Percent off, e.g. 50 = 50%. Use 0 for a flat discount. */
    @Column(nullable = false)
    private double percentOff;

    /** Maximum discount in INR. */
    @Column(nullable = false)
    private double maxDiscount;

    @Column(nullable = false)
    private boolean active = true;

    private Instant expiresAt;
}
