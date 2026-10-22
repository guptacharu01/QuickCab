package com.cabbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request/response shapes for registration and login. Never expose password hashes. */
public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String phone,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @Pattern(regexp = "RIDER|DRIVER", message = "Role must be RIDER or DRIVER") String role,
            String vehicleModel,
            String vehiclePlate) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthResponse(String token, String role, String name, Long userId) {}
}
