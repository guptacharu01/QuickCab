package com.cabbooking.service;

import com.cabbooking.dto.AuthDtos.*;
import com.cabbooking.entity.Driver;
import com.cabbooking.entity.User;
import com.cabbooking.repository.DriverRepository;
import com.cabbooking.repository.UserRepository;
import com.cabbooking.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Registration and login. Public admin registration is intentionally impossible. */
@Service
public class AuthService {

    private final UserRepository users;
    private final DriverRepository drivers;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository users, DriverRepository drivers, PasswordEncoder encoder,
                       AuthenticationManager authManager, JwtUtil jwtUtil) {
        this.users = users;
        this.drivers = drivers;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User.Role role = User.Role.valueOf(req.role());
        if (role == User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot self-register");
        }
        if (role == User.Role.DRIVER && (req.vehicleModel() == null || req.vehicleModel().isBlank()
                || req.vehiclePlate() == null || req.vehiclePlate().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle details are required for drivers");
        }
        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email().toLowerCase());
        u.setPhone(req.phone());
        u.setPasswordHash(encoder.encode(req.password())); // BCrypt
        u.setRole(role);
        users.save(u);

        if (role == User.Role.DRIVER) {
            Driver d = new Driver();
            d.setUser(u);
            d.setVehicleModel(req.vehicleModel());
            d.setVehiclePlate(req.vehiclePlate());
            drivers.save(d);
        }
        String token = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, u.getRole().name(), u.getName(), u.getId());
    }

    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException -> 401 via the exception handler.
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().toLowerCase(), req.password()));
        User u = users.findByEmail(req.email().toLowerCase()).orElseThrow();
        String token = jwtUtil.generate(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, u.getRole().name(), u.getName(), u.getId());
    }
}
