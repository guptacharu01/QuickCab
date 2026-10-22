package com.cabbooking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Small request/response shapes for ratings, tickets, promos and payments. */
public class MiscDtos {

    public record RatingRequest(
            @NotNull @Min(1) @Max(5) Integer stars,
            String comment) {}

    public record TicketRequest(
            @NotBlank String subject,
            @NotBlank String message) {}

    public record TicketResponse(Long id, String subject, String message, String status,
                                 String createdAt, String userName) {}

    public record PromoResponse(String code, double percentOff, double maxDiscount, boolean valid) {}

    /** Returned when a PaymentIntent is created. Null clientSecret = Stripe not configured. */
    public record PaymentIntentResponse(String clientSecret, String status, double amount, boolean configured) {}

    public record PaymentResponse(Long id, double amount, String currency, String status,
                                  String createdAt, String paidAt) {}

    public record ErrorResponse(String error, String message) {}

    public record RatingResponse(Long id, int stars, String comment, String riderName, String createdAt) {}

    public record ActiveStatusRequest(boolean active) {}

    public record TicketStatusRequest(String status) {}
}
