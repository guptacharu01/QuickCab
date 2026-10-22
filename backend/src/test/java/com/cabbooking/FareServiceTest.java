package com.cabbooking;

import com.cabbooking.service.FareService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the fare math: distance, pricing formula and promos.
 * Run with: mvn test
 */
class FareServiceTest {

    private final FareService fares = new FareService(60, 15, 80);

    @Test
    void haversineDelhiToAirportIsSane() {
        // Connaught Place -> IGI Airport T3 is roughly 13-16 km.
        double km = fares.haversineKm(28.6315, 77.2167, 28.5562, 77.1000);
        assertTrue(km > 12 && km < 18, "unexpected distance: " + km);
    }

    @Test
    void fareFollowsBasePlusPerKm() {
        assertEquals(60 + 15 * 10, fares.estimate(10), 0.01);
    }

    @Test
    void fareNeverBelowMinimum() {
        assertEquals(80, fares.estimate(0.1), 0.01);
    }

    @Test
    void promoDiscountIsCapped() {
        // 50% off a 500 fare, capped at 100 -> discount is 100.
        assertEquals(100, fares.discountFor(500, 50, 100), 0.01);
        // 50% off a 100 fare -> discount is 50.
        assertEquals(50, fares.discountFor(100, 50, 100), 0.01);
        // Flat 30 off.
        assertEquals(30, fares.discountFor(500, 0, 30), 0.01);
    }

    @Test
    void etaScalesWithDistance() {
        assertTrue(fares.etaMinutes(16) > fares.etaMinutes(2));
    }
}
