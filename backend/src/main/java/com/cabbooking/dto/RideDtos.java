package com.cabbooking.dto;

import jakarta.validation.constraints.NotNull;

/** Request/response shapes for the ride booking flow. */
public class RideDtos {

    public record CreateRideRequest(
            @NotNull Double pickupLat,
            @NotNull Double pickupLng,
            @NotNull Double dropoffLat,
            @NotNull Double dropoffLng,
            String pickupLabel,
            String dropoffLabel,
            String promoCode) {}

    public record FareEstimateResponse(double distanceKm, double estimatedFare, int etaMinutes) {}

    public record DriverInfo(String name, String phone, String vehicleModel, String vehiclePlate,
                             double ratingAvg, Double lat, Double lng) {}

    public record RideResponse(
            Long id,
            String pickupLabel, String dropoffLabel,
            double pickupLat, double pickupLng, double dropoffLat, double dropoffLng,
            double distanceKm, double estimatedFare, Double finalFare,
            String promoCode, String status, String createdAt,
            DriverInfo driver) {}

    /** Driver moves a ride forward: ACCEPTED -> DRIVER_ARRIVING -> IN_PROGRESS -> COMPLETED. */
    public record StatusUpdateRequest(
            @NotNull String status) {}

    public record LocationUpdateRequest(
            @NotNull Double lat,
            @NotNull Double lng) {}

    public record SosRequest(String note) {}

    public record DriverProfileResponse(String name, String email, String phone,
                                        String vehicleModel, String vehiclePlate, boolean online,
                                        double ratingAvg, int ratingCount, double totalEarnings,
                                        Double lat, Double lng) {}

    public record UpdateDriverProfileRequest(String vehicleModel, String vehiclePlate, String phone) {}

    public record AvailabilityRequest(boolean online) {}
}
