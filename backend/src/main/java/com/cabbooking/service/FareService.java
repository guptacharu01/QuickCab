package com.cabbooking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Distance + fare math. Pricing formula is configurable via application.properties. */
@Service
public class FareService {

    private final double base;
    private final double perKm;
    private final double min;
    private static final double AVG_SPEED_KMH = 32.0;

    public FareService(@Value("${fare.base}") double base,
                       @Value("${fare.per-km}") double perKm,
                       @Value("${fare.min}") double min) {
        this.base = base;
        this.perKm = perKm;
        this.min = min;
    }

    /** Great-circle distance in km. */
    public double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    /** fare = max(min, base + perKm * km), rounded to 2 decimals. */
    public double estimate(double km) {
        return round2(Math.max(min, base + perKm * km));
    }

    public int etaMinutes(double km) {
        return (int) Math.max(2, Math.round(km / AVG_SPEED_KMH * 60));
    }

    /** Apply a promo code to a fare; returns the discount amount. */
    public double discountFor(double fare, double percentOff, double maxDiscount) {
        double d = percentOff > 0 ? fare * percentOff / 100.0 : maxDiscount;
        return round2(Math.min(d, Math.min(fare, maxDiscount > 0 ? maxDiscount : d)));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
